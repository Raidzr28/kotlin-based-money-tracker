package com.moneymanager.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/*
 * The fingerprint or face path into the app.
 *
 * Strong biometrics only. The weak class covers sensors the platform does not consider reliable
 * enough to gate credentials with, and this gates someone's financial record.
 *
 * Biometrics never replace the PIN, they shortcut it: a sensor can fail, a finger can be taped
 * over, and the user can always fall back to typing. That is also why nothing here can set or
 * change a PIN -- this only opens a lock that a PIN already closed.
 */

/** Whether this device has a usable strong biometric enrolled right now. */
@Composable
fun biometricAvailable(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
}

/**
 * Returns a function that shows the system biometric prompt, or null when the device cannot.
 *
 * A null return is the caller's signal to hide the affordance rather than to show a button that
 * fails when pressed.
 */
@Composable
fun rememberBiometricPrompt(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFailed: (String) -> Unit = {},
): (() -> Unit)? {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val available = biometricAvailable()
    if (activity == null || !available) return null

    return remember(activity, onSuccess) {
        {
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                        onSuccess()

                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        // Cancelling is not an error worth reporting: the user chose the PIN.
                        if (code != BiometricPrompt.ERROR_USER_CANCELED &&
                            code != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            code != BiometricPrompt.ERROR_CANCELED
                        ) {
                            onFailed(message.toString())
                        }
                    }
                },
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText("Use PIN")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                    .setConfirmationRequired(false)
                    .build()
            )
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
