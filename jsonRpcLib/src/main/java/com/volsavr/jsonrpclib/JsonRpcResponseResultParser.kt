package com.volsavr.jsonrpclib

import kotlinx.serialization.json.JsonPrimitive

class JsonRpcResponseResultParser: AnyResultParser<JsonRpcResponse>() {
    override fun parse(obj: Any): JsonRpcResponse {
        try {
            return obj as JsonRpcResponse
        } catch (error: Throwable) {
            throw InvalidFormatResultParserError(error)
        }
    }
}