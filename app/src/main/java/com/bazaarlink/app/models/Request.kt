package com.bazaarlink.app.models

import java.util.Date

enum class RequestStatus {
    BROADCASTING, ACCEPTED, EXPIRED
}

data class Request(
    val requestId: String = "",
    val buyerId: String = "",
    val rawQuery: String = "",
    val voiceNoteUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val category: String = "mobile parts",
    val aiTags: List<String> = emptyList(),
    val location: GeoLocation = GeoLocation(),
    val status: String = "BROADCASTING",
    val acceptedQuoteId: String? = null,
    val acceptedVendorId: String? = null,
    val createdAt: Date = Date(),
    val expiresAt: Date = Date(System.currentTimeMillis() + 30 * 60 * 1000)
)
