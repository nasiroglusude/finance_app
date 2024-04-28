package com.example.financeapp.model

data class Category(
    val id: String = "",
    val title: String = "",
    val color: String = ""
) {
    // Add a no-argument constructor
    constructor() : this("", "", "")
}
