package com.mobilemoney.data.config

data class IconOption(
    val name: String,
    val label: String
)

object AccountIcons {
    val all = listOf(
        IconOption("wallet", "Наличные"),
        IconOption("credit_card", "Карта"),
        IconOption("account_balance", "Счёт"),
        IconOption("savings", "Накопления"),
        IconOption("home", "Дом"),
        IconOption("business", "Бизнес"),
        IconOption("school", "Обучение"),
        IconOption("favorite", "Личное")
    )
}