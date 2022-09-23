package com.volsavr.jsonrpclib

data class HTTPRequestExecutorConfig(val baseURL: String) {
    companion object {
        const val version = "2.0"
    }
}