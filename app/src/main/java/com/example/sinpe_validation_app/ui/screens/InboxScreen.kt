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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sinpe_validation_app.data.local.entities.SmsEntity
import com.example.sinpe_validation_app.ui.viewmodel.SmsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(viewModel: SmsViewModel = viewModel()) {
    val allSms by viewModel.allSms.collectAsState(initial = emptyList())
    val smsCount by viewModel.smsCount.collectAsState(initial = 0)
    val isSyncing by viewModel.isSyncing.collectAsState()

    val palePink = Color(0xFFFFD1DC)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.generarMensajeDePrueba() },
                containerColor = palePink,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Generar mensaje de prueba"
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
                HeaderSection(smsCount)

                Spacer(modifier = Modifier.height(16.dp))

                if (allSms.isEmpty()) {
                    EmptyState(
                        isSyncing = isSyncing,
                        onSync = { viewModel.syncExistingMessages() }
                    )
                } else {
                    SmsListSection(
                        smsList = allSms,
                        isSyncing = isSyncing,
                        onDeleteAll = { viewModel.deleteAllSms() },
                        onDeleteSms = { viewModel.deleteSms(it) },
                        onSync = { viewModel.syncExistingMessages() },
                        onMarkAsProcessed = { viewModel.markAsProcessed(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(count: Int) {
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
                text = "Bandeja de SMS SINPE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "$count mensajes detectados",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Icon(
            imageVector = Icons.Default.Mail,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun EmptyState(isSyncing: Boolean, onSync: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                Text(text = "Buscando mensajes...", color = MaterialTheme.colorScheme.primary)
            } else {
                Icon(
                    imageVector = Icons.Default.Mail,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Esperando SMS de bancos...",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Los mensajes aparecerán aquí cuando lleguen",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                Button(onClick = onSync) {
                    Text("Buscar mensajes existentes")
                }
            }
        }
    }
}

@Composable
private fun SmsListSection(
    smsList: List<SmsEntity>,
    isSyncing: Boolean,
    onDeleteAll: () -> Unit,
    onDeleteSms: (Long) -> Unit,
    onSync: () -> Unit,
    onMarkAsProcessed: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mensajes recientes",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 12.dp).height(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = onSync,
                        modifier = Modifier.height(36.dp).padding(end = 8.dp)
                    ) {
                        Text("Sincronizar", fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = onDeleteAll,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Limpiar todo", fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(smsList) { sms ->
                SmsCard(
                    sms = sms,
                    onClick = { onMarkAsProcessed(sms.id) },
                    onDelete = { onDeleteSms(sms.id) }
                )
            }
        }
    }
}

@Composable
private fun SmsCard(sms: SmsEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (sms.procesado) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "De: ${sms.remitente}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatTime(sms.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge de estado
                    if (!sms.procesado) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF4CAF50),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NUEVO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = sms.contenido,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
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

private fun formatTime(timestamp: Long): String {
    return try {
        val instant = Instant.ofEpochMilli(timestamp)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val localDateTime = zonedDateTime.toLocalDateTime()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        localDateTime.format(formatter)
    } catch (e: Exception) {
        "N/A"
    }
}
