package com.nowilock

import android.R
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class logs (var time: String, val note: String, val iv: String, val id: Int)
class db(context: Context): SQLiteOpenHelper(context, "db.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {

        db?.execSQL("CREATE TABLE db_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, time TEXT, note TEXT, iv TEXT)")
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {}


    fun insert (time: String, note: String, iv: String) {
        val db = this.writableDatabase

        db.execSQL("INSERT INTO db_logs (time, note, iv) VALUES (?, ?, ?)", arrayOf(time, note, iv))
    }

    fun update (iv: String, note: String) {
        val db = this.writableDatabase

        db.execSQL("UPDATE db_logs SET note = ? WHERE iv = ?", arrayOf(note, iv))
    }

    fun delete (iv: String) {
        val db = writableDatabase

        db.execSQL("DELETE FROM db_logs WHERE iv = ?", arrayOf(iv))
    }

    fun select (): Boolean {
        val db = this.readableDatabase
        val query = db.rawQuery("SELECT * FROM db_logs", null, null)

        fun add () {
            logs_list = logs_list.plus(logs(query.getString(1), query.getString(2), query.getString(3), query.getInt(0)))
        }

        if (query.moveToFirst()) {
            add()
            while (query.moveToNext()) {
                add()
            }
            return true
        }else {
            return false
        }
    }
    companion object {
        var logs_list = listOf<logs>()
    }
}