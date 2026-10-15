package com.sms.sopnopay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors

class SmsReceiver : BroadcastReceiver() {
    companion object {
        const val PREFS = "mfs_gateway_settings"
        private val executor = Executors.newSingleThreadExecutor()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        val app = context.applicationContext
        executor.execute {
            try {
                val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                if (!prefs.getBoolean("consent", false)) return@execute
                val endpoint = prefs.getString("endpoint", "").orEmpty()
                val email = prefs.getString("email", "").orEmpty()
                val deviceKey = prefs.getString("device_key", "").orEmpty()
                if (!endpoint.startsWith("https://") || email.isBlank() || deviceKey.isBlank()) return@execute
                val allowed = prefs.getString("senders", "").orEmpty().split(",")
                    .map { it.trim().uppercase(Locale.ROOT) }.filter { it.isNotBlank() }.toSet()
                if (allowed.isEmpty()) return@execute

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val grouped = messages.groupBy { it.originatingAddress.orEmpty() }
                for ((senderRaw, parts) in grouped) {
                    val sender = senderRaw.trim()
                    if (sender.uppercase(Locale.ROOT) !in allowed) continue
                    val body = parts.joinToString("") { it.messageBody.orEmpty() }.trim()
                    if (!isLikelyPaymentSms(body)) continue
                    val timestamp = parts.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
                    val eventId = sha256("$sender|$timestamp|$body")
                    val seen = prefs.getStringSet("processed_hashes", emptySet())?.toMutableSet() ?: mutableSetOf()
                    if (eventId in seen) continue

                    val queue = Volley.newRequestQueue(app)
                    val request = object : StringRequest(
                        Request.Method.POST, endpoint,
                        { response ->
                            if (response.contains("\"status\"") && response.contains("\"success\"")) {
                                val hashes = prefs.getStringSet("processed_hashes", emptySet())?.toMutableSet() ?: mutableSetOf()
                                hashes.add(eventId)
                                while (hashes.size > 500) hashes.remove(hashes.first())
                                prefs.edit().putStringSet("processed_hashes", hashes).apply()
                            }
                        },
                        { error -> Log.w("MfsSmsGateway", "API request failed: ${error.networkResponse?.statusCode ?: "network"}") }
                    ) {
                        override fun getParams(): MutableMap<String, String> = hashMapOf(
                            "email" to email,
                            "device_key" to deviceKey,
                            "message" to body,
                            "address" to sender,
                            "tmp_id" to eventId.take(32)
                        )
                        override fun getHeaders(): MutableMap<String, String> =
                            hashMapOf("Accept" to "application/json")
                    }
                    request.retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f)
                    queue.add(request)
                }
            } catch (e: Exception) {
                Log.w("MfsSmsGateway", "SMS processing failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun isLikelyPaymentSms(text: String): Boolean {
        val s = text.lowercase(Locale.ROOT)
        val hasMoney = listOf("tk", "৳", "amount", " টাকা", "taka").any { s.contains(it) }
        val hasTransactionMarker = listOf("trxid", "trx id", "transaction id", "txn id", "transaction", "payment").any { s.contains(it) }
        val hasIncomingMarker = listOf("received", "cash in", "cash-in", "payment received", "credited", "successful").any { s.contains(it) }
        return hasMoney && hasTransactionMarker && hasIncomingMarker
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
