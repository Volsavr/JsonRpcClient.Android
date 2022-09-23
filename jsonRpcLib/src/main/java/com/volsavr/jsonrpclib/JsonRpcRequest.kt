package com.volsavr.jsonrpclib

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class JsonRpcRequest<R>(val id: Int? = null, invocation: Invocation<R>) {

    private val method = invocation.method
    private val params = invocation.params

    // buildBody
    fun buildBody(): JsonObject {
        val body: MutableMap<String, JsonElement> = mutableMapOf(
            //JsonKeys.jsonrpc.name to JsonPrimitive(HTTPRequestExecutorConfig.version),
            JsonKeys.method.name to JsonPrimitive(method)
        )
        params?.let { body[JsonKeys.params.name] = params }
        id?.let { body[JsonKeys.id.name] = JsonPrimitive(it) }
        return JsonObject(body)
    }

    private enum class JsonKeys {
        jsonrpc,
        method,
        params,
        result,
        error,
        code,
        message,
        data,
        id
    }

    override fun toString(): String = "{ " +
            "id = " + id + ", " +
            "method = " + method + ", " +
            "params = " + params?.toString() +
            "}"
}