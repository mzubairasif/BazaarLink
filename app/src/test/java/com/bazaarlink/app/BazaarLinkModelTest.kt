package com.bazaarlink.app

import com.bazaarlink.app.models.GeoLocation
import com.bazaarlink.app.models.Quote
import com.bazaarlink.app.models.QuoteStatus
import com.bazaarlink.app.models.Request
import com.bazaarlink.app.models.RequestStatus
import com.bazaarlink.app.models.User
import com.bazaarlink.app.models.UserRole
import com.bazaarlink.app.models.VendorProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BazaarLinkModelTest {

    @Test
    fun userModel_defaultInitialization_isBuyer() {
        val user = User(userId = "user_123", displayName = "Zaid")
        assertEquals("user_123", user.userId)
        assertEquals("BUYER", user.role)
        assertEquals("Zaid", user.displayName)
        assertNull(user.vendorProfile)
    }

    @Test
    fun vendorProfile_defaultConnectsBalance_isFifty() {
        val profile = VendorProfile(shopName = "Star Electronics")
        assertEquals("Star Electronics", profile.shopName)
        assertEquals(50, profile.connectsBalance)
        assertEquals(5.0, profile.rating, 0.01)
        assertEquals(0, profile.totalRatings)
    }

    @Test
    fun requestModel_defaultStatus_isBroadcasting() {
        val request = Request(
            buyerId = "buyer_1",
            rawQuery = "iPhone 13 screen",
            category = "mobile parts"
        )
        assertEquals("BROADCASTING", request.status)
        assertEquals(RequestStatus.BROADCASTING.name, request.status)
        assertNull(request.acceptedQuoteId)
        assertNull(request.acceptedVendorId)
        assertTrue(request.expiresAt.time > request.createdAt.time)
    }

    @Test
    fun quoteModel_defaultStatus_isPending() {
        val quote = Quote(
            requestId = "req_100",
            vendorId = "vendor_200",
            vendorShopName = "Al-Rehman Shop",
            offeredPricePKR = 15000.0
        )
        assertEquals("PENDING", quote.status)
        assertEquals(QuoteStatus.PENDING.name, quote.status)
        assertEquals(15000.0, quote.offeredPricePKR, 0.01)
    }
}
