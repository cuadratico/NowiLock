package com.nowilock

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.KeyGenerator

class RegisterActivity : AppCompatActivity() {

    private var back_all: ConstraintLayout? = null
    private var back_bio_error: ConstraintLayout? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register_activity)


        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES

        back_all = findViewById(R.id.back_all)
        val info = findViewById<TextView>(R.id.info)
        val opor = findViewById<TextView>(R.id.opor)
        val input_pass = findViewById<EditText>(R.id.input_pass)
        val progress = findViewById<LinearProgressIndicator>(R.id.progress)
        val create = findViewById<ShapeableImageView>(R.id.bottom_create)

        back_bio_error = findViewById(R.id.back_all_bio_error)
        val info_bio_error = findViewById<ShapeableImageView>(R.id.info_bio_error)

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
            info.text = "Create your password to encrypt your records in NowiLock"
        }else {
            if (!pref.getBoolean("block", false)) {
                opor.text = "*".repeat(pref.getInt("opor", 9) - 1)
            }else {
                block()
            }
        }

        input_pass.addTextChangedListener {
            entropy(it.toString(), progress)
        }

        fun secure_update () {
            pref.edit().putString("key_u", input_pass.text.toString()).commit()
            pref.edit().putString("salt", Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16))).commit()
            pref.edit().putString("hash", Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(input_pass.text.toString().toByteArray() + Base64.getDecoder().decode(pref.getString("salt", "")) ))).commit()
        }

        info_bio_error.setOnClickListener {

            MaterialAlertDialogBuilder(this).apply {
                setTitle("Because you cannot log in")
                setMessage("You cannot log in because you have neither biometric data nor a PIN set up on your device. NowiLock requires this type of authentication to be used securely.")
                setPositiveButton("Set up a PIN or biometric data") {_, _ ->
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                setNegativeButton("Later") {_, _ -> }
            }.show()
        }

        create.setOnClickListener {
            if (input_pass.text.isNotEmpty()) {

                if (!pref.getBoolean("start", false)) {

                    val kgs = KeyGenParameterSpec.Builder(input_pass.text.toString(), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()

                    val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                                init(kgs)
                    }
                    kg.generateKey()

                    pref.edit().putBoolean("start", true).commit()
                    secure_update()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    if (MessageDigest.isEqual(Base64.getDecoder().decode(pref.getString("hash", "")), MessageDigest.getInstance("SHA256").digest(input_pass.text.toString().toByteArray() + Base64.getDecoder().decode(pref.getString("salt", ""))))) {
                        val promt = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Authenticate yourself")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            .build()

                        BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)

                                secure_update()

                                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                                finish()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(this@RegisterActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                                input_pass.setText("")
                            }
                        }).authenticate(promt)

                    } else {
                        input_pass.setText("")

                        if (opor.text.length == 1) {
                            pref.edit().putBoolean("block", true).commit()
                            recreate()
                        } else {
                            pref.edit().putInt("opor", pref.getInt("opor", 9) - 1).commit()
                            opor.text = "*".repeat(pref.getInt("opor", 9) - 1)
                        }

                    }
                }

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

    override fun onResume() {
        super.onResume()

        if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) {
            back_all?.visibility = View.INVISIBLE
            back_bio_error?.visibility = View.VISIBLE
        } else {
            back_all?.visibility = View.VISIBLE
            back_bio_error?.visibility = View.INVISIBLE
        }
    }
}