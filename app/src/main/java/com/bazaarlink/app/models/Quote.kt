package com.bazaarlink.app.models

import java.util.Date

enum class QuoteStatus {
    PENDING, ACCEPTED, REJECTED
}

data class Quote(
    val quoteId: String = "",
    val requestId: String = "",
    val vendorId: String = "",
    val vendorShopName: String = "",
    val vendorRating: Double = 5.0,
    val vendorLocation: GeoLocation = GeoLocation(),
    val offeredPricePKR: Double = 0.0,
    val note: String = "",
    val status: String = "PENDING",
    val createdAt: Date = Date(),
    // Production‑grade image storage – URLs of uploaded images (e.g., Firebase Storage URLs)
    val imageUrls: List<String> = emptyList()
)
