package com.nowilock.recy

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.nowilock.R
import com.nowilock.db
import com.nowilock.db.Companion.logs_list
import com.nowilock.logs
import com.nowilock.update

class holder_logs(val context: FragmentActivity, view: View): RecyclerView.ViewHolder(view)  {

    val log = view.findViewById<TextView>(R.id.log)
    val note = view.findViewById<TextView>(R.id.note)
    val edit = view.findViewById<AppCompatButton>(R.id.edit)
    val delete = view.findViewById<AppCompatButton>(R.id.delete)

    fun element (logs_data: logs) {
        log.text = logs_data.time
        note.text = logs_data.note

        val db = db(context)
        edit.setOnClickListener {
            val edit_dialog = Dialog(context)
            val edit_view = LayoutInflater.from(context).inflate(R.layout.edit_note, null)

            val input_note = edit_view.findViewById<EditText>(R.id.input_note)
            val edit_confirmation = edit_view.findViewById<AppCompatButton>(R.id.edit)

            input_note.setText(logs_data.note)

            edit_confirmation.setOnClickListener {
                if (input_note.text.isNotEmpty()) {
                    logs_data.note = input_note.text.toString()
                    db.update(logs_data.iv, input_note.text.toString())
                    edit_dialog.dismiss()
                    update = true
                }else {
                    Toast.makeText(context, "You haven't specified anything", Toast.LENGTH_SHORT).show()
                }
            }
            edit_dialog.setContentView(edit_view)
            edit_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            edit_dialog.show()
        }

        delete.setOnClickListener {
            if (BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {

                val promt = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authentication is required")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setConfirmationRequired(true)
                    .build()

                BiometricPrompt(context, ContextCompat.getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        db.delete(logs_data.iv)
                        logs_list.removeIf { it.iv == logs_data.iv }
                        update = true
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(context, "You need to try again", Toast.LENGTH_SHORT).show()
                    }
                }).authenticate(promt)

            }


        }
    }
}