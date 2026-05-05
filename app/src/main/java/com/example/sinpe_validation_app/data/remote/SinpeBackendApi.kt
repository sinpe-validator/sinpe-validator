package com.example.sinpe_validation_app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
interface SinpeBackendApi {
    @POST("api/payment")
    suspend fun sendPayment(@Body payment: ReceivedSmsDto): Response<ReceivedSmsDto>

    @POST("api/sms")
    suspend fun sendSms(@Body smsRequest: SmsRequestDto): Response<Void>
}
