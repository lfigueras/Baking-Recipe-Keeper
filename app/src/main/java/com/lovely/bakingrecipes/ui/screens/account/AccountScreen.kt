package com.lovely.bakingrecipes.ui.screens.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.lovely.bakingrecipes.ui.components.brandedTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    user: FirebaseUser?,
    isBusy: Boolean,
    message: String?,
    onMessageShown: () -> Unit,
    onSignInEmail: (String, String) -> Unit,
    onRegisterEmail: (String, String) -> Unit,
    onGoogleIdToken: (String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignOut: () -> Unit,
    onEditProfile: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    // Look up the generated web client id at runtime; absent if Google sign-in isn't enabled.
    val webClientId = remember {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id != 0) context.getString(id) else null
    }
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
        }.getOrNull()?.idToken?.let(onGoogleIdToken)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = brandedTopAppBarColors(),
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (user != null) {
                SignedInContent(user = user, onSignOut = onSignOut, onEditProfile = onEditProfile)
            } else {
                SignInContent(
                    isBusy = isBusy,
                    showGoogle = webClientId != null,
                    onSignInEmail = onSignInEmail,
                    onRegisterEmail = onRegisterEmail,
                    onForgotPassword = onForgotPassword,
                    onGoogleClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(webClientId!!)
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        client.signOut()
                        googleLauncher.launch(client.signInIntent)
                    }
                )
            }
        }
    }
}

@Composable
private fun SignedInContent(
    user: FirebaseUser,
    onSignOut: () -> Unit,
    onEditProfile: () -> Unit
) {
    val photoUrl = user.photoUrl?.toString()
    if (photoUrl != null) {
        coil.compose.AsyncImage(
            model = photoUrl,
            contentDescription = "Profile photo",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
        )
    } else {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }

    Text(
        text = user.displayName?.takeIf { it.isNotBlank() } ?: "Signed in",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp)
    )
    Text(
        text = user.email?.takeIf { it.isNotBlank() } ?: "No email shared",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val providers = user.providerData
        .mapNotNull { providerLabel(it.providerId) }
        .distinct()
    if (providers.isNotEmpty()) {
        Text(
            text = "Signed in with ${providers.joinToString(", ")}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileRow(label = "Name", value = user.displayName?.takeIf { it.isNotBlank() } ?: "—")
            ProfileRow(label = "Email", value = user.email?.takeIf { it.isNotBlank() } ?: "—")
            ProfileRow(
                label = "Email verified",
                value = if (user.isEmailVerified) "Yes" else "No"
            )
            ProfileRow(label = "User ID", value = user.uid)
        }
    }

    Text(
        text = "Your recipes will sync to this account across devices.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp)
    )
    Button(
        onClick = onEditProfile,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text("Edit profile")
    }
    OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text("Sign out")
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

private fun providerLabel(providerId: String): String? = when (providerId) {
    "facebook.com" -> "Facebook"
    "google.com" -> "Google"
    "password" -> "Email"
    else -> null
}

@Composable
private fun SignInContent(
    isBusy: Boolean,
    showGoogle: Boolean,
    onSignInEmail: (String, String) -> Unit,
    onRegisterEmail: (String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onGoogleClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Text(
        text = "Sign in to sync your recipes",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = "Keep your recipes safe and available on any device.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )

    Button(
        onClick = { onSignInEmail(email, password) },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text("Sign in")
        }
    }
    OutlinedButton(
        onClick = { onRegisterEmail(email, password) },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Create account")
    }
    TextButton(onClick = { onForgotPassword(email) }) {
        Text("Forgot password?")
    }

    if (showGoogle) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        OutlinedButton(
            onClick = onGoogleClick,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue with Google")
        }
    }
}
