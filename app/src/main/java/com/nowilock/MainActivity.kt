package com.nowilock

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.nowilock.db_space.db
import com.nowilock.opti_funs.cip_ins
import com.nowilock.opti_funs.create_biometric
import com.nowilock.opti_funs.create_dialog
import com.nowilock.opti_funs.load
import com.nowilock.recy.adapter_logs
import com.nowilock.recy.logs_data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Base64
import javax.crypto.Cipher

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: adapter_logs
    private lateinit var pref: SharedPreferences
    private var logs_list = listOf<logs_data>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        pref = EncryptedSharedPreferences.create(this, "ap",
            MasterKey.Builder(this).apply {
                setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            }.build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )


        val search = findViewById<SearchView>(R.id.search)

        val recy = findViewById<RecyclerView>(R.id.recy)

        val information = findViewById<TextView>(R.id.info)

        val service_status = findViewById<ConstraintLayout>(R.id.service_status)
        val s_s_icon = findViewById<ShapeableImageView>(R.id.s_s_icon)

        val db = db(this)

        if (pref.getBoolean("service", false)) {
            s_s_icon.setImageResource(R.drawable.regi)
        }

        adapter = adapter_logs(logs_list, { logs_data ->

            val (dialog_pre, view_pre) = create_dialog(this, R.layout.edit_note)

            val input_note = view_pre.findViewById<EditText>(R.id.input_note)
            val edit_note = view_pre.findViewById<ShapeableImageView>(R.id.edit)


            create_biometric(this@MainActivity, {
                val load = load(this, R.raw.desen_logs, "Decrypting your note")

                lifecycleScope.launch (Dispatchers.IO) {

                    var note = "note".toByteArray()
                    if (logs_data.en == 1) {
                        val json_note = JSONObject(logs_data.note_global)

                        val c = cip_ins(pref, Cipher.DECRYPT_MODE, json_note.getString("iv_note"))
                        note = c.doFinal(Base64.getDecoder().decode(json_note.getString("data_note")))
                    }

                    withContext(Dispatchers.Main) {
                        input_note.setText(String(note))
                        load.dismiss()
                    }

                    note.fill(0)

                }
            }, {
                dialog_pre.dismiss()
            })

            edit_note.setOnClickListener {
                create_biometric(this, {
                    val load = load(this, R.raw.desen_logs, "Encrypting your note")

                    lifecycleScope.launch (Dispatchers.IO) {

                        val c = cip_ins(pref, Cipher.ENCRYPT_MODE)
                        val json = JSONObject().apply {
                            put("data_note", Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(input_note.text.toString().toByteArray())))
                            put("iv_note", Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                        }.toString()

                        db.update(logs_data.id.toString(), json)
                        logs_list = logs_list.map { if ( it.id == logs_data.id ) { it.copy(note_global = json, en = 1) } else { it } }

                        withContext(Dispatchers.Main) {
                            load.dismiss()
                            dialog_pre.dismiss()
                            adapter.update_list(logs_list)
                        }

                    }
                }, {})
            }

        }, { logs_data ->

            MaterialAlertDialogBuilder(this).apply {
                setMessage("Do you want to permanently delete these logs?")
                setPositiveButton("Delete") {_, _ ->
                    create_biometric(this@MainActivity, {

                        lifecycleScope.launch (Dispatchers.IO) {
                            db.delete(pref, logs_data.id.toString())
                            logs_list = logs_list.minus(logs_data)

                            withContext(Dispatchers.Main) {
                                if (logs_list.isEmpty()) {
                                    pref.edit().putBoolean("db_full", false).commit()
                                    information.visibility = View.VISIBLE
                                    search.visibility = View.GONE
                                }

                                adapter.update_list(logs_list)
                            }
                        }

                    }, {})
                }
                setNegativeButton("No") {_, _ -> }
            }.show()

        })
        recy.adapter = adapter
        recy.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        if (pref.getBoolean("db_full", false)) {

            val load_data = load(this, R.raw.desen_logs, "Decrypting the values")

            lifecycleScope.launch (Dispatchers.IO){

                for ((id, json_meta_d, json_secure_d, en) in db.select()) {

                    val json = JSONObject(json_meta_d)
                    val c = cip_ins(pref, Cipher.DECRYPT_MODE, json.getString("meta_iv"))

                    logs_list = logs_list.plus(logs_data(id, String(c.doFinal(Base64.getDecoder().decode(json.getString("meta_data")))), json_secure_d, en))

                }

                withContext(Dispatchers.Main) {
                    load_data.dismiss()
                    information.visibility = View.GONE
                    adapter.update_list(logs_list)
                }
            }

        } else {
            search.visibility = View.GONE
        }

        search.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String?): Boolean {

                val new_list = logs_list.filter { Regex(".*$query.*").matches(it.time) }
                adapter.update_list(new_list)

                return true
            }

            override fun onQueryTextSubmit(p0: String?): Boolean = false

        })

        service_status.setOnClickListener {
            create_biometric(this, {
                if (pref.getBoolean("service", false)) {
                    s_s_icon.setImageResource(R.drawable.no_regi)

                    stopService(Intent(this, log_regi::class.java))
                    Toast.makeText(this, "The log has stopped", Toast.LENGTH_SHORT).show()
                } else {
                    s_s_icon.setImageResource(R.drawable.regi)

                    startForegroundService(Intent(this, log_regi::class.java))
                    Toast.makeText(this, "The log has started", Toast.LENGTH_SHORT).show()
                }

                pref.edit().putBoolean("service", !pref.getBoolean("service", false)).commit()


            }, {})
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

    override fun onPause() {
        super.onPause()
        finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        if (!pref.getBoolean("service", false)) {
            pref.edit().remove(name_p_hash).commit()
        }
    }
}