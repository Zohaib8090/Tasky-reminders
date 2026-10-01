package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Unlocks locked tasks/notes with the device's own security: fingerprint, face,
 * or the screen-lock PIN / pattern / password as the password fallback.
 */
object BiometricAuth {
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        if (!isAvailable(activity)) {
            Toast.makeText(
                activity,
                "Set up a screen lock or fingerprint/face unlock in device settings first",
                Toast.LENGTH_LONG
            ).show()
            onFailure()
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Returns a function that asks the user to authenticate, then runs onSuccess. */
@Composable
fun rememberAuthPrompt(): (title: String, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { title, onSuccess, onFailure ->
            val activity = context.findFragmentActivity()
            if (activity == null) onFailure() else BiometricAuth.authenticate(activity, title, onSuccess, onFailure)
        }
    }
}
