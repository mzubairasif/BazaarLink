package com.bazaarlink.app.models

import com.google.firebase.firestore.DocumentId
import java.util.Date

enum class UserRole {
    BUYER, VENDOR
}

data class User(
    @DocumentId
    val userId: String = "",
    val email: String = "",
    val role: String = "BUYER",
    val registeredRoles: List<String> = listOf("BUYER"),
    val displayName: String = "",
    val phoneNumber: String = "",
    val cnic: String = "",
    val createdAt: Date = Date(),
    val vendorProfile: VendorProfile? = null
)

data class VendorProfile(
    val shopName: String = "",
    val marketZone: String = "",
    val categories: List<String> = emptyList(),
    val location: GeoLocation = GeoLocation(),
    val connectsBalance: Int = 50,
    val rating: Double = 5.0,
    val totalRatings: Int = 0
)

data class GeoLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val marketName: String = ""
)
