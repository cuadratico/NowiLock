package com.nowilock

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nowilock.db_space.db
import com.nowilock.opti_funs.cip_ins
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Cipher


class log_regi: Service() {

     override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(1,
            NotificationCompat.Builder(this, "noti_lock").apply {
                setContentTitle("NowiLock")
                setContentText("Log logging is active")
                setSmallIcon(R.drawable.padlock)
            }.build()
        )

        val broadcast = object: BroadcastReceiver() {
            override fun onReceive(con: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {

                    val pref = EncryptedSharedPreferences.create(
                        applicationContext, "ap",
                        MasterKey.Builder(applicationContext).apply {
                            setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        }.build(),
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )

                    val c = cip_ins(pref, Cipher.ENCRYPT_MODE)

                    db(applicationContext).insert(
                        pref,
                        JSONObject().apply {
                            put("meta_data", Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(LocalDateTime.now().toString().split("T").joinToString("  ").toByteArray())))
                            put("meta_iv", Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                        }.toString()
                    )
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