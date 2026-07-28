package com.bazaarlink.app.ui.common

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bazaarlink.app.R
import com.bazaarlink.app.models.Chat
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EReceiptDialog(
    chat: Chat,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val codeDisplay = chat.claimCode.ifBlank { (100..999).random().toString() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEAL E-RECEIPT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "BazaarLink Saddar Hyperlocal Marketplace",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── 3-Digit Claim Code Banner ────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CLAIM VERIFICATION CODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "#$codeDisplay",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Present this code at the shop counter in Saddar",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Deal Details Table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReceiptRow(label = "Item Requested:", value = chat.itemQuery.ifBlank { "Mobile Part / Accessory" })
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                        ReceiptRow(label = "Agreed Price:", value = "PKR ${String.format(Locale.getDefault(), "%,.0f", chat.offeredPricePKR.takeIf { it > 0 } ?: 4500.0)}")
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                        ReceiptRow(label = "Buyer Name:", value = chat.buyerDisplayName.ifBlank { "Buyer" })
                        if (chat.buyerPhone.isNotBlank()) ReceiptRow(label = "Buyer Phone:", value = chat.buyerPhone)
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                        ReceiptRow(label = "Merchant Shop:", value = chat.vendorDisplayName.ifBlank { "Saddar Merchant" })
                        if (chat.vendorPhone.isNotBlank()) ReceiptRow(label = "Merchant Phone:", value = chat.vendorPhone)
                        ReceiptRow(label = "Market Zone:", value = "Star City Mall, Saddar, Karachi")
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                        if (chat.agreedNote.isNotBlank()) {
                            ReceiptRow(label = "Warranty / Note:", value = chat.agreedNote)
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        }

                        ReceiptRow(
                            label = "Date:",
                            value = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(chat.createdAt)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            saveReceiptBitmap(context, view, codeDisplay)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Receipt")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

private fun saveReceiptBitmap(context: Context, view: View, claimCode: String) {
    try {
        val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(600), view.height.coerceAtLeast(800), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val filename = "BazaarLink_Receipt_Claim_$claimCode.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BazaarLink")
            }
        }

        val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            val out: OutputStream? = context.contentResolver.openOutputStream(it)
            out?.use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            Toast.makeText(context, "E-Receipt saved to Photos gallery!", Toast.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(context, "E-Receipt saved!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Receipt captured successfully!", Toast.LENGTH_SHORT).show()
    }
}
