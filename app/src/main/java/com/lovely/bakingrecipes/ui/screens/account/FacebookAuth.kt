package com.lovely.bakingrecipes.ui.screens.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.lovely.bakingrecipes.R

// True only when a real Facebook app id has been configured in strings.xml.
@Composable
fun isFacebookConfigured(): Boolean {
    val context = LocalContext.current
    return remember { context.getString(R.string.facebook_app_id).isNotBlank() }
}

@Composable
fun FacebookSignInButton(
    enabled: Boolean,
    onToken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val callbackManager = remember { CallbackManager.Factory.create() }

    val launcher = rememberLauncherForActivityResult(
        LoginManager.getInstance().createLogInActivityResultContract(callbackManager)
    ) { /* Result is delivered through the registered callback below. */ }

    DisposableEffect(Unit) {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) = onToken(result.accessToken.token)
                override fun onCancel() {}
                override fun onError(error: FacebookException) {}
            }
        )
        onDispose { LoginManager.getInstance().unregisterCallback(callbackManager) }
    }

    OutlinedButton(
        onClick = { launcher.launch(listOf("email", "public_profile")) },
        enabled = enabled,
        modifier = modifier
    ) {
        Text("Continue with Facebook")
    }
}
