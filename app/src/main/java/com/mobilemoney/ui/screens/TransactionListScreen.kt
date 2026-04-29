package com.mobilemoney.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilemoney.data.config.AppIcons
import com.mobilemoney.data.model.TransactionUi
import com.mobilemoney.ui.utils.FormatUtils
import com.mobilemoney.viewmodel.TransactionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = viewModel(),
    onAddClick: () -> Unit,
    onTransactionClick: (UUID) -> Unit,
    onAccountsClick: () -> Unit,
    onCategoriesClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Операции") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
        },
        bottomBar = {
            BottomNavigationBar(
                onAccountsClick = onAccountsClick,
                onCategoriesClick = onCategoriesClick
            )
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
    transaction: TransactionUi,
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

@Composable
fun BottomNavigationBar(
    onAccountsClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {}
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Главная") },
            selected = false,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
            label = { Text("Счета") },
            selected = false,
            onClick = onAccountsClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Category, contentDescription = null) },
            label = { Text("Категории") },
            selected = false,
            onClick = onCategoriesClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Настройки") },
            selected = false,
            onClick = { }
        )
    }
}


