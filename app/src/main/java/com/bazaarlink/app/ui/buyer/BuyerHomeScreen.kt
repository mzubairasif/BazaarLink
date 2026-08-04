package com.bazaarlink.app.ui.buyer

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import com.bazaarlink.app.R
import com.bazaarlink.app.models.GeoLocation
import com.bazaarlink.app.viewmodels.BuyerUiState
import com.bazaarlink.app.viewmodels.BuyerViewModel

import androidx.compose.material.icons.filled.ReceiptLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BuyerHomeScreen(
    viewModel: BuyerViewModel,
    buyerId: String,
    onBroadcastStarted: (requestId: String) -> Unit,
    onViewSentRequestsClicked: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val suggestedTags by viewModel.suggestedTags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    var queryText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Reference photos picked by buyer
    var referenceImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Whether the full suggested-tag pool is visible
    var showTagPool by remember { mutableStateOf(false) }

    // Search query & focus state for tag pool filter
    var tagSearchQuery by remember { mutableStateOf("") }
    var isTagSearchFocused by remember { mutableStateOf(false) }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> referenceImages = (referenceImages + uris).distinct() }

    LaunchedEffect(uiState) {
        if (uiState is BuyerUiState.WaitingForQuotes) {
            val reqId = (uiState as BuyerUiState.WaitingForQuotes).requestId
            viewModel.resetUiState()
            onBroadcastStarted(reqId)
        } else if (uiState is BuyerUiState.Error) {
            errorMessage = (uiState as BuyerUiState.Error).message
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onViewSentRequestsClicked() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "My Requests",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "My Requests",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Location Header ────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.location_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(id = R.string.default_location),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.buyer_home_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(id = R.string.buyer_home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Description Field ──────────────────────────────────────
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text(text = stringResource(id = R.string.query_placeholder), style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = {
                            Toast.makeText(context, context.getString(R.string.mic_button), Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = stringResource(id = R.string.mic_button), modifier = Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 2
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Reference Photos ───────────────────────────────────────
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.reference_photos_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedCard(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.add_reference_photo),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (referenceImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        referenceImages.forEachIndexed { index, uri ->
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                // Remove button on each thumbnail
                                IconButton(
                                    onClick = {
                                        referenceImages = referenceImages.toMutableList().also { it.removeAt(index) }
                                    },
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.remove_photo),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Tags ───────────────────────────────────────────────────
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.tags_section_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (suggestedTags.isNotEmpty()) {
                        Text(
                            text = if (showTagPool)
                                stringResource(id = R.string.tags_hide_pool)
                            else
                                stringResource(id = R.string.tags_browse),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showTagPool = !showTagPool }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selected tags as compact dismissible InputChips + a "+" to open pool
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    selectedTags.forEach { label ->
                        val tag = suggestedTags.find { it.label == label }
                        val display = if (tag != null) "${tag.emoji} $label" else label
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggleTag(label) },
                            label = { Text(text = display, style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.remove_tag),
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    // "+" chip to open the tag pool
                    if (suggestedTags.isNotEmpty()) {
                        FilterChip(
                            selected = false,
                            onClick = { showTagPool = !showTagPool },
                            label = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(id = R.string.tags_browse),
                                    modifier = Modifier.size(13.dp)
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Expandable suggested tag pool (capped to 1-2 rows + search field below)
                if (showTagPool) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            val filteredTags = remember(suggestedTags, selectedTags, tagSearchQuery) {
                                suggestedTags.filter { tag ->
                                    !selectedTags.contains(tag.label) &&
                                            (tagSearchQuery.isBlank() || tag.label.contains(tagSearchQuery, ignoreCase = true))
                                }
                            }

                            // 3 rows capped area (max ~110.dp height) with scroll support
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 110.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    filteredTags.forEach { tag ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                viewModel.toggleTag(tag.label)
                                                tagSearchQuery = ""
                                            },
                                            label = {
                                                Text(
                                                    text = "${tag.emoji} ${tag.label}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    }

                                    if (filteredTags.isEmpty() && tagSearchQuery.isBlank()) {
                                        Text(
                                            text = stringResource(id = R.string.tags_all_selected),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Smooth alpha fade for trailing search icon when user taps/types in field
                            val searchIconAlpha by animateFloatAsState(
                                targetValue = if (isTagSearchFocused || tagSearchQuery.isNotEmpty()) 0f else 0.5f,
                                animationSpec = tween(durationMillis = 200),
                                label = "tag_search_icon_alpha"
                            )

                            // Underlined single line tag search input
                            TextField(
                                value = tagSearchQuery,
                                onValueChange = { tagSearchQuery = it },
                                placeholder = {
                                    Text(
                                        text = stringResource(id = R.string.search_tags_placeholder),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                trailingIcon = {
                                    if (searchIconAlpha > 0.05f) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = searchIconAlpha),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isTagSearchFocused = it.isFocused },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (tagSearchQuery.isNotBlank()) {
                                            viewModel.toggleTag(tagSearchQuery.trim())
                                            tagSearchQuery = ""
                                        }
                                    }
                                )
                            )
                        }
                    }
                }



                Spacer(modifier = Modifier.height(24.dp))

                // ── Go / Broadcast Button ──────────────────────────────────
                Button(
                    onClick = {
                        if (queryText.isBlank()) {
                            errorMessage = context.getString(R.string.error_empty_query)
                            return@Button
                        }
                        viewModel.broadcastRequest(
                            buyerId = buyerId,
                            rawQuery = queryText,
                            category = "mobile parts",
                            location = GeoLocation(
                                latitude = 24.8607,
                                longitude = 67.0011,
                                marketName = "Star City Mall, Saddar"
                            ),
                            localImageUris = referenceImages.map { it.toString() }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = uiState !is BuyerUiState.Broadcasting
                ) {
                    if (uiState is BuyerUiState.Broadcasting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.go_broadcast),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
