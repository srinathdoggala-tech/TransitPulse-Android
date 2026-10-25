package com.mishr.habittracker.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mishr.habittracker.core.common.Constants
import com.mishr.habittracker.domain.model.AuthState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSignOutWarning by remember { mutableStateOf(false) }
    var showDeleteWarning by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.NavigateToOnboarding -> onNavigateToOnboarding()
                is SettingsEffect.ShowError -> Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                is SettingsEffect.RequiresReauthForDeletion -> {
                    Toast.makeText(context, "Recent login required to delete account.", Toast.LENGTH_LONG).show()
                    coroutineScope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(Constants.WEB_CLIENT_ID)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val result = credentialManager.getCredential(context, request)
                            val credential = result.credential
                            if (credential is GoogleIdTokenCredential) {
                                viewModel.processIntent(SettingsIntent.ReauthenticateAndDelete(credential.idToken))
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    if (showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteWarning = false },
            title = { Text("Delete Account") },
            text = { Text("Are you absolutely sure? This will permanently delete your account and all associated cloud data. This action CANNOT be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteWarning = false
                        viewModel.processIntent(SettingsIntent.DeleteAccount)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showSignOutWarning) {
        AlertDialog(
            onDismissRequest = { showSignOutWarning = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have ${state.pendingSyncCount} changes waiting to sync to the cloud. Signing out now will permanently delete these changes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutWarning = false
                        viewModel.processIntent(SettingsIntent.SignOut)
                    }
                ) {
                    Text("Sign Out & Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val auth = state.authState) {
                is AuthState.Authenticated -> {
                    Text("Signed in as ${auth.email ?: "Unknown"}")
                    OutlinedButton(onClick = { 
                        if (state.pendingSyncCount > 0) {
                            showSignOutWarning = true
                        } else {
                            viewModel.processIntent(SettingsIntent.SignOut)
                        }
                    }) {
                        Text("Sign Out")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDeleteWarning = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Account")
                    }
                }
                AuthState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                AuthState.Unauthenticated -> {
                    Text("Currently using Guest Mode")
                    Button(onClick = onNavigateToOnboarding) {
                        Text("Sign In")
                    }
                }
            }
        }
    }
}
