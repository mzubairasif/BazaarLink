package com.bazaarlink.app.ui.vendor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bazaarlink.app.R

@Composable
fun SubmitQuoteDialog(
    onRequestDismiss: () -> Unit,
    onSubmitQuote: (price: Double, note: String, images: List<String>) -> Unit
) {
    var priceInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    // Image picker state – holds URIs of selected photos
    var selectedImages by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val minPhotosError = stringResource(id = R.string.error_min_photos)
    // Launcher for selecting multiple images (any image type)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = uris
    }

    AlertDialog(
        onDismissRequest = onRequestDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.send_quote),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text(text = stringResource(id = R.string.offered_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Button to pick photos (min 3)
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.add_photos_button))
                }

                // Thumbnails of selected images
                if (selectedImages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedImages.forEach { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(text = stringResource(id = R.string.warranty_note)) },
                    placeholder = { Text(text = stringResource(id = R.string.quote_note_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 3
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceInput.toDoubleOrNull()
                    if (price == null || price <= 0) {
                        errorMsg = "Please enter a valid price in PKR"
                        return@Button
                    }
                    onSubmitQuote(price, noteInput, selectedImages.map { it.toString() })
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = stringResource(id = R.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onRequestDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}
