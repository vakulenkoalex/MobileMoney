package com.mobilemoney.data.local

enum class SenderType(val displayName: String) {
    PHONE_NUMBER("Номер телефона"),
    PACKAGE_NAME("Имя пакета"),
    MESSENGER_PACKAGE_NAME("Имя пакета мессенджера"),
    MESSENGER_USERNAME("Имя пользователя в мессенджере")
}
