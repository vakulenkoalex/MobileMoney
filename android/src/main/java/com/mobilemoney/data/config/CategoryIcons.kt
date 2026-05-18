package com.mobilemoney.data.config

data class CategoryIconOption(
    val name: String
)

object CategoryIcons {
    val all = listOf(
        CategoryIconOption("restaurant"),
        CategoryIconOption("directions_bus"),
        CategoryIconOption("shopping_cart"),
        CategoryIconOption("movie"),
        CategoryIconOption("local_hospital"),
        CategoryIconOption("work"),
        CategoryIconOption("card_giftcard"),
        CategoryIconOption("local_taxi"),
        CategoryIconOption("school"),
        CategoryIconOption("home"),
        CategoryIconOption("pets"),
        CategoryIconOption("phone"),
        CategoryIconOption("flight"),
        CategoryIconOption("fitness_center"),
        CategoryIconOption("checkroom"),
        CategoryIconOption("more_horiz")
    )
}