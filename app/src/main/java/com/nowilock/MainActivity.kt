package com.nowilock

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.shapes.Shape
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
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

var update = false
class MainActivity : AppCompatActivity() {
    private lateinit var scope: Job
    private lateinit var adapter: adapter_logs
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val alias = intent.extras?.getString("ali").orEmpty()
        val recy = findViewById<RecyclerView>(R.id.recy)
        val information = findViewById<TextView>(R.id.info)
        val icon_service = findViewById<ShapeableImageView>(R.id.icon_service)
        val service_status = findViewById<ConstraintLayout>(R.id.service_status)
        val activate_info = findViewById<ShapeableImageView>(R.id.activate_info)
        information.visibility = View.INVISIBLE
        val db = db(this)

        adapter = adapter_logs(this, logs_list)
        recy.adapter = adapter
        recy.layoutManager = LinearLayoutManager(this)


        val mk = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(this, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        if (pref.getBoolean("service", false)) {
            icon_service.setImageResource(R.drawable.play_button)
            activate_info.setImageResource(R.drawable.circle_green)
        }

        if (db.select()) {

            scope = lifecycleScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                for (position in 0..logs_list.size - 1) {
                    val (time, note, iv) = logs_list[position]

                    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                    c.init(
                        Cipher.DECRYPT_MODE,
                        ks.getKey(alias, null),
                        GCMParameterSpec(128, Base64.getDecoder().decode(iv))
                    )
                    logs_list[position].time = String(c.doFinal(Base64.getDecoder().decode(time)))
                }
                withContext(Dispatchers.Main) {
                    adapter.update(logs_list)
                }
                scope.cancel()
            }
            scope.start()
        }else {
            information.visibility = View.VISIBLE
        }

        lifecycleScope.launch (Dispatchers.IO){

            while (true) {

                if (update) {
                    update = false
                    withContext(Dispatchers.Main) {
                        adapter.update(logs_list)
                        if (logs_list.isEmpty()) {
                            information.visibility = View.VISIBLE
                        }
                    }
                }

                delay(50)
            }
        }


        service_status.setOnClickListener {
            val dialog_activate = AlertDialog.Builder(this)

            dialog_activate.setNegativeButton("Close") {_, _ ->}

            if (pref.getBoolean("service", false)) {
                dialog_activate.setTitle("You want to disable logging?")
                dialog_activate.setMessage("If you disable logging, you will no longer be able to record logs until you enable it again.")
                dialog_activate.setPositiveButton("Desactivate") {_, _ ->
                    val promt = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Authenticate yourself")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .build()

                    BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            icon_service.setImageResource(R.drawable.pause_button)
                            activate_info.setImageResource(R.drawable.circle_red)
                            pref.edit().putBoolean("service", false).commit()
                            stopService(Intent(applicationContext, log_regi::class.java))
                            val manager = getSystemService(NotificationManager::class.java)
                            manager.cancel(1)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(applicationContext, "Authentication error", Toast.LENGTH_SHORT).show()
                        }
                    }).authenticate(promt)
                }
            }else {
                dialog_activate.setTitle("You want to enable log logging")
                dialog_activate.setMessage("If you enable this option, NowiLock will record your phone's logs. This may result in increased battery consumption.")
                dialog_activate.setPositiveButton("Activate") {_, _ ->
                    val promt = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Authenticate yourself")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .build()

                    BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            icon_service.setImageResource(R.drawable.play_button)
                            activate_info.setImageResource(R.drawable.circle_green)
                            pref.edit().putBoolean("service", true).commit()
                            ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, log_regi::class.java))
                            Toast.makeText(applicationContext, "Registration activated", Toast.LENGTH_SHORT).show()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(applicationContext, "Authentication error", Toast.LENGTH_SHORT).show()
                        }
                    }).authenticate(promt)
                }
            }


            dialog_activate.show()
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