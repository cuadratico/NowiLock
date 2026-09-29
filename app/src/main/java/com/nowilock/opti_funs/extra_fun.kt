package com.nowilock.opti_funs

import android.app.Activity
import android.app.Dialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.nowilock.R
import com.nowilock.name_p_hash
import kotlinx.coroutines.flow.SharingCommand
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec


fun create_biometric (context: AppCompatActivity, succ: () -> Unit, error: () -> Unit) {
    BiometricPrompt(context, ContextCompat.getMainExecutor(context), object: BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            succ()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            error()
        }
    }).authenticate(
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle("Auth")
            setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        }.build()
    )
}

fun create_dialog (context: Activity, view: Int): Pair<Dialog, View> {
    val view = LayoutInflater.from(context).inflate(view, null)
    val dialog = Dialog(context).apply {
        setContentView(view)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }
    dialog.show()
    return Pair(dialog, view)
}

fun load (context: Activity, anim: Int, load_text: String): Dialog {
    val (dialog, view) = create_dialog(context, R.layout.load)

    val info = view.findViewById<TextView>(R.id.info)
    info.text = "$load_text..."

    val animation = view.findViewById<LottieAnimationView>(R.id.anim)
    animation.setAnimation(anim)

    dialog.setCancelable(false)

    return dialog
}

fun cip_ins (pref: SharedPreferences, ins: Int, iv: String = ""): Cipher {

    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    val c = Cipher.getInstance("AES/GCM/NoPadding")
    if (ins == Cipher.ENCRYPT_MODE) {
        return c.apply { init(ins, ks.getKey(pref.getString(name_p_hash, ""), null)) }
    } else {
        return c.apply { init(ins, ks.getKey(pref.getString(name_p_hash, ""), null), GCMParameterSpec(128, Base64.getDecoder().decode(iv))) }
    }

}