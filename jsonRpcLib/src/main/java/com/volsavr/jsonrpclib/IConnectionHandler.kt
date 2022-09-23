package com.volsavr.jsonrpclib

 interface IConnectionHandler{
    fun onConnectionOpened()
    fun onConnectionFailure()
    fun onConnectionClosed()
    fun onConnectionClosing()
}