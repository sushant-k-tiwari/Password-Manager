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
                val platformName = dialogView.findViewById<EditText>(R.id.editTextPlatform).text.toString().trim()
                val username = dialogView.findViewById<EditText>(R.id.editTextUsername).text.toString().trim()
                val password = dialogView.findViewById<EditText>(R.id.editTextPassword).text.toString().trim()

                if (platformName.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                    savePassword(platformName, username, password)
                    addPasswordToView(platformName, username, password)
                } else {
                    Toast.makeText(this@MainActivity, "All fields are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun savePassword(platformName: String, username: String, password: String) {
        val passwords = sharedPreferences.getStringSet("passwords", mutableSetOf())?.toMutableSet()
        passwords?.add("$platformName:$username:$password")
        sharedPreferences.edit().putStringSet("passwords", passwords).apply()
    }

    private fun loadPasswords() {
        val passwords = sharedPreferences.getStringSet("passwords", mutableSetOf()) ?: return
        passwords.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 3) {
                val platformName = parts[0]
                val username = parts[1]
                val password = parts[2]
                addPasswordToView(platformName, username, password)
            }
        }
    }

    private fun addPasswordToView(platformName: String, username: String, password: String) {
        val inflater = LayoutInflater.from(this)
        val passwordCardView = inflater.inflate(R.layout.item_password, passwordContainer, false) as CardView

        val tvPlatformName: TextView = passwordCardView.findViewById(R.id.tvPlatformName)
        val tvUsername: TextView = passwordCardView.findViewById(R.id.tvUsername)
        val tvPassword: TextView = passwordCardView.findViewById(R.id.tvPassword)

        tvPlatformName.text = platformName
        tvUsername.text = username
        tvPassword.text = password

        // Initially hide the password
        tvPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Handle single tap to show/hide password and double tap to copy password
        passwordCardView.setOnClickListener(object : View.OnClickListener {
            private var lastTapTime: Long = 0

            override fun onClick(view: View?) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 300) {  // Double-tap detected
                    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Password", password)
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(this@MainActivity, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                } else {  // Single tap detected
                    if (tvPassword.inputType == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                        tvPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    } else {
                        tvPassword.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    }
                }
                lastTapTime = currentTime
            }
        })

        // Set a long-click listener to delete the entry
        passwordCardView.setOnLongClickListener {
            showDeleteConfirmationDialog(platformName, username, password, passwordCardView)
            true
        }

        passwordContainer.addView(passwordCardView)
    }

    private fun showDeleteConfirmationDialog(platformName: String, username: String, password: String, cardView: View) {
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete?")
            .setPositiveButton("Delete") { dialog, which ->
                deletePassword(platformName, username, password)
                passwordContainer.removeView(cardView)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun deletePassword(platformName: String, username: String, password: String) {
        val passwords = sharedPreferences.getStringSet("passwords", mutableSetOf())?.toMutableSet()
        passwords?.remove("$platformName:$username:$password")
        sharedPreferences.edit().putStringSet("passwords", passwords).apply()
    }
}
