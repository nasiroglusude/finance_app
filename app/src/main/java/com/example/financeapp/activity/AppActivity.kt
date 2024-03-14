package com.example.financeapp.activity

import android.app.Activity
import android.os.Bundle
import com.example.financeapp.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class AppActivity:Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app)

    }

}