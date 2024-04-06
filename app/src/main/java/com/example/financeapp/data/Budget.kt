package com.example.financeapp.data

data class Budget(
    val id: String = "",
    val title: String = "",
    val amount: String = "",
    val color: String = "",
    val currency: String = "",
    val type: String = ""
) {
    // Add a no-argument constructor
    constructor() : this("", "", "", "", "", "")
}
