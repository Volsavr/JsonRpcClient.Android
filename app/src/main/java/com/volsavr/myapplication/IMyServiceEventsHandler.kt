package com.volsavr.myapplication

interface IMyServiceEventsHandler {
    fun onContactUpdated(contact: Contact)
    fun onContactDeleted(contact: Contact)
}