package com.mobilemoney.domain.model

enum class SenderType(val displayName: String) {
    PHONE_NUMBER("Номер телефона"),
    EMAIL("Email"),
    ALPHANUMERIC("Алфавитно-цифровой"),
    PACKAGE_NAME("Имя пакета"),
    MESSENGER_PACKAGE_NAME("Имя пакета мессенджера"),
    MESSENGER_USERNAME("Имя пользователя в мессенджере")
}

data class Sender(
    val id: String,
    val sender: String,
    val label: String = "",
    val type: SenderType = SenderType.PHONE_NUMBER
)
