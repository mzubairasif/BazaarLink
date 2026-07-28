package com.bazaarlink.app.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarlink.app.di.ServiceLocator
import com.bazaarlink.app.models.GeoLocation
import com.bazaarlink.app.models.Quote
import com.bazaarlink.app.models.Request
import com.bazaarlink.app.repository.BazaarLinkRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class VendorUiState {
    object Idle : VendorUiState()
    object SubmittingQuote : VendorUiState()
    data class QuoteSubmitted(val quoteId: String) : VendorUiState()
    data class Error(val message: String) : VendorUiState()
}

class VendorViewModel(
    private val repository: BazaarLinkRepository = ServiceLocator.repository,
    private val firestore: FirebaseFirestore = ServiceLocator.firebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<VendorUiState>(VendorUiState.Idle)
    val uiState: StateFlow<VendorUiState> = _uiState.asStateFlow()

    private val _incomingRequests = MutableStateFlow<List<Request>>(emptyList())
    val incomingRequests: StateFlow<List<Request>> = _incomingRequests.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    fun listenToIncomingRequests(category: String = "mobile parts") {
        // Remove any existing listener first
        listenerRegistration?.remove()
        listenerRegistration = null

        Log.d("BazaarLink", "VendorVM: Setting up DIRECT Firestore listener for status=BROADCASTING")

        listenerRegistration = firestore.collection("requests")
            .whereEqualTo("status", "BROADCASTING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BazaarLink", "VendorVM: Snapshot listener ERROR: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w("BazaarLink", "VendorVM: Snapshot is null")
                    return@addSnapshotListener
                }

                val fromCache = snapshot.metadata.isFromCache
                Log.d("BazaarLink", "VendorVM: Snapshot received! ${snapshot.documents.size} docs (fromCache=$fromCache)")

                val requests = mutableListOf<Request>()
                for (doc in snapshot.documents) {
                    try {
                        val request = doc.toObject(Request::class.java)?.copy(requestId = doc.id)
                        if (request != null) {
                            Log.d("BazaarLink", "VendorVM: Parsed request id=${request.requestId} query='${request.rawQuery}' status=${request.status} images=${request.imageUrls.size}")
                            requests.add(request)
                        }
                    } catch (e: Exception) {
                        Log.e("BazaarLink", "VendorVM: Failed to parse doc ${doc.id}: ${e.message}", e)
                    }
                }

                requests.sortByDescending { it.createdAt }
                _incomingRequests.value = requests
                Log.d("BazaarLink", "VendorVM: Emitted ${requests.size} requests to UI")
            }
    }

    fun submitQuote(
        requestId: String,
        vendorId: String,
        vendorShopName: String,
        offeredPricePKR: Double,
        note: String,
        localImageUris: List<String> = emptyList(),
        vendorRating: Double = 5.0,
        vendorLocation: GeoLocation = GeoLocation(marketName = "Star City Mall, Saddar")
    ) {
        _uiState.value = VendorUiState.SubmittingQuote
        val quoteId = UUID.randomUUID().toString()
        viewModelScope.launch {
            // Upload each vendor photo to Firebase Storage first
            val uploadedUrls = localImageUris.mapNotNull { uriStr ->
                val uri = Uri.parse(uriStr)
                repository.uploadImage(uri, "quote_images/$quoteId/${UUID.randomUUID()}").getOrNull()
            }
            val newQuote = Quote(
                quoteId = quoteId,
                requestId = requestId,
                vendorId = vendorId,
                vendorShopName = vendorShopName,
                vendorRating = vendorRating,
                vendorLocation = vendorLocation,
                offeredPricePKR = offeredPricePKR,
                note = note,
                imageUrls = uploadedUrls
            )
            repository.submitQuote(newQuote)
                .onSuccess { _uiState.value = VendorUiState.QuoteSubmitted(quoteId) }
                .onFailure { _uiState.value = VendorUiState.Error(it.message ?: "Failed to submit quote") }
        }
    }

    fun resetUiState() {
        _uiState.value = VendorUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        Log.d("BazaarLink", "VendorVM: Listener removed on ViewModel cleared")
    }
}
