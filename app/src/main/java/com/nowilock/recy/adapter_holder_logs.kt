package com.nowilock.recy

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.nowilock.R
import com.nowilock.db
import com.nowilock.db.Companion.logs_list
import com.nowilock.recy.diffui_test
import com.nowilock.logs

class adapter_logs(var list: List<logs>, val edit_lam: (logs) -> Unit, val delete_lam: (logs) -> Unit): RecyclerView.Adapter<holder_logs>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): holder_logs {
        return holder_logs(LayoutInflater.from(parent.context).inflate(R.layout.recy_logs, null))
    }

    override fun onBindViewHolder(holder: holder_logs, position: Int) {
        return holder.element(list[position], edit_lam, delete_lam)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update_list (new_list: List<logs>) {
        val result = DiffUtil.calculateDiff(diffui_test(list, new_list))
        list = new_list
        result.dispatchUpdatesTo(this)
    }
}

class holder_logs(view: View): RecyclerView.ViewHolder(view)  {

    val log = view.findViewById<TextView>(R.id.log)
    val note = view.findViewById<TextView>(R.id.note)
    val edit = view.findViewById<ShapeableImageView>(R.id.edit)
    val delete = view.findViewById<ShapeableImageView>(R.id.delete)

    fun element (logs_data: logs, edit_lam: (logs) -> Unit, delete_lam: (logs) -> Unit) {
        log.text = logs_data.time
        note.text = logs_data.note

        edit.setOnClickListener {
            edit_lam(logs_data)
        }

        delete.setOnClickListener {
            delete_lam(logs_data)
        }
    }
}