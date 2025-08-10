package com.nowilock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Cipher

class log_regi: Service() {

    @RequiresApi(Build.VERSION_CODES.O)
    private fun noti(): Notification {

        val channel = NotificationChannel("noti_lock", "channel_oti", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, "noti_lock").apply {
            setContentTitle("NowiLock")
            setContentText("Log logging is active")
            setSmallIcon(R.drawable.padlock)
        }
            .build()
    }

     @RequiresApi(Build.VERSION_CODES.O)
     override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(1, noti())

        val broadcast = object: BroadcastReceiver() {
            override fun onReceive(con: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    val mk = MasterKey.Builder(applicationContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    val pref = EncryptedSharedPreferences.create(applicationContext, "ap", mk,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

                    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

                    val db = db(applicationContext)

                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                    c.init(Cipher.ENCRYPT_MODE, ks.getKey(pref.getString("key", null), null))

                    db.insert(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(
                        LocalDateTime.now().toString().split("T").joinToString("  ").toByteArray())), "Put a note",
                        Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                }
            }

        }

        registerReceiver(broadcast, IntentFilter(Intent.ACTION_USER_PRESENT))

        return START_STICKY

    }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


}