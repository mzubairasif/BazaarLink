package com.bazaarlink.app.ui.vendor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bazaarlink.app.R
import com.bazaarlink.app.models.Request
import com.bazaarlink.app.models.User
import com.bazaarlink.app.viewmodels.VendorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorFeedScreen(
    viewModel: VendorViewModel,
    vendorUser: User
) {
    val incomingRequests by viewModel.incomingRequests.collectAsState()

    var selectedRequest by remember { mutableStateOf<Request?>(null) }
    var showQuoteSentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.listenToIncomingRequests(category = "mobile parts")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.vendor_dashboard_title)) },
                    actions = {
                        val connects = vendorUser.vendorProfile?.connectsBalance ?: 50
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.connects_balance, connects),
                                    style = MaterialTheme.typography.labelLarge,
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
                modifier = Modifier.fillMaxSize().padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.vendor_dashboard_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (incomingRequests.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.height(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(id = R.string.no_requests_found),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(incomingRequests, key = { it.requestId }) { req ->
                                VendorRequestItemCard(
                                    request = req,
                                    onSendQuoteClicked = { selectedRequest = req }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full-screen request detail with quote form
        AnimatedVisibility(
            visible = selectedRequest != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            selectedRequest?.let { req ->
                RequestDetailSheet(
                    request = req,
                    vendorUser = vendorUser,
                    onDismiss = { selectedRequest = null },
                    onSubmitQuote = { price, note, images ->
                        viewModel.submitQuote(
                            requestId = req.requestId,
                            vendorId = vendorUser.userId,
                            vendorShopName = vendorUser.vendorProfile?.shopName ?: "My Shop",
                            offeredPricePKR = price,
                            note = note,
                            localImageUris = images
                        )
                        selectedRequest = null
                        showQuoteSentDialog = true
                    }
                )
            }
        }

        // ── Confirmation Pop-up: Bid / Quote Sent ──────────────────────────
        if (showQuoteSentDialog) {
            AlertDialog(
                onDismissRequest = { showQuoteSentDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.quote_sent_success_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.quote_sent_success_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { showQuoteSentDialog = false },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.done),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun VendorRequestItemCard(
    request: Request,
    onSendQuoteClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = getLocalizedCategory(request.category),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
                    Text(
                        text = getLocalizedMarketName(request.location.marketName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            Spacer(modifier = Modifier.height(12.dp))
            Text(text = request.rawQuery, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            // Buyer's reference images
            if (request.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                var enlargedUrl by remember { mutableStateOf<String?>(null) }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    request.imageUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { enlargedUrl = url }
                        )
                    }
                }
                enlargedUrl?.let { url ->
                    com.bazaarlink.app.ui.common.FullscreenImageDialog(imageUrl = url, onDismiss = { enlargedUrl = null })
                }
            }

            // Tags
            if (request.aiTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    request.aiTags.take(5).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSendQuoteClicked,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(id = R.string.send_quote), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
fun getLocalizedCategory(category: String): String {
    return when (category.lowercase()) {
        "mobile parts", "mobile_parts" -> stringResource(id = R.string.category_mobile_parts)
        "accessories" -> stringResource(id = R.string.category_accessories)
        "repair", "repair services" -> stringResource(id = R.string.category_repair)
        else -> category.uppercase()
    }
}

@Composable
fun getLocalizedMarketName(marketName: String): String {
    if (marketName.isBlank()) return stringResource(id = R.string.market_saddar)
    return when {
        marketName.contains("Star City", ignoreCase = true) -> stringResource(id = R.string.market_star_city)
        marketName.contains("Saddar", ignoreCase = true) -> stringResource(id = R.string.market_saddar)
        else -> marketName
    }
}

