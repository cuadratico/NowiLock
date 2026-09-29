package com.nowilock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nowilock.opti_funs.create_biometric
import com.nowilock.opti_funs.create_dialog
import com.nowilock.opti_funs.entropy
import com.nowilock.opti_funs.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.KeyGenerator

class RegisterActivity : AppCompatActivity() {

    private var back_all: ConstraintLayout? = null
    private var back_bio_error: ConstraintLayout? = null

    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register_activity)


        back_all = findViewById(R.id.back_all)
        val info = findViewById<TextView>(R.id.info)
        val opor = findViewById<TextView>(R.id.opor)
        val input_pass = findViewById<EditText>(R.id.input_pass)
        val progress = findViewById<LinearProgressIndicator>(R.id.progress)
        val create = findViewById<ShapeableImageView>(R.id.bottom_create)

        back_bio_error = findViewById(R.id.back_all_bio_error)
        val info_bio_error = findViewById<ShapeableImageView>(R.id.info_bio_error)

        pref = EncryptedSharedPreferences.create(
            this,
            "ap",
            MasterKey.Builder(this).apply {
                setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            }.build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        fun block () {
            val (dialog_block, view_block) = create_dialog(this, R.layout.block)

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
                    Toast.makeText(this@RegisterActivity, "The blockade has ended", Toast.LENGTH_SHORT).show()
                    finishAffinity()
                }
            }

            dialog_block.setCancelable(false)

        }

        if (!pref.getBoolean("start", false))  {
            opor.visibility = View.GONE
            info.text = "Create your password"
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

        fun secure_data_update (salt_alias: String, hash_alias: String) {
            pref.edit().putString(salt_alias, Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16))).commit()
            pref.edit().putString(hash_alias, Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(input_pass.text.toString().toByteArray() + Base64.getDecoder().decode(pref.getString(salt_alias, "")) ))).commit()
        }

        create.setOnClickListener {
            if (input_pass.text.isEmpty()) {
                Toast.makeText(this, "You need to specify a password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            create_biometric(this, {
                val load_dialog = load(this, R.raw.login, "Processing your cryptographic key")

                lifecycleScope.launch(Dispatchers.IO) {

                    if (!pref.getBoolean("start", false)) {

                        try {
                            secure_data_update(name_p_salt, name_p_hash)

                            KeyGenerator.getInstance(
                                KeyProperties.KEY_ALGORITHM_AES,
                                "AndroidKeyStore"
                            ).apply {
                                init(
                                    KeyGenParameterSpec.Builder(
                                        pref.getString(name_p_hash, "").toString(),
                                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                                    ).apply {
                                        setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                    }.build()
                                )
                            }.generateKey()
                            pref.edit().putBoolean("start", true).commit()

                            secure_data_update(name_salt_very, name_hash_very)

                            withContext(Dispatchers.Main) {
                                startActivity(
                                    Intent(
                                        this@RegisterActivity,
                                        MainActivity::class.java
                                    )
                                )
                                finish()
                            }
                        } catch (e: GeneralSecurityException) {
                            Log.e("create_key_error", e.toString())
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Error creating the key",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    } else {

                        if (MessageDigest.isEqual(Base64.getDecoder().decode(pref.getString(name_hash_very, "")), MessageDigest.getInstance("SHA256").digest(input_pass.text.toString().toByteArray() + Base64.getDecoder().decode(pref.getString(name_salt_very, ""))))) {

                            pref.edit().putString(name_p_hash, Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA256").digest(input_pass.text.toString().toByteArray() + Base64.getDecoder().decode(pref.getString(name_p_salt, ""))))).commit()
                            secure_data_update(name_salt_very, name_hash_very)

                            withContext(Dispatchers.Main) {
                                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                                finish()
                            }

                        } else {

                            if (opor.text.length == 1) {
                                pref.edit().putBoolean("block", true).commit()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@RegisterActivity, "Too many attempts", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            } else {
                                pref.edit().putInt("opor", pref.getInt("opor", 9) - 1).commit()
                                withContext(Dispatchers.Main) {
                                    input_pass.text.clear()
                                    opor.text = "*".repeat(pref.getInt("opor", 9) - 1)
                                }
                            }

                        }

                    }

                    load_dialog.dismiss()
                }

            }, {
                Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
                input_pass.text.clear()
            })

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

    override fun onDestroy() {
        super.onDestroy()

        if (!pref.getBoolean("start", false)) {
            pref.edit().clear().commit()
        }

    }

    override fun onResume() {
        super.onResume()

        if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) {
            back_all?.visibility = View.GONE
            back_bio_error?.visibility = View.VISIBLE
        } else {
            back_all?.visibility = View.VISIBLE
            back_bio_error?.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String?>, grantResults: IntArray, deviceId: Int) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)

        if (requestCode == 100) {

            if (grantResults[0] == -1) {
                Toast.makeText(this, "Notifications are necessary", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS))

                finish()
            } else {
                getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel(
                        "noti_lock",
                        "channel_oti",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }

        }
    }

}