package com.example.financeapp.enums

import com.example.financeapp.R

enum class Repetition(val code: String, val displayName: String) {
    NONE("none", "Tek Seferlik"),
    MONTHLY("monthly", "Aylık"),
    ANNUAL("annual", "Yıllık");

    companion object {
        fun fromCode(code: String): Repetition? {
            return entries.find { it.code == code }
        }
    }
}
