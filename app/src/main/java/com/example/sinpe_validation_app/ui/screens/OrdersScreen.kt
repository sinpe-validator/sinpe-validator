package com.example.sinpe_validation_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sinpe_validation_app.data.remote.CreateOrderRequestDto
import com.example.sinpe_validation_app.data.remote.OrderDto
import com.example.sinpe_validation_app.data.remote.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun OrdersScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    var orders by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("8000.00") }

    fun loadOrders() {
        scope.launch {
            isLoading = true
            message = null

            try {
                val response = RetrofitClient.instance.getOrders()

                if (response.isSuccessful) {
                    orders = response.body().orEmpty()
                } else {
                    message = "Error al cargar órdenes: ${response.code()}"
                }
            } catch (e: Exception) {
                message = "Fallo de red: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun createOrder() {
        scope.launch {
            val amount = amountText.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                message = "Digite un monto válido."
                return@launch
            }

            isLoading = true
            message = null

            try {
                val request = CreateOrderRequestDto(
                    amount = amount,
                    description = "Orden creada desde la app"
                )

                val response = RetrofitClient.instance.createOrder(request)

                if (response.isSuccessful) {
                    val order = response.body()
                    message = "Orden creada. Código SINPE: ${order?.orderCode ?: "N/A"}"
                    loadOrders()
                } else {
                    message = "Error al crear orden: ${response.code()}"
                }
            } catch (e: Exception) {
                message = "Fallo de red: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadOrders()
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { createOrder() },
                containerColor = Color(0xFFFFD1DC),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear orden"
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OrdersHeader(count = orders.size)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto de la orden") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { createOrder() }) {
                        Text("Crear orden")
                    }

                    Button(onClick = { loadOrders() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null
                        )
                        Text("Actualizar")
                    }
                }

                if (message != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orders) { order ->
                        OrderCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Órdenes SINPE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "$count órdenes registradas",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun OrderCard(order: OrderDto) {
    val statusColor = when (order.status.lowercase()) {
        "paid" -> Color(0xFF4CAF50)
        "approved" -> Color(0xFF4CAF50)
        "pending" -> Color(0xFFFFC107)
        "underreview" -> Color(0xFFFF9800)
        "expired" -> Color(0xFFF44336)
        "rejected" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.outline
    }

    val statusText = when (order.status.lowercase()) {
        "paid" -> "PAGADA"
        "approved" -> "APROBADA"
        "pending" -> "PENDIENTE"
        "underreview" -> "EN REVISIÓN"
        "expired" -> "VENCIDA"
        "rejected" -> "RECHAZADA"
        else -> order.status.uppercase()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Código: ${order.orderCode}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Monto: ₡${"%.2f".format(order.amount)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = statusColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Creada: ${order.createdAt}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Text(
                text = "Expira: ${order.expiresAt}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            if (!order.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = order.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}