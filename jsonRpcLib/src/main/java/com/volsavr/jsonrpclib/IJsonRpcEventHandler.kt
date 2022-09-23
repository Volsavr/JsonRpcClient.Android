package com.volsavr.jsonrpclib

interface IJsonRpcEventHandler {
    fun onEvent(event: JsonRpcEvent)
}