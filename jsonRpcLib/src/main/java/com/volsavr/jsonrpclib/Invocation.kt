package com.volsavr.jsonrpclib

import kotlinx.serialization.json.JsonObject

data class Invocation<Result>(
    val method: String,
    val params: JsonObject?,
    val parser: AnyResultParser<Result>
)