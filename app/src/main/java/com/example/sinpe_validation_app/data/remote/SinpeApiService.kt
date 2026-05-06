package com.example.sinpe_validation_app.data.remote

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.sinpe_validation_app.data.local.SinpeDatabase
import com.example.sinpe_validation_app.data.local.entities.SmsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class SinpeApiService : Service() {

    companion object {
        private const val TAG = "SinpeApiService"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_BODY = "body"
        const val EXTRA_TIMESTAMP = "timestamp"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: SinpeDatabase

    override fun onCreate() {
        super.onCreate()
        database = SinpeDatabase.getDatabase(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "sms_service",
                "Procesamiento de SMS",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = androidx.core.app.NotificationCompat.Builder(this, "sms_service")
            .setContentTitle("Validando SMS")
            .setContentText("Procesando mensaje recibido...")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .build()

        startForeground(1, notification)

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val sender = intent.getStringExtra(EXTRA_SENDER) ?: return START_STICKY
        val body   = intent.getStringExtra(EXTRA_BODY)   ?: return START_STICKY
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())

        println("DEBUG: [SinpeApiService] Iniciando procesamiento de SMS de: $sender")
        Log.i(TAG, "Procesando SMS de: $sender")

        serviceScope.launch {
            try {
                println("DEBUG: [SinpeApiService] Guardando localmente...")
                saveSmsLocally(sender, body, timestamp)

                val isoDate = Instant.ofEpochMilli(timestamp)
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT)

                val smsDto = SmsRequestDto(
                    senderName = sender,
                    smsContent = body,
                    receivedAt = isoDate
                )
                sendSmsToBackend(smsDto)


                Log.i(TAG, "SMS procesado correctamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando SMS: ${e.message}", e)
            } finally {
                stopSelf(startId)
            }
        }

        return START_STICKY
    }

    private suspend fun saveSmsLocally(sender: String, body: String, timestamp: Long) {
        try {
            val smsDao = database.smsDao()
            val smsEntity = SmsEntity(
                remitente = sender,
                contenido = body,
                timestamp = timestamp,
                procesado = false
            )
            smsDao.insertSms(smsEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando en BD: ${e.message}")
        }
    }

    private suspend fun sendSmsToBackend(dto: SmsRequestDto) {
        try {
            println("DEBUG: [SinpeApiService] Enviando SMS original al backend...")
            val response = RetrofitClient.instance.sendSms(dto)
            if (response.isSuccessful) {
                println("DEBUG: [SinpeApiService] SMS original enviado con ÉXITO")
                Log.i(TAG, "Backend recibió el SMS exitosamente")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Sin error body"
                println("DEBUG: [SinpeApiService] ERROR en backend (sms): ${response.code()}")
                Log.e(TAG, "Error en backend (sms): Código ${response.code()} - $errorMsg")
            }
        } catch (e: Exception) {
            println("DEBUG: [SinpeApiService] FALLO de red (sms): ${e.message}")
            Log.e(TAG, "Fallo de red al conectar con el backend (sms): ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
