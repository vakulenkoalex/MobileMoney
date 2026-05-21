package com.mobilemoney.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class DebugClipboardResult(
    val clipboardText: String,
    val matched: Boolean,
    val accountName: String? = null,
    val amount: String? = null,
    val shop: String? = null,
    val cardMaskParsed: String? = null,
    val cardMaskAccount: String? = null,
    val cardMaskMatches: Boolean = false,
    val balance: String? = null
)

@Composable
fun DebugClipboardDialog(
    result: DebugClipboardResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отладка парсинга") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Буфер:", style = MaterialTheme.typography.labelMedium)
                Text(result.clipboardText)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Text("Счёт: ${result.accountName ?: "—"}")
                Spacer(Modifier.height(4.dp))
                Text("amount: ${result.amount ?: "—"}")
                Text("shop: ${result.shop ?: "—"}")
                Text("cardMask: ${result.cardMaskParsed ?: "—"} vs ${result.cardMaskAccount ?: "—"} ${if (result.cardMaskMatches) "✅" else "❌"}")
                Text("balance: ${result.balance ?: "—"}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
