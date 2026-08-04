package com.bazaarlink.app.ui.common

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ReviewDialog(
    vendorShopName: String,
    buyerId: String,
    vendorId: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Double, comment: String) -> Unit
) {
    val context = LocalContext.current
    var selectedRating by remember { mutableDoubleStateOf(0.0) }
    var commentText by remember { mutableStateOf("") }
    val isSelfRating = buyerId.isNotBlank() && buyerId == vendorId

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Rate & Review Merchant",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = vendorShopName.ifBlank { "Saddar Merchant" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── 5-Star Interactive Rating Bar (Unfilled by default) ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { starIndex ->
                        val isFilled = selectedRating > 0.0 && starIndex <= selectedRating
                        Icon(
                            imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Star $starIndex",
                            tint = if (isFilled) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    selectedRating = starIndex.toDouble()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (selectedRating.toInt()) {
                        5 -> "⭐⭐⭐⭐⭐ Outstanding Deal!"
                        4 -> "⭐⭐⭐⭐ Great Experience"
                        3 -> "⭐⭐⭐ Average Service"
                        2 -> "⭐⭐ Fair / Minor Issue"
                        1 -> "⭐ Poor Service"
                        else -> "Tap a star to give rating"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedRating > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Written Comment Field
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Share your experience with this Saddar shop...", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                if (isSelfRating) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⚠️ You cannot rate your own vendor account.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
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
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (isSelfRating) {
                                Toast.makeText(context, "You cannot rate your own vendor account.", Toast.LENGTH_SHORT).show()
                            } else if (selectedRating > 0.0) {
                                onSubmit(selectedRating, commentText)
                            }
                        },
                        enabled = !isSelfRating && selectedRating > 0.0,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Submit Review", fontWeight = FontWeight.Bold)
                    }
                }

            }
        }
    }
}
