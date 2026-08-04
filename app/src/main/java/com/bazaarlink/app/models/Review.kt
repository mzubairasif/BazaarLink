package com.bazaarlink.app.models

import java.util.Date

data class Review(
    val reviewId: String = "",
    val vendorId: String = "",
    val buyerId: String = "",
    val buyerDisplayName: String = "",
    val requestId: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val createdAt: Date = Date()
)
