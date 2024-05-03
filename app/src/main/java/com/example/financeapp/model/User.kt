package com.example.financeapp.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    var id: String = "", // Include default values for properties
    var firstName: String = "",
    var lastName: String = "",
    var phoneNumber: String = "",
    var dateOfBirth: String = "",
    var email: String = "",
    var password: String = "",
    var balance:String = "",
    var currency: String = "",
    var creationDate: String? = null,
    var profilePhoto: String? = null
) {
    // Add a no-argument constructor
    constructor() : this("", "", "", "", "", "", "","","", null,null)
}
