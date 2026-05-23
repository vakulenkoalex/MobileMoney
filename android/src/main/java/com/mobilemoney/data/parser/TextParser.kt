package com.mobilemoney.data.parser

data class ParsedTextData(
    val amount: String,
    val shop: String,
    val cardMask: String,
    val balance: String? = null
)

object TextParser {

    private val REQUIRED_GROUPS = listOf("amount", "shop", "cardMask")
    private val NAMED_GROUP_REGEX = Regex("\\(\\?<([a-zA-Z_]+)>")

    fun parse(text: String, regex: String): ParsedTextData? {
        if (text.isBlank() || regex.isBlank()) return null

        return try {
            val regexPattern = Regex(regex, RegexOption.IGNORE_CASE)
            val match = regexPattern.find(text) ?: return null

            val groupNames = NAMED_GROUP_REGEX.findAll(regex)
                .map { it.groupValues[1] }
                .toSet()

            val values = groupNames.associateWith { name ->
                match.groups[name]?.value?.trim()
            }

            for (g in REQUIRED_GROUPS) {
                if (values[g].isNullOrBlank()) return null
            }

            ParsedTextData(
                amount = values["amount"]!!,
                shop = values["shop"]!!,
                cardMask = values["cardMask"]!!.replace("*", "").replace("х", ""),
                balance = values["balance"]
            )
        } catch (e: Exception) {
            null
        }
    }
}
