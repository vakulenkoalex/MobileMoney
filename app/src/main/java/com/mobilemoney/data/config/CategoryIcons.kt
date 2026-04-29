package com.mobilemoney.data.config

data class CategoryIconOption(
    val name: String,
    val label: String
)

object CategoryIcons {
    val all = listOf(
        CategoryIconOption("restaurant", "Еда"),
        CategoryIconOption("directions_bus", "Транспорт"),
        CategoryIconOption("shopping_cart", "Магазин"),
        CategoryIconOption("movie", "Кино"),
        CategoryIconOption("local_hospital", "Здоровье"),
        CategoryIconOption("work", "Работа"),
        CategoryIconOption("card_giftcard", "Подарок"),
        CategoryIconOption("local_taxi", "Такси"),
        CategoryIconOption("school", "Обучение"),
        CategoryIconOption("home", "Дом"),
        CategoryIconOption("pets", "Животные"),
        CategoryIconOption("phone", "Связь"),
        CategoryIconOption("flight", "Путешествия"),
        CategoryIconOption("fitness_center", "Спорт"),
        CategoryIconOption("checkroom", "Одежда"),
        CategoryIconOption("more_horiz", "Прочее")
    )
}