package com.volsavr.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.volsavr.jsonrpclib.IConnectionHandler
import com.volsavr.jsonrpclib.JsonRpcClient
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), IConnectionHandler, IMyServiceEventsHandler {
    //region Fields
    private var service: MyJsonRpcService? = null
    //endregion

    //region Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val client = JsonRpcClient("wss://soket_host", "key", "test.client", this)
        service = MyJsonRpcService(client,this)
    }
    //endregion

    //region CoroutineExceptionHandler
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.d("MainActivity", "[CoroutineExceptionHandler]-> $exception", null)
    }
    //endregion

    //region IConnectionHandler
    override fun onConnectionOpened(){

        // Starts a coroutine on the main thread
        GlobalScope.launch(Dispatchers.Main + coroutineExceptionHandler) {
            withContext(Dispatchers.IO) { // Changes to Dispatchers.IO and suspends the main thread
                val result = service?.contactGet()
                Log.d("Contact", result!!)
            }
        }
    }
    override fun onConnectionFailure(){

    }
    override fun onConnectionClosed(){

    }
    override fun onConnectionClosing(){

    }
    //endregion

    //region IMyServiceEventsHandler
    override fun onContactDeleted(contact: Contact) {
        TODO("Not yet implemented")
    }

    override fun onContactUpdated(contact: Contact) {
        TODO("Not yet implemented")
    }
    //endregion
}