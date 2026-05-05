package com.example.sinpe_validation_app.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.sinpe_validation_app.BuildConfig
import com.example.sinpe_validation_app.data.remote.SinpeApiService

//Listener que detecta los mensajes, y los procesa si son de los números que nos interesan.
class SinpeSmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SinpeSmsReceiver"

        val BANK_NUMBERS: Set<String> by lazy {
            BuildConfig.SINPE_BANK_NUMBERS
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
                .also { Log.d(TAG, "Números de bancos permitidos cargados: $it") }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "Se recibió el intent pero no había mensajes")
            return
        }

        for (sms in messages) {
            val sender = sms.originatingAddress?.trim() ?: continue
            val body   = sms.messageBody?.trim()        ?: continue
            val timestamp = sms.timestampMillis

            println("DEBUG: [SinpeSmsReceiver] SMS recibido de: $sender")
            Log.d(TAG, "SMS recibido de: $sender en el timestamp: $timestamp")

            if (!isFromOfficialBank(sender)) {
                println("DEBUG: [SinpeSmsReceiver] Remitente '$sender' ignorado")
                Log.d(TAG, "Remitente '$sender' ignorado — no es número oficial de banco")
                continue
            }

            println("DEBUG: [SinpeSmsReceiver] SMS SINPE detectado! Enviando a procesamiento...")
            Log.i(TAG, "SMS SINPE detectado de '$sender' — enviando al backend")
            forwardToBackend(context, sender, body, timestamp)
        }
    }


    private fun isFromOfficialBank(sender: String): Boolean {
        val normalized = sender.replace(Regex("[+\\s-]"), "")
        return BANK_NUMBERS.any { bankNumber ->
            normalized.endsWith(bankNumber) || normalized == bankNumber
        }
    }

    //Envío al backend
    private fun forwardToBackend(context: Context, sender: String, body: String, timestamp: Long) {
        val serviceIntent = Intent(context, SinpeApiService::class.java).apply {
            putExtra(SinpeApiService.EXTRA_SENDER, sender)
            putExtra(SinpeApiService.EXTRA_BODY,   body)
            putExtra(SinpeApiService.EXTRA_TIMESTAMP, timestamp)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}