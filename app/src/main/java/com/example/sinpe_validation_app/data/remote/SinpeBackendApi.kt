package com.example.sinpe_validation_app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

interface SinpeBackendApi {

    @POST("api/sms")
    suspend fun sendSms(@Body smsRequest: SmsRequestDto): Response<Void>

    @POST("api/orders/")
    suspend fun createOrder(
        @Body request: CreateOrderRequestDto
    ): Response<OrderDto>

    @GET("api/orders/")
    suspend fun getOrders(): Response<List<OrderDto>>
}
