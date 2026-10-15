package com.sms.sopnopay

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var endpoint: EditText
    private lateinit var email: EditText
    private lateinit var deviceKey: EditText
    private lateinit var senders: EditText
    private lateinit var consent: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(SmsReceiver.PREFS, MODE_PRIVATE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
        }
        val scroll = ScrollView(this).apply { addView(content) }
        content.addView(TextView(this).apply {
            text = "MFS SopnoPay SMS Gateway"
            textSize = 26f
        })
        content.addView(TextView(this).apply {
            text = "Only exact allowlisted sender IDs and recognized transaction messages are sent to your PHP add.php API. Configure this for a phone/account you are authorized to operate."
            textSize = 14f
            setPadding(0, 10, 0, 16)
        })
        endpoint = field(content, "HTTPS URL to add.php", prefs.getString("endpoint", "") ?: "")
        email = field(content, "Registered account email (users.email)", prefs.getString("email", "") ?: "")
        deviceKey = field(content, "Authorized device_key (from your devices table)", prefs.getString("device_key", "") ?: "", true)
        senders = field(content, "Official sender IDs, comma-separated exact matches", prefs.getString("senders", "") ?: "")
        content.addView(TextView(this).apply {
            text = "Use only the exact sender IDs shown on your genuine bKash/Nagad transaction SMS. Do not enter phone numbers or broad wildcards."
            textSize = 13f
            setPadding(0, 8, 0, 12)
        })
        consent = CheckBox(this).apply {
            text = "I authorize this device to process matching payment SMS and send them to my configured HTTPS API."
            isChecked = prefs.getBoolean("consent", false)
        }
        content.addView(consent)
        content.addView(Button(this).apply {
            text = "Save settings"
            setOnClickListener { saveSettings() }
        })
        content.addView(Button(this).apply {
            text = "Grant SMS permission"
            setOnClickListener { requestSmsPermissions() }
        })
        content.addView(TextView(this).apply {
            text = "Note: this app sends SMS data to your server. Keep device_key private. Android battery restrictions can delay background processing."
            textSize = 13f
            setPadding(0, 16, 0, 0)
        })
        setContentView(scroll)
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestSmsPermissions()
        }
    }

    private fun field(parent: LinearLayout, label: String, initial: String, secret: Boolean = false): EditText {
        parent.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setPadding(0, 8, 0, 3)
        })
        val edit = EditText(this).apply {
            setText(initial)
            textSize = 14f
            inputType = if (secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                else InputType.TYPE_CLASS_TEXT
        }
        parent.addView(edit, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        return edit
    }

    private fun saveSettings() {
        val url = endpoint.text.toString().trim()
        val mail = email.text.toString().trim()
        val key = deviceKey.text.toString().trim()
        val senderList = senders.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (!url.startsWith("https://") || !url.endsWith("add.php")) {
            Toast.makeText(this, "Enter your HTTPS add.php endpoint.", Toast.LENGTH_LONG).show()
            return
        }
        if (mail.isBlank() || key.isBlank() || senderList.isEmpty()) {
            Toast.makeText(this, "Email, device key, and sender allowlist are required.", Toast.LENGTH_LONG).show()
            return
        }
        getSharedPreferences(SmsReceiver.PREFS, MODE_PRIVATE).edit()
            .putString("endpoint", url)
            .putString("email", mail)
            .putString("device_key", key)
            .putString("senders", senderList.joinToString(","))
            .putBoolean("consent", consent.isChecked)
            .apply()
        Toast.makeText(this, "Settings saved.", Toast.LENGTH_SHORT).show()
    }

    private fun requestSmsPermissions() {
        val needed = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        val missing = needed.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 100)
        else Toast.makeText(this, "SMS permission already granted.", Toast.LENGTH_SHORT).show()
    }
}
