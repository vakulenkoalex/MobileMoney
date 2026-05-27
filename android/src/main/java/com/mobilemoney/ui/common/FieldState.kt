package com.mobilemoney.ui.common

data class FieldState<T>(
    val value: T? = null,
    val error: String? = null
) {
    val isValid get() = error == null

    fun validate(emptyMessage: String): FieldState<T> =
        if (value == null) copy(error = emptyMessage) else this

    fun withValue(value: T?): FieldState<T> =
        copy(value = value, error = null)
}

fun <T> FieldState<T>.require(emptyMessage: String): FieldState<T> {
    val validated = validate(emptyMessage)
    if (!validated.isValid) return validated
    if (value is String && (value as String).isBlank()) return copy(error = emptyMessage)
    return this
}
