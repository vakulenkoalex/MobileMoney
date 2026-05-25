package com.mobilemoney.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilemoney.di.DI
import com.mobilemoney.viewmodel.MessageRegexFormViewModel
import kotlinx.coroutines.flow.drop
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageRegexFormScreen(
    regexId: UUID?,
    onNavigateBack: () -> Unit,
    viewModel: MessageRegexFormViewModel = DI.messageRegexFormViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(regexId) {
        viewModel.resetState()
        if (regexId != null) {
            viewModel.loadRegex(regexId)
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
                title = { Text(if (uiState.isEditing) "Редактирование regex" else "Новый regex", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.validateAndSave() }
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
            OutlinedTextField(
                value = uiState.label,
                onValueChange = { viewModel.updateLabel(it) },
                label = { Text("Название (метка) *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.labelError != null,
                supportingText = {
                    if (uiState.labelError != null) {
                        Text(uiState.labelError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.pattern,
                onValueChange = { viewModel.updatePattern(it) },
                label = { Text("Regex pattern") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.patternError != null,
                supportingText = {
                    if (uiState.patternError != null) {
                        Text(uiState.patternError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Именованные группы: amount, shop, cardMask, direction (balance опционально)")
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.skipBalanceCheck,
                    onCheckedChange = { viewModel.updateSkipBalanceCheck(it) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Не проверять баланс",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
