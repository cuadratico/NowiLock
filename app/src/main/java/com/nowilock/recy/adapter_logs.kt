package com.nowilock.recy

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.nowilock.R
import com.nowilock.logs

class adapter_logs(val context: FragmentActivity, var list: List<logs>): RecyclerView.Adapter<holder_logs>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): holder_logs {
        return holder_logs(context, LayoutInflater.from(parent.context).inflate(R.layout.recy_logs, null))
    }

    override fun onBindViewHolder(holder: holder_logs, position: Int) {
        return holder.element(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update (new_list: List<logs>) {
        this.list = new_list
        notifyDataSetChanged()
    }
}