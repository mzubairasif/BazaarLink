package com.bazaarlink.app.models

import java.util.Date

data class Chat(
    val chatId: String = "",          // = requestId for simplicity
    val requestId: String = "",
    val buyerId: String = "",
    val vendorId: String = "",
    val buyerDisplayName: String = "",
    val vendorDisplayName: String = "",
    // Custom nicknames each party sets for the other
    val buyerNicknameForVendor: String = "",  // What buyer calls the vendor
    val vendorNicknameForBuyer: String = "",  // What vendor calls the buyer
    val lastMessage: String = "",
    val lastMessageAt: Date = Date(),
    val createdAt: Date = Date()
)
