package com.volsavr.myapplication

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val id: String,
    val name: String,
)