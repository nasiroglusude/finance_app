package com.example.financeapp.model

data class Debt(
    val id: String,
    val creditorName: String,
    val amount: String,
    val currency: String,
    val lastDate: String,
    var status: String
){
    // Add a no-argument constructor
    constructor() : this("", "", "", "","","")
}



