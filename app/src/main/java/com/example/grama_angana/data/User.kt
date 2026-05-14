package com.example.grama_angana.data

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: String = "USER" // USER, ADMIN
)
