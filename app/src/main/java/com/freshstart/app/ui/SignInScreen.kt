package com.freshstart.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.freshstart.app.data.Account
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(account: Account, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun run(block: suspend () -> Unit) {
        busy = true
        message = null
        scope.launch {
            runCatching { block() }.onFailure { message = authError(it) }
            busy = false
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Welcome to FreshStart", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Sign in to keep your plan, meals and progress safe, even if you change phones.",
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = { activity?.let { a -> run { account.signInWithGoogle(a) } } },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue with Google") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(if (creating) "Create an account with email" else "Or sign in with email", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(if (creating) "Choose a password (6+ characters)" else "Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        val canSubmit = !busy && email.contains("@") && password.length >= 6
        Button(
            onClick = {
                run { if (creating) account.createAccount(email, password) else account.signInWithEmail(email, password) }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (creating) "Create account" else "Sign in") }
        TextButton(onClick = { creating = !creating; message = null }) {
            Text(if (creating) "I already have an account" else "New here? Create an account")
        }
        if (!creating) {
            TextButton(enabled = !busy && email.contains("@"), onClick = {
                run {
                    account.sendPasswordReset(email)
                    message = "Check your email for a link to reset your password."
                }
            }) { Text("Forgot password?") }
        }

        if (busy) CircularProgressIndicator(Modifier.size(28.dp))
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        OutlinedButton(onClick = onSkip, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Use without an account")
        }
        Text(
            "Without an account, your data stays only on this phone and is lost if you uninstall the app. " +
                "You can sign in later from Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun authError(t: Throwable): String = when (t) {
    is Account.AccountException -> t.message ?: "Something went wrong."
    is FirebaseAuthWeakPasswordException -> "Please choose a longer password (at least 6 characters)."
    is FirebaseAuthUserCollisionException -> "There's already an account with that email. Try signing in instead."
    is FirebaseAuthInvalidUserException -> "No account found with that email. Tap \"Create an account\"."
    is FirebaseAuthInvalidCredentialsException -> "That email or password isn't right."
    is androidx.credentials.exceptions.GetCredentialCancellationException -> "Sign-in was cancelled."
    is androidx.credentials.exceptions.NoCredentialException -> "No Google account found on this phone. Add one in Android settings or use email."
    is java.io.IOException -> "Couldn't connect. Check your internet connection."
    else -> t.message ?: "Something went wrong. Please try again."
}
