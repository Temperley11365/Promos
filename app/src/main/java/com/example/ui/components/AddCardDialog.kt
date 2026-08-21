package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Bank
import com.example.model.CardNetwork
import com.example.model.CardType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (Bank, CardType, CardNetwork, String, String) -> Unit
) {
    var selectedBank by remember { mutableStateOf(Bank.GALICIA) }
    var selectedCardType by remember { mutableStateOf(CardType.CREDIT) }
    var selectedCardNetwork by remember { mutableStateOf(CardNetwork.VISA) }
    var cardNickname by remember { mutableStateOf("") }
    var last4Digits by remember { mutableStateOf("1234") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Registrar Nueva Tarjeta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("add_card_dialog_content")
            ) {
                Text(
                    text = "Vinculá tus tarjetas para calcular automáticamente el mayor descuento en naftas y comercios locales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Visual Live Card Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(selectedBank.primaryColorHex),
                                        Color(selectedBank.primaryColorHex).copy(alpha = 0.75f),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedBank.displayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = selectedCardType.displayName,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "•••• •••• •••• ${last4Digits.ifBlank { "••••" }}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cardNickname.ifBlank { "MI TARJETA" }.uppercase(),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = selectedCardNetwork.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 1. Bank Selector
                Text(
                    text = "1. Banco o Billetera",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    val availableBanks = Bank.entries.filter { it != Bank.TODOS && it != Bank.MODO }
                    items(availableBanks) { bank ->
                        val isSelected = bank == selectedBank
                        Surface(
                            onClick = { selectedBank = bank },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(bank.primaryColorHex) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("bank_chip_${bank.id}")
                        ) {
                            Text(
                                text = bank.shortName,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // 2. Card Network
                Text(
                    text = "2. Red de la Tarjeta",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    CardNetwork.entries.forEach { network ->
                        FilterChip(
                            selected = network == selectedCardNetwork,
                            onClick = { selectedCardNetwork = network },
                            label = { Text(network.displayName) },
                            modifier = Modifier.testTag("network_chip_${network.name}")
                        )
                    }
                }

                // 3. Card Type
                Text(
                    text = "3. Tipo de Tarjeta",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    listOf(CardType.CREDIT, CardType.DEBIT, CardType.PREPAID).forEach { type ->
                        FilterChip(
                            selected = type == selectedCardType,
                            onClick = { selectedCardType = type },
                            label = { Text(type.displayName) },
                            modifier = Modifier.testTag("type_chip_${type.name}")
                        )
                    }
                }

                // 4. Details (Optional Nickname & Last 4 digits)
                OutlinedTextField(
                    value = cardNickname,
                    onValueChange = { cardNickname = it },
                    label = { Text("Nombre o alias (ej. Galicia Black)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("card_nickname_input")
                )

                OutlinedTextField(
                    value = last4Digits,
                    onValueChange = { if (it.length <= 4) last4Digits = it },
                    label = { Text("Últimos 4 dígitos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_last4_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedBank,
                        selectedCardType,
                        selectedCardNetwork,
                        cardNickname.ifBlank { "${selectedBank.shortName} ${selectedCardNetwork.displayName}" },
                        last4Digits.ifBlank { "0000" }
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("save_card_button")
            ) {
                Text("Guardar Tarjeta", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_card_button")
            ) {
                Text("Cancelar")
            }
        }
    )
}
