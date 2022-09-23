package com.volsavr.myapplication

import kotlinx.serialization.json.decodeFromJsonElement
import android.util.Log
import com.volsavr.jsonrpclib.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class MyJsonRpcService(
    val client: JsonRpcClient,
    val eventHandler: IMyServiceEventsHandler
): JsonRpcService(client), IJsonRpcEventHandler {

    //region Fields
    private val json = Json { ignoreUnknownKeys = true }
    //endregion

    //region Constructor
    init {
        //setup event handler
        client.eventHandler = this
    }
    //endregion

    //region Methods
    suspend fun contactGet(): String {
        if(!client.isConnected)
            throw Exception("Client does not connected")

        val parameters = "{\"version\": 0}"
        val parametersObject = Json.decodeFromString<JsonObject>(parameters)
        val contactGet = invoke("contact.get", parametersObject, AnyResultParser<JsonRpcResponse>())
        return contactGet.toString()
    }
    //endregion

    //region IJsonRpcEventHandler
    override fun onEvent(event: JsonRpcEvent) {

        Log.d("PbxJsonRpcService","onEvent->event name: ${event.method}")

        var eventType = EventType.getEventTypeByProtocolName(event.method)

        if (event.params == null)
            return // event without params (Not allowed in current protocol)

        when (eventType) {
            EventType.Unknown -> {} // Unknown event received from the server

            EventType.ContactUpdated -> {
                val data =
                    json.decodeFromJsonElement<Contact>(event.params!!)
                eventHandler.onContactUpdated(data)
            }

            EventType.ContactDeleted -> {
                val data =
                    json.decodeFromJsonElement<Contact>(event.params!!)
                eventHandler.onContactDeleted(data)
            }
        }
    }
    //endregion
}

enum class EventType(val protocolName: String) {
    ContactUpdated("contact.updated"),
    ContactDeleted("contact.deleted"),
    Unknown("");

    companion object {
        fun getEventTypeByProtocolName(protocolName: String): EventType {
            for (eventType in EventType.values()) {
                if (eventType.protocolName == protocolName)
                    return eventType
            }

            return Unknown
        }
    }
}