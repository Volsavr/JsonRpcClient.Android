package com.volsavr.jsonrpclib

import com.volsavr.jsonrpclib.JsonPrimitiveResultParser
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

abstract class JsonRpcService(private val client: JsonRpcClient) {

    suspend fun <Result> invoke(method: String, params: JsonObject? = null, parser: AnyResultParser<Result>): Result {
        val invocation = makeInvocation(method, params, parser)
        return client.invoke(invocation)
    }

    suspend fun invoke(method: String, params: JsonObject? = null): JsonPrimitive {
        return invoke(method, params, JsonPrimitiveResultParser())
    }

    private fun <Result> makeInvocation(method: String, params: JsonObject? = null, parser: AnyResultParser<Result>): Invocation<Result> {
        return Invocation<Result>(method, params, parser)
    }
}