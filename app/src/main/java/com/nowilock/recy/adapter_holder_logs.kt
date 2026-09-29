package com.nowilock.recy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.nowilock.R

data class logs_data (val id: Int, var time: String, val note_global: String, var en: Int)

class adapter_logs(var list: List<logs_data>, val view_lam: (logs_data) -> Unit, val delete_lam: (logs_data) -> Unit): RecyclerView.Adapter<holder_logs>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): holder_logs {
        return holder_logs(LayoutInflater.from(parent.context).inflate(R.layout.recy_logs, null))
    }

    override fun onBindViewHolder(holder: holder_logs, position: Int) {
        return holder.element(list[position], view_lam, delete_lam)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update_list (new_list: List<logs_data>) {
        val result = DiffUtil.calculateDiff(diffui_test(list, new_list))
        list = new_list
        result.dispatchUpdatesTo(this)
    }
}

class holder_logs(view: View): RecyclerView.ViewHolder(view)  {

    val log = view.findViewById<TextView>(R.id.log)
    val view = view.findViewById<ShapeableImageView>(R.id.view)
    val delete = view.findViewById<ShapeableImageView>(R.id.delete)

    fun element (logs_data: logs_data, view_lam: (logs_data) -> Unit, delete_lam: (logs_data) -> Unit) {
        log.text = logs_data.time

        view.setOnClickListener {
            view_lam(logs_data)
        }

        delete.setOnClickListener {
            delete_lam(logs_data)
        }
    }
}

class diffui_test(val old_list: List<logs_data>, val new_list: List<logs_data>): DiffUtil.Callback() {
    override fun getOldListSize(): Int = old_list.size

    override fun getNewListSize(): Int = new_list.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old_list[oldItemPosition].id == new_list[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old_list[oldItemPosition] == new_list[newItemPosition]
    }
}