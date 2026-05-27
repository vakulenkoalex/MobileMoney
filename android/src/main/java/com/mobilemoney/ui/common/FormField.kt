package com.mobilemoney.ui.common

data class FormField(
    val value: String = "",
    val label: String = "",
    val error: String? = null
) {
    val isBlank get() = value.isBlank()
    val isValid get() = error == null

    fun validate(): FormField =
        if (isBlank) copy(error = "Введите '$label'") else this

    fun withValue(value: String): FormField =
        copy(value = value, error = null)
}
