package com.bazaarlink.app.ui.common

import android.Manifest
import android.content.ContentValues

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bazaarlink.app.models.Chat
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EReceiptDialog(
    chat: Chat,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val codeDisplay = chat.claimCode.ifBlank { (100..999).random().toString() }

    val writePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveReceiptToGallery(context, chat, codeDisplay)
        } else {
            Toast.makeText(context, "Storage permission is required to save receipts to Photos gallery", Toast.LENGTH_LONG).show()
        }
    }


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
                            value = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(chat.createdAt ?: Date())
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                saveReceiptToGallery(context, chat, codeDisplay)
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    saveReceiptToGallery(context, chat, codeDisplay)
                                } else {
                                    writePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            }
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

private fun generateEReceiptBitmap(context: Context, chat: Chat, claimCode: String): Bitmap {
    val width = 1080
    val height = 1560
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Color definitions
    val primaryColor = android.graphics.Color.parseColor("#0F52BA")
    val primaryLight = android.graphics.Color.parseColor("#EBF2FF")
    val darkTextColor = android.graphics.Color.parseColor("#1C1B1F")
    val grayTextColor = android.graphics.Color.parseColor("#49454F")
    val dividerColor = android.graphics.Color.parseColor("#E0E0E0")
    val whiteColor = android.graphics.Color.WHITE

    // Background
    canvas.drawColor(whiteColor)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer Border
    paint.color = primaryColor
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 16f
    canvas.drawRect(30f, 30f, width - 30f, height - 30f, paint)

    // Header Banner
    paint.style = Paint.Style.FILL
    paint.color = primaryColor
    canvas.drawRect(30f, 30f, width - 30f, 220f, paint)

    // Header Text
    paint.color = whiteColor
    paint.textSize = 50f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("BAZAARLINK E-RECEIPT", width / 2f, 120f, paint)

    paint.textSize = 26f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Saddar Hyperlocal Reverse-Marketplace, Karachi", width / 2f, 175f, paint)

    // ── 3-Digit Claim Code Banner ──────────────────────────────────
    val bannerRect = RectF(80f, 260f, width - 80f, 480f)
    paint.color = primaryLight
    canvas.drawRoundRect(bannerRect, 30f, 30f, paint)

    paint.color = primaryColor
    paint.textSize = 30f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("CLAIM VERIFICATION CODE", width / 2f, 320f, paint)

    paint.textSize = 90f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("#$claimCode", width / 2f, 415f, paint)

    paint.textSize = 24f
    paint.color = grayTextColor
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Present this claim code at shop counter in Saddar", width / 2f, 455f, paint)

    // ── Deal Details Table Card ──────────────────────────────────────
    val tableRect = RectF(80f, 520f, width - 80f, 1340f)
    paint.color = android.graphics.Color.parseColor("#F7F9FC")
    canvas.drawRoundRect(tableRect, 24f, 24f, paint)

    paint.style = Paint.Style.STROKE
    paint.color = dividerColor
    paint.strokeWidth = 3f
    canvas.drawRoundRect(tableRect, 24f, 24f, paint)

    paint.style = Paint.Style.FILL

    val startY = 590f
    val rowHeight = 90f
    var currentY = startY

    val priceText = "PKR ${String.format(Locale.getDefault(), "%,.0f", chat.offeredPricePKR.takeIf { it > 0 } ?: 4500.0)}"
    val dateText = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(chat.createdAt ?: Date())

    val rows = listOf(
        Pair("Item Requested", chat.itemQuery.ifBlank { "Mobile Part / Accessory" }),
        Pair("Agreed Deal Price", priceText),
        Pair("Buyer Name", chat.buyerDisplayName.ifBlank { "Buyer" }),
        Pair("Buyer Phone", chat.buyerPhone.ifBlank { "N/A" }),
        Pair("Merchant Shop", chat.vendorDisplayName.ifBlank { "Saddar Merchant" }),
        Pair("Merchant Phone", chat.vendorPhone.ifBlank { "N/A" }),
        Pair("Market Location", "Star City Mall, Saddar, Karachi"),
        Pair("Date & Time", dateText)
    )

    rows.forEachIndexed { index, (label, valStr) ->
        paint.color = grayTextColor
        paint.textSize = 28f
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(label, 120f, currentY, paint)

        paint.color = if (label == "Agreed Deal Price") primaryColor else darkTextColor
        paint.textSize = 30f
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val truncatedValue = if (valStr.length > 32) valStr.take(30) + "..." else valStr
        canvas.drawText(truncatedValue, width - 120f, currentY, paint)

        if (index < rows.size - 1) {
            val lineY = currentY + 30f
            paint.color = dividerColor
            paint.strokeWidth = 2f
            canvas.drawLine(120f, lineY, width - 120f, lineY, paint)
        }
        currentY += rowHeight
    }

    // ── Verification Seal at Bottom ─────────────────────────────────
    paint.color = primaryColor
    paint.textSize = 28f
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("✔ VERIFIED SADDAR MARKETPLACE DEAL", width / 2f, 1410f, paint)

    paint.color = grayTextColor
    paint.textSize = 22f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Generated by BazaarLink Android App • ibex iSprint Blueprint 2026", width / 2f, 1460f, paint)

    return bitmap
}

private fun saveReceiptToGallery(context: Context, chat: Chat, claimCode: String) {
    try {
        val bitmap = generateEReceiptBitmap(context, chat, claimCode)
        val filename = "BazaarLink_Receipt_Claim_$claimCode.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BazaarLink")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (imageUri != null) {
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Toast.makeText(context, "E-Receipt (#$claimCode) saved to Photos gallery!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to create receipt image in gallery", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("BazaarLink", "saveReceiptToGallery error: ${e.message}", e)
        Toast.makeText(context, "Error saving E-Receipt: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
