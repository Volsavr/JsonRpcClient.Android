package com.volsavr.jsonrpclib

import com.volsavr.jsonrpclib.AnyResultParser
import com.volsavr.jsonrpclib.InvalidFormatResultParserError
import kotlinx.serialization.json.JsonPrimitive

class JsonPrimitiveResultParser : AnyResultParser<JsonPrimitive>() {
    override fun parse(obj: Any): JsonPrimitive {
        try {
            return obj as JsonPrimitive
        } catch (error: Throwable) {
            throw InvalidFormatResultParserError(error)
        }
    }
}