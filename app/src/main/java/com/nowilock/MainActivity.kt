package com.nowilock

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
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
        information.visibility = View.INVISIBLE
        val db = db(this)

        adapter = adapter_logs(this, logs_list)
        recy.adapter = adapter
        recy.layoutManager = LinearLayoutManager(this)

        val request = OneTimeWorkRequestBuilder<worker>().build()
        WorkManager.getInstance(this).enqueue(request)

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
                Log.e("lista", logs_list.toString())
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