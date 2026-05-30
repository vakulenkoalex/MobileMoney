package com.mobilemoney.ui.config

data class CurrencyConfig(
    val code: String,
    val symbol: String,
    val name: String
)

object Currencies {
    val all = listOf(
        CurrencyConfig("RUB", "₽", "Российский рубль"),
        CurrencyConfig("USD", "$", "Доллар США"),
        CurrencyConfig("EUR", "€", "Евро"),
        CurrencyConfig("KZT", "₸", "Казахстанский тенге")
    )

    fun getSymbol(code: String): String {
        return all.find { it.code == code }?.symbol ?: "₽"
    }
}
