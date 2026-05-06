package com.example.sinpe_validation_app.data.remote

import com.google.gson.annotations.SerializedName

data class SmsRequestDto(
    @SerializedName("senderName") val senderName: String,
    @SerializedName("smsContent") val smsContent: String,
    @SerializedName("receivedAt") val receivedAt: String
)

data class CreateOrderRequestDto(
    val amount: Double,
    val description: String?
)

data class OrderDto(
    val idOrder: Int,
    val orderCode: String,
    val amount: Double,
    val status: String,
    val createdAt: String,
    val expiresAt: String,
    val description: String?
)