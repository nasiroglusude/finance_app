package com.example.financeapp.model

data class Budget(
    val id: String = "",
    val title: String = "",
    val amount: String = "",
    val color: String = "",
    val currency: String = "",
    val type: String = "",
    val category: String = "",
    var creationDate: String = ""
) {
    // Add a no-argument constructor
    constructor() : this("", "", "", "", "", "", "", "")
}

