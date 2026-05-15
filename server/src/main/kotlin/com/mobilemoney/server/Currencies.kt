package com.mobilemoney.server

object Currencies {
    data class Currency(
        val code: String,
        val name: String,
        val symbol: String
    )

    val all = listOf(
        Currency("RUB", "Российский рубль", "₽"),
        Currency("USD", "Доллар США", "$"),
        Currency("EUR", "Евро", "€")
    )

    fun fromCode(code: String): Currency? = all.find { it.code == code }
}