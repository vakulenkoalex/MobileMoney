package com.mobilemoney.domain.parser

import com.mobilemoney.domain.model.ParsedTransaction

object TextParser {

    private val REQUIRED_GROUPS = listOf("amount", "shop", "cardMask", "direction")
    private val NAMED_GROUP_REGEX = Regex("\\(\\?<([a-zA-Z_]+)>")
    private val INCOME_KEYWORDS = setOf("поступление", "зачисление", "доход", "приход")

    fun parse(text: String, regex: String): ParsedTransaction? {
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

            val direction = values["direction"]!!
            val isIncome = direction.lowercase().let { dir ->
                INCOME_KEYWORDS.any { dir.contains(it) }
            }

            ParsedTransaction(
                amount = values["amount"]!!,
                shop = values["shop"]!!,
                cardMask = values["cardMask"]!!.replace("*", "").replace("х", ""),
                isIncome = isIncome,
                balance = values["balance"]
            )
        } catch (e: Exception) {
            null
        }
    }
}
