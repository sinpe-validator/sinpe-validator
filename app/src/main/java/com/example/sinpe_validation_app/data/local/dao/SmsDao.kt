package com.example.sinpe_validation_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.sinpe_validation_app.data.local.entities.SmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertSms(sms: SmsEntity)
    @Query("SELECT * FROM sms_detectados ORDER BY timestamp DESC")
    fun getAllSms(): Flow<List<SmsEntity>>

    @Query("SELECT * FROM sms_detectados ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestSms(limit: Int): Flow<List<SmsEntity>>

    @Query("SELECT * FROM sms_detectados WHERE procesado = 0 ORDER BY timestamp DESC")
    fun getUnprocessedSms(): Flow<List<SmsEntity>>

    @Query("UPDATE sms_detectados SET procesado = 1 WHERE id = :id")
    suspend fun markAsProcessed(id: Long)

    @Query("DELETE FROM sms_detectados WHERE id = :id")
    suspend fun deleteSms(id: Long)

    @Query("DELETE FROM sms_detectados")
    suspend fun deleteAllSms()

    @Query("SELECT COUNT(*) FROM sms_detectados")
    fun getSmsCount(): Flow<Int>
}