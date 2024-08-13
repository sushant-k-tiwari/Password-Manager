package com.sushant.passwordmanager

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var passwordContainer: LinearLayout
    private val sharedPreferences by lazy { getSharedPreferences("PasswordStore", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        passwordContainer = findViewById(R.id.passwordContainer)
        val btnAdd : Button = findViewById(R.id.btnAdd)
        btnAdd.setOnClickListener {
            showAddPasswordDialog()
        }
        loadPasswords()
    }

    private fun showAddPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_password, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add New Password")
            .setPositiveButton("Save") { dialog, which ->
                val platformName = dialogView.findViewById<EditText>(R.id.editTextPlatform).text.toString()
                val password = dialogView.findViewById<EditText>(R.id.editTextPassword).text.toString()

                savePassword(platformName, password)
                addPasswordToView(platformName, password)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun savePassword(platformName: String, password: String) {
        val passwords = sharedPreferences.getStringSet("passwords", mutableSetOf())?.toMutableSet()
        passwords?.add("$platformName:$password")
        sharedPreferences.edit().putStringSet("passwords", passwords).apply()
    }

    private fun loadPasswords() {
        val passwords = sharedPreferences.getStringSet("passwords", mutableSetOf()) ?: return
        passwords.forEach { entry ->
            val (platformName, password) = entry.split(":")
            addPasswordToView(platformName, password)
        }
    }

    private fun addPasswordToView(platformName: String, password: String) {
        val inflater = LayoutInflater.from(this)
        val passwordCardView = inflater.inflate(R.layout.item_password, passwordContainer, false) as CardView

        val tvPlatformName: TextView = passwordCardView.findViewById(R.id.tvPlatformName)
        val tvPassword: TextView = passwordCardView.findViewById(R.id.tvPassword)

        tvPlatformName.text = platformName
        tvPassword.text = password

        // Set a long-click listener to delete the entry
        passwordCardView.setOnLongClickListener {
            showDeleteConfirmationDialog(platformName, password, passwordCardView)
            true
        }

        // Set an OnClickListener to copy the password to clipboard
        passwordCardView.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Password", password)
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
        }


        passwordContainer.addView(passwordCardView)
    }

    private fun showDeleteConfirmationDialog(platformName: String, password: String, cardView: View) {
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete?")
            .setPositiveButton("Delete") { dialog, which ->
                deletePassword(platformName, password)
                passwordContainer.removeView(cardView)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun deletePassword(platformName: String, password: String) {
        val passwords = sharedPreferences.getStringSet("passwords", mutableSetOf())?.toMutableSet()
        passwords?.remove("$platformName:$password")
        sharedPreferences.edit().putStringSet("passwords", passwords).apply()
    }
}