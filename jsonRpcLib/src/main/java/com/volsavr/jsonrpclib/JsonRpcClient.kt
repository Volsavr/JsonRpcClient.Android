package com.volsavr.jsonrpclib

import com.daveanthonythomas.moshipack.MoshiPack
import android.util.Log
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.*
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.msgpack.core.MessagePack
import java.util.concurrent.TimeUnit

class JsonRpcClient(private val url: String,
                    private val key: String,
                    private val userAgent: String,
                    private val connectionHandler: IConnectionHandler? = null): WebSocketListener() {

    //region Fields
    var eventHandler: IJsonRpcEventHandler? = null

    private var webSocket: WebSocket
    private val requestIdGenerator = RequestIdGenerator()
    var isConnected: Boolean = false

    //ToDo: consider migration to ConcurrentMap
    private var responses: MutableMap<Int, CancellableContinuation<JsonRpcResponse>> =
        mutableMapOf()
    //endregion

    //region Constants
    private val payloadVersion: Byte = 0
    //endregion

    //region Constructor
    init {
        val request = Request.Builder()
            .addHeader("API-KEY", key)
            .addHeader("user-agent", userAgent)
            .url(url)
            .build()

        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()

        Log.d("JsonRpcClient", "initiated socket connection")

        webSocket = client.newWebSocket(request, this)

        //client.dispatcher.executorService.shutdown()
    }
    //endregion

    //region WebSocketListener
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("JsonRpcClient", "onOpen")

        isConnected = true
        super.onOpen(webSocket, response)
        connectionHandler?.onConnectionOpened()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)

        Log.d("JsonRpcClient", "onMessage -> received ${bytes.size} bytes")

        if (bytes.size == 0) return

        val version: Byte = bytes[0]

        if (version != payloadVersion)
            return // unsupported payload version

        //extract command payload
        val bytesArray = bytes.toByteArray()
        val bytesWithoutVersion = ByteArray(bytesArray.size - 1)
        bytesArray.copyInto(bytesWithoutVersion, 0, 1)

        // Todo: Migrate to MoshiPack to use one library (issue: https://stackoverflow.com/questions/68424893/why-does-moshi-parse-integers-longs-as-double)
        val message = MessagePack.newDefaultUnpacker(bytesWithoutVersion)
        val unpacked = message.unpackValue()
        val json = unpacked.toString()

        Log.d("JsonRpcClient", "onMessage -> $json")

        val jsonElement = Json.parseToJsonElement(json)

        if(jsonElement.jsonObject.containsKey("method")){
            //decode json to JsonRpcEvent
            val event = Json.decodeFromJsonElement<JsonRpcEvent>(jsonElement)
            //run event handler
            eventHandler?.onEvent(event)
        }
        else {
            //decode json to JsonRpcResponse
            val response = Json.decodeFromJsonElement<JsonRpcResponse>(jsonElement)
            val requestId = response.id

            if(!responses.contains(requestId))
                return

            //resume suspended function with result
            responses[requestId]?.resume(response) {}
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("JsonRpcClient", "onClosed")

        isConnected = false
        super.onClosed(webSocket, code, reason)
        connectionHandler?.onConnectionClosed()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d("JsonRpcClient", "onFailure -> response : ${response?.body.toString()}")
        Log.d("JsonRpcClient", "onFailure -> exception : ${t.message}")
        isConnected = false
        super.onFailure(webSocket, t, response)
        connectionHandler?.onConnectionFailure()
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("JsonRpcClient", "onClosing -> code: $code, reason: $reason")
        super.onClosing(webSocket, code, reason)
        connectionHandler?.onConnectionClosing()
    }
    //endregion

    //region Request
    suspend fun <R> invoke(invocation: Invocation<R>) =
        invocation.parser.parse(execute(makeRequest(invocation), 10000L))

    private fun <R> makeRequest(invocation: Invocation<R>) =
        JsonRpcRequest(requestIdGenerator.next(), invocation)

    private suspend fun <R> execute(jsonRpcRequest: JsonRpcRequest<R>, timeout: Long): Any {
        Log.d("JsonRpcClient", "execute")

        if (!isConnected)
            throw ConnectionException()

        //prepare json
        val json = jsonRpcRequest.buildBody().toString()

        Log.d("JsonRpcClient", "execute -> $json")

        //encode request payload
        val packed: BufferedSource = MoshiPack.jsonToMsgpack(json)

        //prepare data array
        val bytesArray = packed.readByteArray()
        val bytesWithVersion = ByteArray(bytesArray.size + 1)
        bytesArray.copyInto(bytesWithVersion, 1, 0)

        //set protocol version
        bytesWithVersion[0] = payloadVersion

        //convert to byte string
        val data = bytesWithVersion.toByteString()

        //log data in hex format
        val hexData = data.hex()
        Log.d("JsonRpcClient", "execute -> payload: $hexData")

        val response = suspendCoroutineWithTimeout<JsonRpcResponse>(timeout) { continuation ->
            // Add the Request details to the Responses dictionary so that we have
            // an entry to match up against whenever the response is received.
            responses[jsonRpcRequest.id!!] = continuation

            // Send the request to the server
            webSocket.send(data)

            Log.d("JsonRpcClient", "execute -> sent ${data.size} bytes")
        }

        //remove key from map
        responses.remove(jsonRpcRequest.id)

        if(response == null) {
            Log.d("JsonRpcClient", "execute -> timeout happened during sending command with id: ${jsonRpcRequest.id}")

            val error = com.volsavr.jsonrpclib.Error(
                "Timeout happened during sending command with id: ${jsonRpcRequest.id}",
                0,
                null
            )

            return JsonRpcResponse(jsonRpcRequest.id!!, null, error)
        }

        return response
    }

    private suspend inline fun <T> suspendCoroutineWithTimeout(timeout: Long, crossinline block: (CancellableContinuation<T>) -> Unit ) : T? {
        var finalValue : T? = null
        withTimeoutOrNull(timeout) {
            finalValue = suspendCancellableCoroutine(block = block)
        }
        return finalValue
    }
    //endregion

    //region Connection Control
    fun closeConnection(){
        if(isConnected)
           webSocket.close(1000, "close")
    }
    //endregion
}

//Request id generation
typealias RequestId = Int
var lastIdx = 0

class RequestIdGenerator {
    fun next(): RequestId {
        lastIdx += 1
        return lastIdx
    }
}

class ConnectionException() : Throwable("Connection was not established")
class ExecuteException(val error: Error) : Throwable(error.toString())
abstract class ResultParserError(error: Throwable) : Throwable(error)
class InvalidFormatResultParserError(error: Throwable) : ResultParserError(error)
class JsonRpcCommandTimeoutException(val msg: String) : Throwable(msg)