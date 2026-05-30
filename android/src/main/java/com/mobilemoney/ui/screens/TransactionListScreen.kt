package com.mobilemoney.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilemoney.ui.config.AppIcons
import com.mobilemoney.data.repository.FeaturePreferences
import com.mobilemoney.domain.model.Transaction
import com.mobilemoney.domain.usecase.transaction.ParseClipboardTransactionUseCase
import com.mobilemoney.ui.common.ErrorHandler
import com.mobilemoney.ui.utils.FormatUtils
import com.mobilemoney.viewmodel.TransactionFormViewModel.ClipboardPrefillData
import com.mobilemoney.viewmodel.TransactionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onTransactionClick: (UUID) -> Unit,
    onAddClick: () -> Unit,
    onClipboardPrefill: (ClipboardPrefillData) -> Unit,
    viewModel: TransactionListViewModel,
    parseClipboardTransactionUseCase: ParseClipboardTransactionUseCase
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val featurePrefs = remember { FeaturePreferences(context) }

    var showClipboardDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var clipboardText by remember { mutableStateOf("") }
    var debugResult by remember { mutableStateOf<DebugClipboardResult?>(null) }
    var clipboardPrefillData by remember { mutableStateOf<ClipboardPrefillData?>(null) }
    var readTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(readTrigger) {
        if (readTrigger == 0) return@LaunchedEffect
        if (!featurePrefs.clipboardParsingEnabled) return@LaunchedEffect

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) {
            ErrorHandler.emitError("Буфер обмена пуст")
            return@LaunchedEffect
        }

        val text = clip.getItemAt(0).coerceToText(context).toString()
        if (text.isBlank()) {
            ErrorHandler.emitError("Не удалось прочитать текст из буфера обмена")
            return@LaunchedEffect
        }

        val debugMode = featurePrefs.debugModeEnabled

        when (val result = parseClipboardTransactionUseCase.parse(text, debugMode)) {
            is ParseClipboardTransactionUseCase.Result.DebugInfo -> {
                debugResult = DebugClipboardResult(
                    clipboardText = result.clipboardText,
                    matched = result.matched,
                    accountName = result.accountName,
                    amount = result.amount,
                    shop = result.shop,
                    cardMaskParsed = result.cardMaskParsed,
                    cardMaskAccount = result.cardMaskAccount,
                    cardMaskMatches = result.cardMaskMatches,
                    isIncome = result.isIncome,
                    balance = result.balance
                )
                showDebugDialog = true
            }
            is ParseClipboardTransactionUseCase.Result.Success -> {
                clipboardText = text
                clipboardPrefillData = ClipboardPrefillData(
                    amount = result.prefill.parsed.amount,
                    accountId = result.prefill.account.id,
                    comment = result.prefill.comment,
                    shop = result.prefill.parsed.shop,
                    categoryId = result.prefill.categoryId,
                    isIncome = result.prefill.parsed.isIncome,
                    clipboardText = text
                )
                showClipboardDialog = true
            }
            is ParseClipboardTransactionUseCase.Result.NoAccount -> {
                ErrorHandler.emitError("Нет счетов с авто-созданием или регулярных выражений")
            }
            is ParseClipboardTransactionUseCase.Result.NoMatch -> {
                ErrorHandler.emitError("Текст не распознан")
            }
        }
    }

    fun readClipboard() {
        readTrigger++
    }

    if (showClipboardDialog) {
        ClipboardDialog(
            clipboardText = clipboardText,
            onConfirm = {
                showClipboardDialog = false
                clipboardPrefillData?.let { onClipboardPrefill(it) }
            },
            onDismiss = {
                showClipboardDialog = false
            }
        )
    }

    if (showDebugDialog && debugResult != null) {
        DebugClipboardDialog(
            result = debugResult!!,
            onDismiss = {
                showDebugDialog = false
                if (debugResult!!.matched && clipboardPrefillData != null) {
                    showClipboardDialog = true
                }
                debugResult = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Операции", style = MaterialTheme.typography.titleSmall) },
                actions = {
                    if (featurePrefs.clipboardParsingEnabled) {
                        IconButton(onClick = { readClipboard() }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Вставить из буфера")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { paddingValues ->
        val sortedTransactions = remember(uiState.transactions) {
            uiState.transactions.sortedByDescending { it.date }
        }
        val groupedByDate = remember(sortedTransactions) {
            sortedTransactions.groupBy {
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
        val sections = remember(groupedByDate) {
            groupedByDate.entries.sortedByDescending { it.key }.map { it.key to it.value }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            sections.forEach { (dateKey, txs) ->
                item(key = dateKey.toString()) {
                    Text(
                        text = headerTitle(dateKey),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 2.dp)
                    )
                }

                items(txs, key = { it.id.toString() }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

// Helpers
fun headerTitle(date: LocalDate): String {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    return when (date) {
        today -> "Сегодня"
        yesterday -> "Вчера"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("ru-RU")))
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(transaction.color).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.getTransactionIcon(transaction.icon),
                contentDescription = null,
                tint = Color(transaction.color),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = transaction.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Text(
            text = FormatUtils.formatAmount(transaction.amount, transaction.currency, transaction.isIncome),
            color = if (transaction.isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}


