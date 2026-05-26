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
import com.mobilemoney.data.config.AppIcons
import com.mobilemoney.data.local.TransactionSource
import com.mobilemoney.di.DI
import com.mobilemoney.domain.model.Category
import com.mobilemoney.viewmodel.TransactionFormViewModel
import com.mobilemoney.viewmodel.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionId: UUID?,
    onNavigateBack: () -> Unit,
    viewModel: TransactionFormViewModel = DI.transactionFormViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var selectedRootCategory by remember { mutableStateOf<Category?>(null) }
    var showSplitCategorySheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadTransaction(transactionId)
        } else if (viewModel.uiState.value.clipboardText == null) {
            viewModel.resetForNewTransaction()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
            viewModel.resetSavedState()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
            viewModel.resetSavedState()
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
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            // Сумма + Дата + Время
            val dateFormat = SimpleDateFormat("dd.MM.yy")
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.updateAmount(it) },
                    label = { Text("Сумма") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(amountFocusRequester),
                    leadingIcon = {
                        Text(
                            text = uiState.selectedAccount?.currency ?: "₽",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                )
                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = dateFormat.format(Date(uiState.date)),
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Дата") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = Color.Transparent
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .clickable { showTimePicker = true }
                ) {
                    OutlinedTextField(
                        value = timeFormat.format(Date(uiState.date)),
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Время") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = Color.Transparent
                        )
                    )
                }
            }

            // Счёт
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showAccountSheet = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.selectedAccount != null)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.selectedAccount != null)
                            AppIcons.getAccountIcon(uiState.selectedAccount!!.icon)
                        else Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (uiState.selectedAccount != null)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.selectedAccount?.name ?: "Выберите счёт",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Категория (только для расхода и прихода)
            if (uiState.type != TransactionType.TRANSFER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { showCategorySheet = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.selectedCategory != null)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.selectedCategory != null)
                                AppIcons.getTransactionIcon(uiState.selectedCategory!!.icon)
                            else Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (uiState.selectedCategory != null)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val categoryLabel = uiState.selectedCategory?.let { cat ->
                        val parent = uiState.categories.find { it.id == cat.parentId }
                        if (parent != null) "${parent.name} → ${cat.name}" else cat.name
                    } ?: "Выберите категорию"
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (showCategorySheet) {
                    val rootCategories = viewModel.getRootCategories()
                    ModalBottomSheet(
                        onDismissRequest = {
                            showCategorySheet = false
                            selectedRootCategory = null
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (selectedRootCategory == null) {
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
                                        items(rootCategories) { category ->
                                            CategoryGridItem(
                                                category = category,
                                                selected = uiState.selectedCategory?.id == category.id,
                                                onClick = {
                                                    val hasChildren = viewModel.getCategoryWithChildren(category.id).size > 1
                                                    if (hasChildren) {
                                                        selectedRootCategory = category
                                                    } else {
                                                        viewModel.updateCategory(category)
                                                        showCategorySheet = false
                                                    }
                                                }
                                            )
                                        }
                                }
                            } else {
                                val categoryList = viewModel.getCategoryWithChildren(selectedRootCategory!!.id)
                                Text(
                                    text = selectedRootCategory!!.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(categoryList) { category ->
                                        CategoryGridItem(
                                            category = category,
                                            selected = uiState.selectedCategory?.id == category.id,
                                            onClick = {
                                                viewModel.updateCategory(category)
                                                showCategorySheet = false
                                                selectedRootCategory = null
                                            }
                                        )
                                    }
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
                val splitFilteredCategories = viewModel.getFilteredCategories()
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

            // Магазин
            OutlinedTextField(
                value = uiState.shop,
                onValueChange = { viewModel.updateShop(it) },
                label = { Text("Магазин") },
                modifier = Modifier.fillMaxWidth()
            )

            // Комментарий
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = { viewModel.updateComment(it) },
                label = { Text("Комментарий") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Источник
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.source == TransactionSource.MANUAL,
                    onClick = { viewModel.updateSource(TransactionSource.MANUAL) },
                    label = { Text("Ручной") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.source == TransactionSource.SMS,
                    onClick = { viewModel.updateSource(TransactionSource.SMS) },
                    label = { Text("SMS") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.source == TransactionSource.PUSH,
                    onClick = { viewModel.updateSource(TransactionSource.PUSH) },
                    label = { Text("Push") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.source == TransactionSource.CLIPBOARD,
                    onClick = { viewModel.updateSource(TransactionSource.CLIPBOARD) },
                    label = { Text("Буфер") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Данные источника
            OutlinedTextField(
                value = uiState.sourceData,
                onValueChange = { viewModel.updateSourceData(it) },
                label = { Text("Данные источника") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
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
    category: Category,
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