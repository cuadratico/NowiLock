package com.nowilock

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.shapes.Shape
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.nowilock.db.Companion.logs_list
import com.nowilock.recy.adapter_logs
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.util.Base64
import java.util.jar.Manifest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class MainActivity : AppCompatActivity() {
    private lateinit var scope: Job
    private lateinit var adapter: adapter_logs
    private lateinit var mk: MasterKey
    private lateinit var pref: SharedPreferences

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES

        val promt = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate yourself")
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        mk = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        pref = EncryptedSharedPreferences.create(this, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)


        val recy = findViewById<RecyclerView>(R.id.recy)
        val information = findViewById<TextView>(R.id.info)
        val service_status = findViewById<ShapeableImageView>(R.id.service_status)
        val activate_info = findViewById<ShapeableImageView>(R.id.activate_info)
        information.visibility = View.INVISIBLE
        val db = db(this)

        adapter = adapter_logs(logs_list, { logs_data ->
            val edit_dialog = Dialog(this)
            val edit_view = LayoutInflater.from(this).inflate(R.layout.edit_note, null)

            val input_note = edit_view.findViewById<EditText>(R.id.input_note)
            val edit_confirmation = edit_view.findViewById<ShapeableImageView>(R.id.edit)

            input_note.setText(logs_data.note)

            edit_confirmation.setOnClickListener {
                if (input_note.text.isNotEmpty()) {
                    db.update(logs_data.iv, input_note.text.toString())
                    logs_list = logs_list.map { if (it.id == logs_data.id) { logs_data.copy(note = input_note.text.toString()) } else { it } }
                    adapter.update_list(logs_list)
                    edit_dialog.dismiss()
                }else {
                    Toast.makeText(this, "You haven't specified anything", Toast.LENGTH_SHORT).show()
                }
            }
            edit_dialog.setContentView(edit_view)
            edit_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            edit_dialog.show()
        }, { logs_data ->
                    BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            db.delete(logs_data.iv)
                            logs_list = logs_list.minus(logs_data)
                            if (logs_list.isEmpty()) {
                                information.visibility = View.VISIBLE
                            }
                            adapter.update_list(logs_list)
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(this@MainActivity, "You need to try again", Toast.LENGTH_SHORT).show()
                        }

                    }).authenticate(promt)
        })

        recy.adapter = adapter
        recy.layoutManager = LinearLayoutManager(this)



        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        if (pref.getBoolean("service", false)) {
            service_status.setImageResource(R.drawable.play_button)
            activate_info.setImageResource(R.drawable.circle_green)
        }

        if (db.select()) {
            scope = lifecycleScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                for (position in 0..logs_list.size - 1) {
                    val (time, note, iv) = logs_list[position]

                    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                    c.init(Cipher.DECRYPT_MODE, ks.getKey(pref.getString("key_u", ""), null), GCMParameterSpec(128, Base64.getDecoder().decode(iv)))
                    logs_list[position].time = String(c.doFinal(Base64.getDecoder().decode(time)))
                }
                withContext(Dispatchers.Main) {
                    adapter.update_list(logs_list)
                    scope.cancel()
                }
            }
            scope.start()
        }else {
            information.visibility = View.VISIBLE
        }


        service_status.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {

                setNegativeButton("Close") { _, _ -> }

                if (pref.getBoolean("service", false)) {
                    setTitle("You want to disable logging?")
                    setMessage("If you disable logging, you will no longer be able to record logs until you enable it again.")
                    setPositiveButton("Desactivate") { _, _ ->


                        BiometricPrompt(this@MainActivity, ContextCompat.getMainExecutor(this@MainActivity), object : BiometricPrompt.AuthenticationCallback() {

                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    service_status.setImageResource(R.drawable.pause_button)
                                    activate_info.setImageResource(R.drawable.circle_red)
                                    pref.edit().putBoolean("service", false).commit()
                                    stopService(Intent(applicationContext, log_regi::class.java))
                                    val manager = getSystemService(NotificationManager::class.java)
                                    manager.cancel(1)
                                }

                                override fun onAuthenticationError(
                                    errorCode: Int,
                                    errString: CharSequence
                                ) {
                                    super.onAuthenticationError(errorCode, errString)
                                    Toast.makeText(this@MainActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                                }
                            }).authenticate(promt)
                    }
                } else {
                    setTitle("You want to enable log logging")
                    setMessage("If you enable this option, NowiLock will record your phone's logs. This may result in increased battery consumption.")
                    setPositiveButton("Activate") { _, _ ->

                        BiometricPrompt(this@MainActivity, ContextCompat.getMainExecutor(this@MainActivity), object : BiometricPrompt.AuthenticationCallback() {

                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    service_status.setImageResource(R.drawable.play_button)
                                    activate_info.setImageResource(R.drawable.circle_green)
                                    pref.edit().putBoolean("service", true).commit()
                                    ContextCompat.startForegroundService(this@MainActivity, Intent(applicationContext, log_regi::class.java))
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    Toast.makeText(this@MainActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                                }
                            }).authenticate(promt)
                    }
                }



            }.show()
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
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)

        if (requestCode == 100 && grantResults[0] == -1) {
            Toast.makeText(this, "Notifications are necessary", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS))
            finishAffinity()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!pref.getBoolean("service", false)) {
            pref.edit().putString("key_u", "").commit()
        }
    }
}