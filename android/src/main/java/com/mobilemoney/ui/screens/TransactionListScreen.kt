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
import com.mobilemoney.data.config.AppIcons
import com.mobilemoney.data.local.AppDatabase
import com.mobilemoney.data.local.MessageRegexEntity
import com.mobilemoney.data.parser.TextParser
import com.mobilemoney.data.parser.ParsedTextData
import com.mobilemoney.data.repository.AccountBalanceCalculator
import com.mobilemoney.data.repository.FeaturePreferences
import com.mobilemoney.data.repository.DatabaseRepository
import com.mobilemoney.di.DI
import com.mobilemoney.domain.model.Transaction
import com.mobilemoney.ui.common.ErrorHandler
import com.mobilemoney.ui.utils.FormatUtils
import com.mobilemoney.viewmodel.TransactionFormViewModel.ClipboardPrefillData
import com.mobilemoney.viewmodel.TransactionListViewModel
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onTransactionClick: (UUID) -> Unit,
    onAddClick: () -> Unit,
    onClipboardPrefill: (ClipboardPrefillData) -> Unit,
    viewModel: TransactionListViewModel = DI.transactionListViewModel
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

        val accounts = AppDatabase.getDatabase(context)
            .accountDao().getAllAccountsIncludingArchived().first()

        val enabledAccounts = accounts.filter { it.autoCreateEnabled }

        val regexDao = AppDatabase.getDatabase(context).messageRegexDao()
        val regexList = regexDao.getAll().first()

        if (debugMode) {
            var bestResult: ParsedTextData? = null
            var bestRegex: MessageRegexEntity? = null
            var bestCount = -1

            for (re in regexList) {
                val parsed = TextParser.parse(text, re.pattern) ?: continue
                val filled = listOfNotNull(
                    parsed.amount.takeIf { it.isNotBlank() },
                    parsed.shop.takeIf { it.isNotBlank() },
                    parsed.cardMask.takeIf { it.isNotBlank() },
                    parsed.balance?.takeIf { it.isNotBlank() }
                ).size
                if (filled > bestCount) {
                    bestCount = filled
                    bestResult = parsed
                    bestRegex = re
                }
            }

            val matchedAccount = bestResult?.cardMask?.let { cm ->
                enabledAccounts.find { it.cardMask == cm }
            }

            if (matchedAccount != null && bestResult != null) {
                clipboardText = text

                val db = AppDatabase.getDatabase(context)
                val balanceCalc = AccountBalanceCalculator(db.transactionDao())
                val dbRepo = DatabaseRepository(db.accountDao(), db.categoryDao(), db.transactionDao(), db.messageDao(), db.senderDao())

                val comment = if (!bestRegex!!.skipBalanceCheck && bestResult.balance != null) {
                    val currentBalance = balanceCalc.getAccountBalance(matchedAccount.id)
                    val expectedBalance = balanceCalc.calculateExpectedBalance(bestResult.balance, bestResult.amount)
                    if (expectedBalance != null) {
                        val discrepancy = balanceCalc.getBalanceDiscrepancy(currentBalance, expectedBalance)
                        if (discrepancy != null) {
                            balanceCalc.formatDiscrepancyComment(discrepancy, expectedBalance, currentBalance)
                        } else ""
                    } else ""
                } else ""

                var categoryId: UUID? = null
                if (bestResult.shop.isNotBlank()) {
                    val lastTx = dbRepo.getLastTransactionByShop(bestResult.shop, bestResult.isIncome)
                    categoryId = lastTx?.categoryId
                }

                clipboardPrefillData = ClipboardPrefillData(
                    amount = bestResult.amount,
                    accountId = java.util.UUID.fromString(matchedAccount.id),
                    comment = comment,
                    shop = bestResult.shop,
                    categoryId = categoryId,
                    isIncome = bestResult.isIncome,
                    clipboardText = text
                )
            }

            debugResult = DebugClipboardResult(
                clipboardText = text,
                matched = bestResult != null,
                accountName = matchedAccount?.name
                    ?: if (bestResult != null) "Счёт не найден" else null,
                amount = bestResult?.amount,
                shop = bestResult?.shop,
                cardMaskParsed = bestResult?.cardMask,
                cardMaskAccount = matchedAccount?.cardMask,
                cardMaskMatches = matchedAccount != null,
                isIncome = bestResult?.isIncome,
                balance = bestResult?.balance
            )
            showDebugDialog = true
            return@LaunchedEffect
        }

        if (enabledAccounts.isEmpty() || regexList.isEmpty()) {
            ErrorHandler.emitError("Нет счетов с авто-созданием или регулярных выражений")
            return@LaunchedEffect
        }

        clipboardText = text
        for (re in regexList) {
            val parsed = TextParser.parse(text, re.pattern) ?: continue

            val matchingAccount = enabledAccounts.find { it.cardMask == parsed.cardMask }
            if (matchingAccount == null) continue

            val account = matchingAccount

            val db = AppDatabase.getDatabase(context)
            val balanceCalc = AccountBalanceCalculator(db.transactionDao())
            val dbRepo = DatabaseRepository(db.accountDao(), db.categoryDao(), db.transactionDao(), db.messageDao(), db.senderDao())

            val isIncome = parsed.isIncome

            val comment = if (!re.skipBalanceCheck && parsed.balance != null) {
                val currentBalance = balanceCalc.getAccountBalance(account.id)
                val expectedBalance = balanceCalc.calculateExpectedBalance(parsed.balance, parsed.amount)
                if (expectedBalance != null) {
                    val discrepancy = balanceCalc.getBalanceDiscrepancy(currentBalance, expectedBalance)
                    if (discrepancy != null) {
                        balanceCalc.formatDiscrepancyComment(discrepancy, expectedBalance, currentBalance)
                    } else ""
                } else ""
            } else ""

            var categoryId: UUID? = null
            if (parsed.shop.isNotBlank()) {
                val lastTx = dbRepo.getLastTransactionByShop(parsed.shop, isIncome)
                categoryId = lastTx?.categoryId
            }

            clipboardPrefillData = ClipboardPrefillData(
                amount = parsed.amount,
                accountId = java.util.UUID.fromString(account.id),
                comment = comment,
                shop = parsed.shop,
                categoryId = categoryId,
                isIncome = isIncome,
                clipboardText = text
            )
            showClipboardDialog = true
            return@LaunchedEffect
        }

        ErrorHandler.emitError("Текст не распознан")
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


