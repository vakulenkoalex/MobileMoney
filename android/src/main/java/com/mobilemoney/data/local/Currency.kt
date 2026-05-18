package com.mobilemoney.data.local

enum class Currency(
    val code: String,
    val displayName: String,
    val symbol: String
) {
    RUB("RUB", "Российский рубль", "₽"),
    USD("USD", "Доллар США", "$"),
    EUR("EUR", "Евро", "€");

    companion object {
        fun fromCode(code: String): Currency? = entries.find { it.code == code }
    }
}