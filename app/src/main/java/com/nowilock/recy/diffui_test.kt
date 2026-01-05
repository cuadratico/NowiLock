package com.nowilock.recy

import androidx.recyclerview.widget.DiffUtil
import com.nowilock.logs

class diffui_test(val old_list: List<logs>, val new_list: List<logs>): DiffUtil.Callback() {
    override fun getOldListSize(): Int = old_list.size

    override fun getNewListSize(): Int = new_list.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old_list[oldItemPosition].id == new_list[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old_list[oldItemPosition] == new_list[newItemPosition]
    }
}