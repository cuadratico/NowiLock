package com.nowilock

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.KeyGenerator

class RegisterActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register_activity)


        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES

        val opor = findViewById<TextView>(R.id.opor)
        val input_pass = findViewById<EditText>(R.id.input_pass)
        val progress = findViewById<LinearProgressIndicator>(R.id.progress)
        val visible_all = findViewById<ConstraintLayout>(R.id.visible_all)
        val visible_icon = findViewById<ShapeableImageView>(R.id.visible_icon)
        val create = findViewById<ConstraintLayout>(R.id.bottom_create)
        create.visibility = View.INVISIBLE

        val mk = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(this, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        fun block () {
            val dialog_block = Dialog(this)
            val view_block = LayoutInflater.from(this).inflate(R.layout.block, null)

            val time_output = view_block.findViewById<TextView>(R.id.time)

            lifecycleScope.launch (Dispatchers.IO){

                for (time in (60 * pref.getInt("multi", 1)).downTo(0)) {

                    withContext(Dispatchers.Main) {
                        time_output.text = time.toString()
                    }
                    delay(1000)
                }

                pref.edit().putBoolean("block", false).commit()
                pref.edit().putInt("opor", 9).commit()
                pref.edit().putInt("multi", pref.getInt("multi", 1) + 1).commit()
                withContext(Dispatchers.Main) {
                    recreate()
                }
            }


            dialog_block.setContentView(view_block)
            dialog_block.setCancelable(false)
            dialog_block.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog_block.show()
        }

        if (!pref.getBoolean("start", false))  {
            opor.visibility = View.INVISIBLE
            create.visibility = View.VISIBLE
        }else {
            if (!pref.getBoolean("block", false)) {
                opor.text = "*".repeat(pref.getInt("opor", 9) - 1)
            }else {
                block()
            }
        }

        input_pass.addTextChangedListener {dato ->

            if (dato!!.isNotEmpty() && pref.getBoolean("start", false)) {

                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (input_pass.text.length == pref.getString("key", "")?.length) {

                    if (ks.getKey(input_pass.text.toString(), null) != null && Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(input_pass.text.toString().toByteArray())) == pref.getString("hash", "")) {

                        if (androidx.biometric.BiometricManager.from(this).canAuthenticate(androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {

                            val promt = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Who are you?")
                                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                .setConfirmationRequired(true)
                                .build()

                            androidx.biometric.BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {

                                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        val intent =
                                            Intent(applicationContext, MainActivity::class.java)
                                                .putExtra("ali", input_pass.text.toString())
                                        startActivity(intent)
                                        finish()
                                    }

                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        super.onAuthenticationError(errorCode, errString)
                                        input_pass.setText("")
                                        recreate()
                                    }
                                }).authenticate(promt)
                        }

                    } else {
                        pref.edit().putInt("opor", pref.getInt("opor", 9) - 1).commit()

                        if (pref.getInt("opor", 9) <= 1) {
                            pref.edit().putBoolean("block", true).commit()
                            block()
                        } else {
                            opor.text = "*".repeat(pref.getInt("opor", 9) - 1)
                            input_pass.setText("")
                        }
                    }
                }
            }
            entropy(dato.toString(), progress)
        }

        var visi = false
        visible_all.setOnClickListener {

            if (visi) {
                input_pass.transformationMethod = PasswordTransformationMethod.getInstance()
                visible_icon.setImageResource(R.drawable.close_eye)
                visi = false
            }else {
                input_pass.transformationMethod = null
                visible_icon.setImageResource(R.drawable.open_eye)
                visi = true
            }

            input_pass.setSelection(input_pass.text.length)
        }

        create.setOnClickListener {

            if (input_pass.text.isNotEmpty()) {

                val kgs = KeyGenParameterSpec.Builder(input_pass.text.toString(), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                    init(kgs)
                }
                kg.generateKey()

                pref.edit().putBoolean("start", true).commit()
                pref.edit().putString("key", input_pass.text.toString()).commit()
                pref.edit().putString("hash", Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(input_pass.text.toString().toByteArray()))).commit()

                val intent = Intent(this, MainActivity::class.java)
                    .putExtra("ali", input_pass.text.toString())

                startActivity(intent)
                finish()

            }else {
                Toast.makeText(this, "You need to specify a password", Toast.LENGTH_SHORT).show()
            }
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}