package com.mobilemoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilemoney.viewmodel.AccountFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    accountId: java.util.UUID?,
    onNavigateBack: () -> Unit,
    viewModel: AccountFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        if (accountId != null) {
            viewModel.loadAccount(accountId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Редактирование счёта" else "Новый счёт") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() }
                    ) {
                        Text("Сохранить", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Название счёта") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ListItem(
                headlineContent = { Text("Валюта") },
                supportingContent = {
                    Text(
                        uiState.currencies.find { it.code == uiState.currencyCode }?.name
                            ?: uiState.currencyCode
                    )
                },
                leadingContent = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showCurrencySheet = true }
            )

            if (showCurrencySheet) {
                ModalBottomSheet(
                    onDismissRequest = { showCurrencySheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Выберите валюту",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        uiState.currencies.forEach { currency ->
                            ListItem(
                                headlineContent = { Text(currency.name) },
                                supportingContent = { Text(currency.symbol) },
                                leadingContent = {
                                    RadioButton(
                                        selected = uiState.currencyCode == currency.code,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.updateCurrency(currency.symbol)
                                    showCurrencySheet = false
                                }
                            )
                        }
                    }
                }
            }

            ListItem(
                headlineContent = { Text("Иконка") },
                supportingContent = {
                    Text(
                        uiState.icons.find { it.name == uiState.icon }?.label
                            ?: "Стандартная"
                    )
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAccountIcon(uiState.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showIconSheet = true }
            )

            if (showIconSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showIconSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Выберите иконку",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.icons) { iconOption ->
                                AccountIconItem(
                                    iconOption = iconOption,
                                    selected = uiState.icon == iconOption.name,
                                    onClick = {
                                        viewModel.updateIcon(iconOption.name)
                                        showIconSheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun AccountIconItem(
    iconOption: com.mobilemoney.viewmodel.IconOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getAccountIcon(iconOption.name),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = iconOption.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

fun getAccountIcon(iconName: String): ImageVector {
    return when (iconName) {
        "wallet" -> Icons.Default.AccountBalanceWallet
        "credit_card" -> Icons.Default.CreditCard
        "account_balance" -> Icons.Default.AccountBalance
        "savings" -> Icons.Default.Savings
        "home" -> Icons.Default.Home
        "business" -> Icons.Default.Business
        "school" -> Icons.Default.School
        "favorite" -> Icons.Default.Favorite
        else -> Icons.Default.AccountBalanceWallet
    }
}