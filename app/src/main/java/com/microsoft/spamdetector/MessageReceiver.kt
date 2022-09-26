package com.microsoft.spamdetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessageReceiver : BroadcastReceiver() {

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action == null) {
            return
        }
        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle["pdus"] as Array<ByteArray>
            val list = ArrayList<SmsMessage>()
            for (i in pdus.indices) {
                val message = SmsMessage.createFromPdu(pdus[i])
                if (message != null) {
                    list.add(message)
                }
            }
            var anySmishMessage = false
            val newlist = list.map {
                val message = it.messageBody ?: ""
                val filteredMessage = SpamDetectorApplication.Instance.filterInput(
                    message.lowercase().trim(),
                    SpamDetectorApplication.Instance.dict
                )
                val results = SpamDetectorApplication.Instance.classifySequence(filteredMessage)
                val isSmishMessage =
                    results[1] >= results[0] || message.contains("congrats", ignoreCase = true)
                if (isSmishMessage) {
                    anySmishMessage = true
                }
                MessageUIModel(
                    id = it.timestampMillis.toString(),
                    message = it.messageBody ?: "",
                    sender = it.originatingAddress ?: "",
                    isSmishMessage = isSmishMessage
                )
            }
            CoroutineScope(Dispatchers.IO).launch {
                SpamDetectorApplication.Instance.dao.addMessages(newlist)
            }
            if (anySmishMessage) {
                val builder = NotificationCompat.Builder(context, "channel_id")
                    .setSmallIcon(R.drawable.ic_baseline_error_outline_24)
                    .setContentTitle("Smish message detected")
                    .setContentText("We have detected a message that contains malicious content.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                with(NotificationManagerCompat.from(context)) {
                    notify(999, builder.build())
                }
            }

        }
    }
}