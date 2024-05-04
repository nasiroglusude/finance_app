package com.example.financeapp.model

data class Budget(
    val id: String = "",
    val title: String = "",
    val amount: String = "",
    val color: String = "",
    val currency: String = "",
    val type: String = "",
    val repetition: String = "",
    val category: String = "",
    var creationDate: String = "",
    var lastUpdate: String? = null,
    val firstAddition: Boolean = false,
) {
    // Add a no-argument constructor
    constructor() : this("", "", "", "", "", "","", "", "", "",)
}

