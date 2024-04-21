package com.example.financeapp.data

data class Child(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val parentMail: String,
    val password: String,
    val creationDate: String?,
    val balance: String,
    val currency: String = "",
)


