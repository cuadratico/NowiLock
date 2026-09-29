package com.nowilock.db_space

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class query_extra (val id: Int, val json_meta_d: String, val json_secure_d: String, val en: Int)
class db (context: Context): SQLiteOpenHelper(context, "logs_info.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {

        db?.execSQL("CREATE TABLE db_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, meta_d TEXT, secure_d TEXT , en INTEGER)")
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {}


    fun insert (pref: SharedPreferences, values_global: String) {
        pref.edit().putBoolean("db_full", true).commit()

        val db = this.writableDatabase
        db.insert("db_logs",
            null,
            ContentValues().apply {
                put("meta_d", values_global)
                put("secure_d", "note")
                put("en", 0)
            }
        )

    }

    fun update (id: String, global_data: String) {
        val db = this.writableDatabase

        db.update("db_logs",
            ContentValues().apply {
                put("secure_d", global_data)
                put("en", 1)
            },
            "id = ?",
            arrayOf(id)
        )

    }

    fun delete (pref: SharedPreferences, id: String) {
        val db = writableDatabase
        db.delete("db_logs", "id = ?", arrayOf(id))
    }

    fun select (): List<query_extra> {
        val db = this.readableDatabase
        val query = db.query("db_logs", null, null, null, null, null, null, null)

        var en_list = listOf<query_extra>()
        fun add () {
            en_list = en_list.plus(query_extra(query.getInt(0), query.getString(1), query.getString(2), query.getInt(3)))
        }

        if (query.moveToFirst()) {
            add()
            while (query.moveToNext()) {
                add()
            }
        }

        return en_list
    }
}