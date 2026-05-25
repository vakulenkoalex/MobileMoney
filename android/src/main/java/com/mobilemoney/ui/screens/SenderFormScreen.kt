package com.mobilemoney.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilemoney.data.local.SenderType
import com.mobilemoney.di.DI
import com.mobilemoney.viewmodel.SenderFormViewModel
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderFormScreen(
    senderId: String?,
    onNavigateBack: () -> Unit,
    viewModel: SenderFormViewModel = DI.senderFormViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(senderId) {
        viewModel.resetState()
        if (senderId != null) {
            viewModel.loadSender(senderId)
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
                title = { Text(
                    if (uiState.isEditing) "Редактирование отправителя"
                    else "Новый отправитель",
                    style = MaterialTheme.typography.titleSmall
                ) },
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
                value = uiState.senderNumber,
                onValueChange = { viewModel.updateSenderNumber(it) },
                label = { Text("Идентификатор отправителя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.label,
                onValueChange = { viewModel.updateLabel(it) },
                label = { Text("Метка") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Вид",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SenderType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.senderType == type,
                        onClick = { viewModel.updateSenderType(type) },
                        label = { Text(type.displayName) }
                    )
                }
            }
        }
    }
}
