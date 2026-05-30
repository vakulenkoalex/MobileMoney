package com.mobilemoney.ui.config

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*

object AppIcons {
    fun getTransactionIcon(iconName: String): ImageVector {
        return when (iconName) {
            "account_balance" -> Icons.Default.AccountBalance
            "attach_money" -> Icons.Default.AttachMoney
            "book" -> Icons.Default.Book
            "cake" -> Icons.Default.Cake
            "camera_alt" -> Icons.Default.CameraAlt
            "card_giftcard" -> Icons.Default.CardGiftcard
            "celebration" -> Icons.Default.Celebration
            "checkroom" -> Icons.Default.Checkroom
            "child_care" -> Icons.Default.ChildCare
            "cleaning_services" -> Icons.Default.CleaningServices
            "credit_card" -> Icons.Default.CreditCard
            "delivery_dining" -> Icons.Default.DeliveryDining
            "directions_bus" -> Icons.Default.DirectionsBus
            "directions_car" -> Icons.Default.DirectionsCar
            "eco" -> Icons.Default.Eco
            "face" -> Icons.Default.Face
            "fastfood" -> Icons.Default.Fastfood
            "favorite" -> Icons.Default.Favorite
            "fitness_center" -> Icons.Default.FitnessCenter
            "flight" -> Icons.Default.Flight
            "free_breakfast" -> Icons.Default.FreeBreakfast
            "groups" -> Icons.Default.Groups
            "headset" -> Icons.Default.Headset
            "home" -> Icons.Default.Home
            "icecream" -> Icons.Default.Icecream
            "kitchen" -> Icons.Default.Kitchen
            "library_books" -> Icons.AutoMirrored.Filled.LibraryBooks
            "liquor" -> Icons.Default.Liquor
            "local_atm" -> Icons.Default.LocalAtm
            "local_cafe" -> Icons.Default.LocalCafe
            "local_dining" -> Icons.Default.LocalDining
            "local_gas_station" -> Icons.Default.LocalGasStation
            "local_grocery_store" -> Icons.Default.LocalGroceryStore
            "local_hospital" -> Icons.Default.LocalHospital
            "local_pharmacy" -> Icons.Default.LocalPharmacy
            "local_taxi" -> Icons.Default.LocalTaxi
            "medical_services" -> Icons.Default.MedicalServices
            "medication" -> Icons.Default.Medication
            "mood" -> Icons.Default.Mood
            "more_horiz" -> Icons.Default.MoreHoriz
            "movie" -> Icons.Default.Movie
            "music_note" -> Icons.Default.MusicNote
            "nature" -> Icons.Default.Nature
            "outdoor_grill" -> Icons.Default.OutdoorGrill
            "park" -> Icons.Default.Park
            "payments" -> Icons.Default.Payments
            "pets" -> Icons.Default.Pets
            "phone" -> Icons.Default.Phone
            "psychology" -> Icons.Default.Psychology
            "receipt" -> Icons.Default.Receipt
            "recycling" -> Icons.Default.Recycling
            "restaurant" -> Icons.Default.Restaurant
            "savings" -> Icons.Default.Savings
            "school" -> Icons.Default.School
            "self_improvement" -> Icons.Default.SelfImprovement
            "shopping_basket" -> Icons.Default.ShoppingBasket
            "shopping_cart" -> Icons.Default.ShoppingCart
            "spa" -> Icons.Default.Spa
            "sports_esports" -> Icons.Default.SportsEsports
            "stadium" -> Icons.Default.Stadium
            "store" -> Icons.Default.Store
            "subway" -> Icons.Default.Subway
            "swap_horiz" -> Icons.Default.SwapHoriz
            "takeout_dining" -> Icons.Default.TakeoutDining
            "theater_comedy" -> Icons.Default.TheaterComedy
            "construction" -> Icons.Default.Construction
            "train" -> Icons.Default.Train
            "volunteer_activism" -> Icons.Default.VolunteerActivism
            "work" -> Icons.Default.Work
            "smartphone" -> Icons.Default.Smartphone
            "sensors" -> Icons.Default.Sensors
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
