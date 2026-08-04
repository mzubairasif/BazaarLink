package com.bazaarlink.app.ui.buyer

import android.text.format.DateUtils
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bazaarlink.app.R
import com.bazaarlink.app.models.Chat
import com.bazaarlink.app.models.Quote
import com.bazaarlink.app.models.Request
import com.bazaarlink.app.models.RequestStatus
import com.bazaarlink.app.ui.common.EReceiptDialog
import com.bazaarlink.app.viewmodels.BuyerViewModel
import com.bazaarlink.app.viewmodels.ChatViewModel

import com.bazaarlink.app.models.Review
import com.bazaarlink.app.ui.common.ReviewDialog

data class ShopReview(
    val reviewerName: String,
    val rating: Double,
    val comment: String,
    val dateAgo: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BuyerRequestDetailsScreen(
    viewModel: BuyerViewModel,
    chatViewModel: ChatViewModel,
    requestId: String,
    buyerId: String,
    buyerDisplayName: String,
    onBack: () -> Unit,
    onOpenChat: (chatId: String) -> Unit
) {
    val activeRequest by viewModel.activeRequest.collectAsState()
    val quotes by viewModel.quotes.collectAsState()
    val vendorReviews by viewModel.vendorReviews.collectAsState()
    val hasReviewed by viewModel.hasReviewed.collectAsState()

    var selectedQuoteForAccept by remember { mutableStateOf<Quote?>(null) }
    var showEReceiptDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        viewModel.loadRequestDetails(requestId)
        viewModel.checkHasReviewed(requestId, buyerId)
    }

    val req = activeRequest
    val isAccepted = req?.status == RequestStatus.ACCEPTED.name
    val acceptedQuote = if (isAccepted) {
        quotes.find { it.quoteId == req?.acceptedQuoteId } ?: quotes.firstOrNull { it.status == "ACCEPTED" }
    } else null

    val targetVendorId = req?.acceptedVendorId ?: acceptedQuote?.vendorId ?: ""

    LaunchedEffect(targetVendorId) {
        if (targetVendorId.isNotBlank()) {
            viewModel.loadVendorReviews(targetVendorId)
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = req?.rawQuery?.ifBlank { req.category.replaceFirstChar { it.uppercase() } } ?: "Request Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (req?.aiTags?.isNotEmpty() == true) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                req.aiTags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = if (req?.status == RequestStatus.ACCEPTED.name) "✅ Deal Closed" else "📡 Live Vendor Offers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
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
            if (req == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Loading request details...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val isAccepted = req.status == RequestStatus.ACCEPTED.name
                val acceptedQuote = if (isAccepted) {
                    quotes.find { it.quoteId == req.acceptedQuoteId } ?: quotes.firstOrNull { it.status == "ACCEPTED" }
                } else null

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // ── Reference Photos Strip (if present) ────
                    if (req.imageUrls.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                req.imageUrls.forEach { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }


                    // ── Accepted Deal View vs Live Bids View ──────────
                    if (isAccepted) {
                        item {
                            AcceptedDealCard(
                                request = req,
                                acceptedQuote = acceptedQuote,
                                buyerId = buyerId,
                                reviewsCount = vendorReviews.size,
                                hasReviewed = hasReviewed,
                                onShowEReceipt = { showEReceiptDialog = true },
                                onOpenChatClicked = {
                                    chatViewModel.createChat(
                                        requestId = req.requestId,
                                        buyerId = buyerId,
                                        vendorId = targetVendorId,
                                        buyerDisplayName = buyerDisplayName,
                                        vendorDisplayName = acceptedQuote?.vendorShopName ?: "Vendor Shop"
                                    ) { chatId ->
                                        onOpenChat(chatId)
                                    }
                                },
                                onOpenReviewClicked = { showReviewDialog = true }
                            )
                        }

                        // ── Shop Customer Reviews Section ────────────
                        item {
                            MerchantReviewsSection(
                                shopName = acceptedQuote?.vendorShopName ?: "Star Mobiles Saddar",
                                rating = acceptedQuote?.vendorRating ?: 4.9,
                                reviews = vendorReviews
                            )
                        }
                    } else {

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Live Vendor Bids (${quotes.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "📡 Active Radar",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (quotes.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Waiting for Saddar merchants to send bids...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(quotes, key = { it.quoteId }) { quote ->
                                VendorOfferCard(
                                    quote = quote,
                                    onAcceptClick = { selectedQuoteForAccept = quote }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

                // ── Confirmation Dialog for Accepting Offer ────────────
                if (selectedQuoteForAccept != null) {
                    val q = selectedQuoteForAccept!!
                    AlertDialog(
                        onDismissRequest = { selectedQuoteForAccept = null },
                        title = { Text(text = "Accept Offer from ${q.vendorShopName}?") },
                        text = {
                            Column {
                                Text(text = "Offered Price: PKR ${q.offeredPricePKR.toInt()}")
                                if (q.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Vendor Note: \"${q.note}\"", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Confirming will close bids and generate your 3-digit E-Receipt claim code & direct chat.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.acceptQuote(req.requestId, q.quoteId, q.vendorId)
                                    chatViewModel.createChat(
                                        requestId = req.requestId,
                                        buyerId = buyerId,
                                        vendorId = q.vendorId,
                                        buyerDisplayName = buyerDisplayName,
                                        vendorDisplayName = q.vendorShopName
                                    ) { _ -> }
                                    selectedQuoteForAccept = null
                                }
                            ) { Text(text = "Confirm Accept") }
                        },
                        dismissButton = {
                            TextButton(onClick = { selectedQuoteForAccept = null }) { Text(text = "Cancel") }
                        }
                    )
                }

                // ── E-Receipt Dialog ──────────────────────────────────
                if (showEReceiptDialog) {
                    val q = acceptedQuote
                    val receiptChat = Chat(
                        requestId = req.requestId,
                        itemQuery = req.rawQuery.ifBlank { "Mobile Accessory Request" },
                        offeredPricePKR = q?.offeredPricePKR ?: 2500.0,
                        vendorDisplayName = q?.vendorShopName ?: "Star Mobiles Saddar",
                        buyerDisplayName = buyerDisplayName
                    )
                    EReceiptDialog(
                        chat = receiptChat,
                        onDismiss = { showEReceiptDialog = false }
                    )
                }

                // ── Review Dialog ────────────────────────────────────
                if (showReviewDialog) {
                    ReviewDialog(
                        vendorShopName = acceptedQuote?.vendorShopName ?: "Star Mobiles Saddar",
                        buyerId = buyerId,
                        vendorId = targetVendorId,
                        onDismiss = { showReviewDialog = false },
                        onSubmit = { rating, comment ->
                            viewModel.submitReview(
                                vendorId = targetVendorId,
                                buyerId = buyerId,
                                buyerDisplayName = buyerDisplayName,
                                requestId = req.requestId,
                                rating = rating,
                                comment = comment,
                                onSuccess = { showReviewDialog = false }
                            )
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun VendorOfferCard(
    quote: Quote,
    onAcceptClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = quote.vendorShopName.ifBlank { "Saddar Merchant" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (quote.vendorRating <= 0.0 || quote.vendorRating > 4.9) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "🆕 New Merchant",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", quote.vendorRating),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PKR ${quote.offeredPricePKR.toInt()}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (quote.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${quote.note}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (quote.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quote.imageUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = quote.vendorLocation.marketName.ifBlank { "Saddar Market (1.2 km)" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAcceptClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "Accept Offer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Redesigned Accepted Deal Card adhering to clean UI hierarchy */
@Composable
private fun AcceptedDealCard(
    request: Request,
    acceptedQuote: Quote?,
    buyerId: String,
    reviewsCount: Int,
    hasReviewed: Boolean,
    onShowEReceipt: () -> Unit,
    onOpenChatClicked: () -> Unit,
    onOpenReviewClicked: () -> Unit
) {
    val shopName = acceptedQuote?.vendorShopName ?: "Star Mobiles Saddar"
    val vendorId = acceptedQuote?.vendorId ?: ""
    val pricePKR = acceptedQuote?.offeredPricePKR ?: 2500.0
    val rating = acceptedQuote?.vendorRating ?: 4.9
    val canReview = buyerId.isNotBlank() && buyerId != vendorId
    val isNewMerchant = reviewsCount < 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── 1. Hero Confirmation Banner ───────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DEAL ACCEPTED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Claim code & chat activated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Agreed Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "PKR ${pricePKR.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. Merchant Profile Header ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = shopName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Merchant",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Verified Saddar Merchant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(14.dp))

            // ── 3. Structured Store Logistics Details ────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Shop Address & Zone",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Star City Mall, Ground Floor, Shop #42, Saddar, Karachi",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Market Distance & Est. Travel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "1.2 km away • 4 mins drive from Star City Saddar",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Business Hours",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "11:00 AM - 9:30 PM (Mon - Sat)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 4. Action Buttons ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onShowEReceipt,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "E-Receipt Code", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onOpenChatClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Chat Vendor", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Rate Merchant Button (if allowed and not already reviewed)
            if (canReview && !hasReviewed) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenReviewClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "⭐ Rate & Review Merchant", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Section displaying real customer reviews for the merchant shop */
@Composable
private fun MerchantReviewsSection(
    shopName: String,
    rating: Double,
    reviews: List<Review>
) {
    val showScore = reviews.size > 3

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Verified Shop Reviews (${reviews.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showScore) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.1f", rating)} / 5.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "🆕 New Merchant",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (reviews.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                reviews.forEach { rev ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = rev.buyerDisplayName.ifBlank { "B" }.take(1),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rev.buyerDisplayName.ifBlank { "Saddar Buyer" },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(rev.rating.toInt().coerceIn(1, 5)) {
                                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Verified Review",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (rev.comment.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = rev.comment,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



