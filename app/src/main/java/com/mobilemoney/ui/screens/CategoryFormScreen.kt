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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilemoney.data.config.AppIcons
import com.mobilemoney.data.config.CategoryIconOption
import com.mobilemoney.viewmodel.CategoryFormViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(
    categoryId: UUID?,
    onNavigateBack: () -> Unit,
    viewModel: CategoryFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showIconSheet by remember { mutableStateOf(false) }

    LaunchedEffect(categoryId) {
        if (categoryId != null) {
            viewModel.loadCategory(categoryId)
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
                title = { Text(if (uiState.isEditing) "Редактирование категории" else "Новая категория") },
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
                label = { Text("Название категории") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !uiState.isIncome,
                    onClick = { viewModel.updateIsIncome(false) },
                    label = { Text("Расход") },
                    leadingIcon = if (!uiState.isIncome) {
                        { Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.isIncome,
                    onClick = { viewModel.updateIsIncome(true) },
                    label = { Text("Доход") },
                    leadingIcon = if (uiState.isIncome) {
                        { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
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
                            imageVector = AppIcons.getCategoryIcon(uiState.icon),
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
                                CategoryIconItem(
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
fun CategoryIconItem(
    iconOption: CategoryIconOption,
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
                imageVector = AppIcons.getCategoryIcon(iconOption.name),
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