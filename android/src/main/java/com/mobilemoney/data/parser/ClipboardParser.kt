package com.mobilemoney.data.parser

data class ParsedClipboardData(
    val amount: String,
    val shop: String,
    val cardMask: String,
    val balance: String
)

object ClipboardParser {

    private val REQUIRED_GROUPS = listOf("amount", "shop", "cardMask", "balance")

    fun parse(text: String, regex: String): ParsedClipboardData? {
        if (text.isBlank() || regex.isBlank()) return null

        return try {
            val regexPattern = Regex(regex, RegexOption.IGNORE_CASE)
            val match = regexPattern.find(text) ?: return null

            for (groupName in REQUIRED_GROUPS) {
                if (match.groups[groupName]?.value == null) return null
            }

            ParsedClipboardData(
                amount = match.groups["amount"]!!.value.trim(),
                shop = match.groups["shop"]!!.value.trim(),
                cardMask = match.groups["cardMask"]!!.value.trim().replace("*", "").replace("х", ""),
                balance = match.groups["balance"]!!.value.trim()
            )
        } catch (e: Exception) {
            null
        }
    }
}