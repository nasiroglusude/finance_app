package com.example.financeapp.enums

enum class Repetition(val code: String, val displayName: String) {
    NONE("none","One Time"),
    MONTHLY("monthly", "Monthly"),
    ANNUAL("annual", "Annual");

    companion object {
        fun fromCode(code: String): Repetition? {
            return entries.find { it.code == code }
        }
    }
}
