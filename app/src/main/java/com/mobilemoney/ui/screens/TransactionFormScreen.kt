package com.mobilemoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilemoney.data.config.AppIcons
import com.mobilemoney.data.model.CategoryUi
import com.mobilemoney.viewmodel.TransactionFormViewModel
import com.mobilemoney.viewmodel.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionId: UUID?,
    onNavigateBack: () -> Unit,
    viewModel: TransactionFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showSplitCategorySheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadTransaction(transactionId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    val filteredCategories = viewModel.getFilteredCategories()

    val amountFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (transactionId == null) {
            amountFocusRequester.requestFocus()
        }
    }

    var wasEditing by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isEditing) {
        if (wasEditing && !uiState.isEditing) {
            amountFocusRequester.requestFocus()
        }
        wasEditing = uiState.isEditing
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Редактирование" else "Создание", style = MaterialTheme.typography.titleSmall) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Тип операции
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.updateType(TransactionType.EXPENSE) },
                    label = { Text("Расход") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.updateType(TransactionType.INCOME) },
                    label = { Text("Приход") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.type == TransactionType.TRANSFER,
                    onClick = { viewModel.updateType(TransactionType.TRANSFER) },
                    label = { Text("Перевод") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Сумма
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Сумма") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester),
                leadingIcon = {
                    Text(
                        text = uiState.selectedAccount?.currency ?: "₽",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )

            // Кнопка Разделить (только при редактировании и не в режиме TRANSFER)
            if (uiState.isEditing && uiState.type != TransactionType.TRANSFER && !uiState.isSplitMode) {
                OutlinedButton(
                    onClick = { viewModel.enableSplitMode() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Разделить операцию")
                }
            }

            // Режим разделения
            if (uiState.isSplitMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Разделение операции",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { viewModel.disableSplitMode() }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }

                        // Итоговая сумма
                        Text(
                            text = "Итого: ${uiState.amount} ${uiState.selectedAccount?.currency ?: "₽"}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Оставшаяся сумма (основная категория)
                        val remainingAmount = viewModel.getRemainingAmount()
                        OutlinedTextField(
                            value = uiState.amount.toDoubleOrNull()?.let { String.format("%.2f", it - (uiState.splitAmount.toDoubleOrNull() ?: 0.0)).replace(",", ".") } ?: "",
                            onValueChange = { },
                            label = { Text(uiState.selectedCategory?.name ?: "Основная категория") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Text(
                                    text = uiState.selectedAccount?.currency ?: "₽",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        )

                        // Сумма для новой категории
                        OutlinedTextField(
                            value = uiState.splitAmount,
                            onValueChange = { viewModel.updateSplitAmount(it) },
                            label = { Text("Новая категория") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Text(
                                    text = uiState.selectedAccount?.currency ?: "₽",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        )

                        // Выбор категории для новой операции
                        ListItem(
                            headlineContent = { Text("Категория") },
                            supportingContent = { Text(uiState.splitCategory?.name ?: "Выберите категорию") },
                            leadingContent = {
                                if (uiState.splitCategory != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.getTransactionIcon(uiState.splitCategory!!.icon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                } else {
                                    Icon(Icons.Default.Category, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { showSplitCategorySheet = true }
                        )
                    }
                }
            }

            // Счёт
            ListItem(
                headlineContent = { Text("Счёт") },
                supportingContent = { Text(uiState.selectedAccount?.name ?: "Выберите счёт") },
                leadingContent = {
                    if (uiState.selectedAccount != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.getAccountIcon(uiState.selectedAccount!!.icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showAccountSheet = true }
            )

            // Категория (только для расхода и прихода)
            if (uiState.type != TransactionType.TRANSFER) {
                ListItem(
                    headlineContent = { Text("Категория") },
                    supportingContent = { Text(uiState.selectedCategory?.name ?: "Выберите категорию") },
                    leadingContent = {
                        if (uiState.selectedCategory != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.getTransactionIcon(uiState.selectedCategory!!.icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else {
                            Icon(Icons.Default.Category, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { showCategorySheet = true }
                )

                if (showCategorySheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showCategorySheet = false }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Выберите категорию",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredCategories) { category ->
                                    CategoryGridItem(
                                        category = category,
                                        selected = uiState.selectedCategory?.id == category.id,
                                        onClick = {
                                            viewModel.updateCategory(category)
                                            showCategorySheet = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Целевой счёт (только для перевода)
            if (uiState.type == TransactionType.TRANSFER) {
                ListItem(
                    headlineContent = { Text("На счёт") },
                    supportingContent = { Text(uiState.targetAccount?.name ?: "Выберите целевой счёт") },
                    leadingContent = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { showTargetAccountSheet = true }
                )

                if (showTargetAccountSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showTargetAccountSheet = false }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Выберите целевой счёт",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            uiState.accounts.filter { it.id != uiState.selectedAccount?.id }.forEach { account ->
                                ListItem(
                                    headlineContent = { Text(account.name) },
                                    leadingContent = { Icon(AppIcons.getAccountIcon(account.icon), contentDescription = null) },
                                    modifier = Modifier.clickable {
                                        viewModel.updateTargetAccount(account)
                                        showTargetAccountSheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom sheet для выбора категории разделения
            if (showSplitCategorySheet) {
                val splitFilteredCategories = viewModel.getSplitFilteredCategories()
                ModalBottomSheet(
                    onDismissRequest = { showSplitCategorySheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Выберите категорию",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(splitFilteredCategories) { category ->
                                CategoryGridItem(
                                    category = category,
                                    selected = uiState.splitCategory?.id == category.id,
                                    onClick = {
                                        viewModel.updateSplitCategory(category)
                                        showSplitCategorySheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Выбор счёта
            if (showAccountSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAccountSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Выберите счёт",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        uiState.accounts.forEach { account ->
                            ListItem(
                                headlineContent = { Text(account.name) },
                                leadingContent = {
                                    Icon(
                                        AppIcons.getAccountIcon(account.icon),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.updateAccount(account)
                                    showAccountSheet = false
                                }
                            )
                        }
                    }
                }
            }

            // Дата и время
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true }
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Дата", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dateFormat.format(Date(uiState.date)))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker = true }
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Время", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(timeFormat.format(Date(uiState.date)))
                    }
                }
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.date
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    viewModel.updateDate(it)
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Отмена")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showTimePicker) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = uiState.date
                }
                val timePickerState = rememberTimePickerState(
                    initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                    initialMinute = calendar.get(Calendar.MINUTE)
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val newCalendar = Calendar.getInstance().apply {
                                    timeInMillis = uiState.date
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                }
                                viewModel.updateDate(newCalendar.timeInMillis)
                                showTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Отмена")
                        }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }

            // Комментарий
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = { viewModel.updateComment(it) },
                label = { Text("Комментарий") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Ошибка
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Кнопки действий (только при редактировании)
            if (uiState.isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.copy() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Копировать")
                    }
                    OutlinedButton(
                        onClick = { viewModel.delete() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Удалить")
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGridItem(
    category: CategoryUi,
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
                imageVector = AppIcons.getTransactionIcon(category.icon),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}