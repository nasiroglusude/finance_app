package com.example.financeapp.model

data class Bill(
    val id: String,
    val ownerName: String,
    val amount: String,
    val currency: String,
    val lastDate: String,
    var status: String
){
    // Add a no-argument constructor
    constructor() : this("", "", "", "","","")
}



