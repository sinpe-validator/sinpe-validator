package com.example.sinpe_validation_app.ui.viewmodel

import android.app.Application
import android.provider.Telephony
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sinpe_validation_app.BuildConfig
import com.example.sinpe_validation_app.data.local.SinpeDatabase
import com.example.sinpe_validation_app.data.local.entities.SmsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val smsDao = SinpeDatabase.getDatabase(application).smsDao()
    private val TAG = "SmsViewModel"

    val allSms: Flow<List<SmsEntity>> = smsDao.getAllSms()
    val smsCount: Flow<Int> = smsDao.getSmsCount()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val bankNumbers: Set<String> by lazy {
        BuildConfig.SINPE_BANK_NUMBERS
            .split(",")
            .map { it.trim().replace(Regex("[+\\s-]"), "") }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun syncExistingMessages() {
        viewModelScope.launch {
            _isSyncing.value = true
            val context = getApplication<Application>().applicationContext
            withContext(Dispatchers.IO) {
                try {
                    val cursor = context.contentResolver.query(
                        Telephony.Sms.Inbox.CONTENT_URI,
                        arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE),
                        null,
                        null,
                        Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
                    )

                    cursor?.use {
                        val addressIdx = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                        val bodyIdx = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
                        val dateIdx = it.getColumnIndex(Telephony.Sms.Inbox.DATE)

                        while (it.moveToNext()) {
                            val address = it.getString(addressIdx)
                            val body = it.getString(bodyIdx)
                            val date = it.getLong(dateIdx)

                            if (isBankNumber(address)) {
                                val sms = SmsEntity(
                                    remitente = address,
                                    contenido = body,
                                    timestamp = date,
                                    procesado = false
                                )
                                smsDao.insertSms(sms)
                            }
                        }
                    }
                    Log.d(TAG, "Sincronización completada")
                } catch (e: Exception) {
                    Log.e(TAG, "Error sincronizando SMS: ${e.message}")
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }

    private fun isBankNumber(sender: String?): Boolean {
        if (sender == null) return false
        val normalized = sender.replace(Regex("[+\\s-]"), "")
        return bankNumbers.any { normalized.endsWith(it) }
    }

    fun deleteAllSms() {
        viewModelScope.launch {
            smsDao.deleteAllSms()
        }
    }

    fun markAsProcessed(id: Long) {
        viewModelScope.launch {
            smsDao.markAsProcessed(id)
        }
    }

    fun deleteSms(id: Long) {
        viewModelScope.launch {
            smsDao.deleteSms(id)
        }
    }


    //Prueba del 7003, para pruebas
    fun generarMensajeDePrueba() {
        val context = getApplication<Application>().applicationContext
        val sender = "7003"
        val body = "Ha recibido 100,000.00 Colones de BRITANY_VILLALOBOS por SINPE Movil, 43567. Referencia 966492106."
        val timestamp = System.currentTimeMillis()

        val intent = android.content.Intent(context, com.example.sinpe_validation_app.data.remote.SinpeApiService::class.java).apply {
            putExtra(com.example.sinpe_validation_app.data.remote.SinpeApiService.EXTRA_SENDER, sender)
            putExtra(com.example.sinpe_validation_app.data.remote.SinpeApiService.EXTRA_BODY, body)
            putExtra(com.example.sinpe_validation_app.data.remote.SinpeApiService.EXTRA_TIMESTAMP, timestamp)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }
}