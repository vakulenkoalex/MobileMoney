package com.mobilemoney.data.config

import com.mobilemoney.data.local.AccountEntity
import com.mobilemoney.data.local.CategoryEntity
import com.mobilemoney.data.local.CurrencyEntity
import com.mobilemoney.data.model.AccountType

object DefaultData {

    val currencies: List<CurrencyEntity> by lazy {
        listOf(
            CurrencyEntity("RUB", "Российский рубль", "₽", System.currentTimeMillis()),
            CurrencyEntity("USD", "Доллар США", "$", System.currentTimeMillis()),
            CurrencyEntity("EUR", "Евро", "€", System.currentTimeMillis())
        )
    }

    val categories: List<CategoryEntity> by lazy {
        val now = System.currentTimeMillis()
        listOf(
            CategoryEntity(java.util.UUID.randomUUID().toString(), "Кафе и рестораны", false, "restaurant", null, now, now),
            CategoryEntity(java.util.UUID.randomUUID().toString(), "Развлечения", false, "movie", null, now, now),
            CategoryEntity(java.util.UUID.randomUUID().toString(), "Здоровье", false, "local_hospital", null, now, now),
            CategoryEntity(java.util.UUID.randomUUID().toString(), "Зарплата", true, "work", null, now, now),
            CategoryEntity(java.util.UUID.randomUUID().toString(), "Подарок", true, "card_giftcard", null, now, now),
            CategoryEntity(java.util.UUID.randomUUID().toString(), "Корректировка", false, "more_horiz", null, now, now),
            CategoryEntity(java.util.UUID.randomUUID().toString(), "Корректировка", true, "more_horiz", null, now, now)
        )
    }

    val accounts: List<AccountEntity> by lazy {
        val now = System.currentTimeMillis()
        listOf(
            AccountEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = "Наличные",
                typeId = AccountType.CASH.id,
                currencyCode = "RUB",
                icon = "wallet",
                isDefault = true,
                archived = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}