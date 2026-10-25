package com.mishr.habittracker.feature.onboarding

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mishr.habittracker.core.common.Constants
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToMigration: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OnboardingEffect.NavigateToHome -> onNavigateToHome()
                is OnboardingEffect.NavigateToMigration -> onNavigateToMigration()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Loopwell",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                
                var isSigningIn by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        isSigningIn = true
                        coroutineScope.launch {
                            signInWithGoogle(context) { result ->
                                isSigningIn = false
                                result.onSuccess {
                                    viewModel.handleIntent(OnboardingIntent.SignInWithGoogle(it))
                                }.onFailure {
                                    viewModel.handleIntent(OnboardingIntent.SignInFailed(it.message ?: "Unknown error"))
                                }
                            }
                        }
                    },
                    enabled = !isSigningIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { viewModel.handleIntent(OnboardingIntent.ContinueAsGuest) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue as Guest")
                }
            }

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private suspend fun signInWithGoogle(context: Context, onResult: (Result<String>) -> Unit) {
    val credentialManager = CredentialManager.create(context)

    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(Constants.WEB_CLIENT_ID)
        .build()

    val request: GetCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(
            request = request,
            context = context
        )
        
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            onResult(Result.success(googleIdTokenCredential.idToken))
        } else {
            Log.e("Auth", "Unexpected type of credential")
            onResult(Result.failure(Exception("Unknown error")))
        }
    } catch (e: GetCredentialException) {
        Log.e("Auth", "GetCredentialException", e)
        onResult(Result.failure(e))
    } catch (e: Exception) {
        Log.e("Auth", "Unknown exception", e)
        onResult(Result.failure(e))
    }
}

