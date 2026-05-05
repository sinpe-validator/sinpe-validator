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

//Procesa los mensajes y los guarda en la BD local, para mostrarlos en la interfaz
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
                //Guardar local
                saveSmsLocally(sender, body, timestamp)

                val isoDate = Instant.ofEpochMilli(timestamp)
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT)

                // Enviar SMS original al backend para procesamiento
                val smsDto = SmsRequestDto(
                    senderName = sender,
                    smsContent = body,
                    receivedAt = isoDate
                )
                sendSmsToBackend(smsDto)

                // Parsear y enviar como pago (opcional, manteniendo lógica anterior si es necesaria)
                val paymentDto = parseSms(body, timestamp)
                if (paymentDto != null) {
                    sendToBackend(paymentDto)
                }

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

    private fun parseSms(body: String, timestamp: Long): ReceivedSmsDto? {
        return try {
            val amountPattern = Pattern.compile("(?:recibido|received)\\s+([\\d,]+\\.\\d{2})", Pattern.CASE_INSENSITIVE)
            val senderPattern = Pattern.compile("de\\s+(.*?)\\s+por\\s+SINPE\\s+Movil", Pattern.CASE_INSENSITIVE)
            val refPattern = Pattern.compile("Referencia\\s+(\\d+)", Pattern.CASE_INSENSITIVE)
            val descPattern = Pattern.compile("Movil,\\s+(.*?)\\.\\s+Referencia", Pattern.CASE_INSENSITIVE)

            val amountMatcher = amountPattern.matcher(body)
            val senderMatcher = senderPattern.matcher(body)
            val refMatcher = refPattern.matcher(body)
            val descMatcher = descPattern.matcher(body)

            if (amountMatcher.find() && senderMatcher.find() && refMatcher.find()) {
                val amountStr = amountMatcher.group(1)?.replace(",", "") ?: "0.00"
                val amount = amountStr.toDouble()
                val senderName = senderMatcher.group(1)?.trim() ?: "Desconocido"
                val reference = refMatcher.group(1) ?: ""
                val description = if (descMatcher.find()) descMatcher.group(1)?.trim() ?: "" else ""

                val isoDate = Instant.ofEpochMilli(timestamp)
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT)

                ReceivedSmsDto(
                    senderName = senderName,
                    amount = amount,
                    sinpeReference = reference,
                    description = description,
                    receivedAt = isoDate
                )
            } else {
                Log.d(TAG, "El mensaje no cumple con el patrón SINPE esperado")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando SMS: ${e.message}")
            null
        }
    }

    private suspend fun sendToBackend(dto: ReceivedSmsDto) {
        try {
            println("DEBUG: [SinpeApiService] Enviando PAGO al backend: ${dto.sinpeReference}")
            val response = RetrofitClient.instance.sendPayment(dto)
            if (response.isSuccessful) {
                println("DEBUG: [SinpeApiService] PAGO enviado con ÉXITO")
                Log.i(TAG, "Backend recibió el pago exitosamente. Referencia: ${response.body()?.sinpeReference}")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Sin error body"
                println("DEBUG: [SinpeApiService] ERROR en backend (pago): ${response.code()}")
                Log.e(TAG, "Error en backend (payment): Código ${response.code()} - $errorMsg")
            }
        } catch (e: Exception) {
            println("DEBUG: [SinpeApiService] FALLO de red (pago): ${e.message}")
            Log.e(TAG, "Fallo de red al conectar con el backend (payment): ${e.message}")
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
