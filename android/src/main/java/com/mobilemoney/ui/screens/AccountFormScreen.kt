package com.mobilemoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.mobilemoney.ui.config.AppIcons
import com.mobilemoney.ui.config.IconOption

import com.mobilemoney.viewmodel.AccountFormViewModel
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    accountId: java.util.UUID?,
    onNavigateBack: () -> Unit,
    viewModel: AccountFormViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }
    var showTypeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        viewModel.resetState()
        if (accountId != null) {
            viewModel.loadAccount(accountId)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.isSaved }
            .drop(1)
            .collect { if (it) onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Редактирование счёта" else "Новый счёт", style = MaterialTheme.typography.titleSmall) },
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
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showIconSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.getAccountIcon(uiState.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                OutlinedTextField(
                    value = uiState.name.value,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text(uiState.name.label) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.name.error != null,
                    supportingText = uiState.name.error?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showTypeSheet = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.type.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (showTypeSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showTypeSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Выберите тип счёта",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        uiState.accountTypes.forEach { type ->
                            ListItem(
                                headlineContent = { Text(type.displayName) },
                                leadingContent = {
                                    RadioButton(
                                        selected = uiState.type == type,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.updateType(type)
                                    showTypeSheet = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showCurrencySheet = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.currencies.find { it.code == uiState.currencyCode }?.name
                        ?: uiState.currencyCode,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

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
                                    viewModel.updateCurrency(currency.code)
                                    showCurrencySheet = false
                                }
                            )
                        }
                    }
                }
            }



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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.isDefault,
                    onCheckedChange = { viewModel.updateIsDefault(it) }
                )
                Text(
                    text = "Использовать для новых операций",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.autoCreateEnabled,
                    onCheckedChange = { viewModel.updateAutoCreateEnabled(it) }
                )
                Text(
                    text = "Создавать операции из скопированного текста",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (uiState.autoCreateEnabled) {
                OutlinedTextField(
                    value = uiState.cardMask.value,
                    onValueChange = { viewModel.updateCardMask(it) },
                    label = { Text(uiState.cardMask.label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.cardMask.error != null,
                    supportingText = uiState.cardMask.error?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    }
                )
            }
        }
    }
}

@Composable
fun AccountIconItem(
    iconOption: IconOption,
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
                imageVector = AppIcons.getAccountIcon(iconOption.name),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

