package com.mobilemoney.data.config

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

object AppIcons {
    fun getTransactionIcon(iconName: String): ImageVector {
        return when (iconName) {
            "shopping_cart" -> Icons.Default.ShoppingCart
            "local_taxi" -> Icons.Default.LocalTaxi
            "work" -> Icons.Default.Work
            "swap_horiz" -> Icons.Default.SwapHoriz
            "local_hospital" -> Icons.Default.LocalHospital
            "movie" -> Icons.Default.Movie
            "restaurant" -> Icons.Default.Restaurant
            "directions_bus" -> Icons.Default.DirectionsBus
            "card_giftcard" -> Icons.Default.CardGiftcard
            "school" -> Icons.Default.School
            "home" -> Icons.Default.Home
            "pets" -> Icons.Default.Pets
            "phone" -> Icons.Default.Phone
            "flight" -> Icons.Default.Flight
            "fitness_center" -> Icons.Default.FitnessCenter
            "checkroom" -> Icons.Default.Checkroom
            "more_horiz" -> Icons.Default.MoreHoriz
            else -> Icons.Default.Category
        }
    }

    fun getCategoryIcon(iconName: String): ImageVector = getTransactionIcon(iconName)

    fun getAccountIcon(iconName: String): ImageVector {
        return when (iconName) {
            "wallet" -> Icons.Default.AccountBalanceWallet
            "credit_card" -> Icons.Default.CreditCard
            "account_balance" -> Icons.Default.AccountBalance
            "savings" -> Icons.Default.Savings
            "home" -> Icons.Default.Home
            "business" -> Icons.Default.Business
            "school" -> Icons.Default.School
            "favorite" -> Icons.Default.Favorite
            else -> Icons.Default.AccountBalanceWallet
        }
    }
}