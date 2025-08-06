package com.nowilock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.security.KeyStore
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Cipher

class worker(val context: Context, parameters: WorkerParameters): Worker(context, parameters) {
    override fun doWork(): Result {

        val broadcast = object: BroadcastReceiver() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onReceive(c: Context, intent: Intent?) {

                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    val db = db(context)

                    val mk = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    val pref = EncryptedSharedPreferences.create(context, "ap", mk,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

                    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                    c.init(Cipher.ENCRYPT_MODE, ks.getKey(pref.getString("key", ""), null))

                    db.insert(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(LocalDateTime.now().toString().split("T").joinToString(" - ").toByteArray())), "note", Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                }
            }

        }

        context.registerReceiver(broadcast, IntentFilter(Intent.ACTION_USER_PRESENT))

        return Result.success()
    }


}