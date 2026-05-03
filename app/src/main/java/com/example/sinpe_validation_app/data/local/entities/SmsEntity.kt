package com.example.sinpe_validation_app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "sms_detectados",
    indices = [androidx.room.Index(value = ["remitente", "contenido", "timestamp"], unique = true)]
)
data class SmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remitente: String,
    val contenido: String,
    val timestamp: Long = System.currentTimeMillis(),
    val procesado: Boolean = false
) {
    fun getFormattedTime(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return try {
            LocalDateTime.now().format(formatter)
        } catch (e: Exception) {
            "N/A"
        }
    }
}