package com.mishr.habittracker.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mishr.habittracker.domain.model.MigrationStatus

@Composable
fun MigrationScreen(
    onMigrationComplete: () -> Unit,
    onContinueAsGuest: () -> Unit,
    viewModel: MigrationViewModel = hiltViewModel()
) {
    val status by viewModel.migrationStatus.collectAsState()

    if (status is MigrationStatus.Completed) {
        onMigrationComplete()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (status) {
                is MigrationStatus.NotStarted -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preparing to sync your data...", style = MaterialTheme.typography.titleMedium)
                }
                is MigrationStatus.InProgress -> {
                    val progress = (status as MigrationStatus.InProgress).progress
                    CircularProgressIndicator(progress = progress)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Syncing to cloud...", style = MaterialTheme.typography.titleMedium)
                }
                is MigrationStatus.Failed -> {
                    val failed = status as MigrationStatus.Failed
                    Text("Migration Failed", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(failed.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    if (failed.canRetry) {
                        Button(onClick = { viewModel.retry() }) {
                            Text("Retry")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedButton(onClick = { onContinueAsGuest() }) {
                        Text("Continue in Guest Mode for now")
                    }
                }
                is MigrationStatus.Completed -> {
                    Text("Migration Complete!", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}
