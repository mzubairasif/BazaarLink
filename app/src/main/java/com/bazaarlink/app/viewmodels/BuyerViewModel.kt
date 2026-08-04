package com.bazaarlink.app.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarlink.app.di.ServiceLocator
import com.bazaarlink.app.models.GeoLocation
import com.bazaarlink.app.models.Quote
import com.bazaarlink.app.models.Request
import com.bazaarlink.app.models.Review
import com.bazaarlink.app.models.TagModel
import com.bazaarlink.app.repository.BazaarLinkRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class BuyerUiState {
    object Idle : BuyerUiState()
    object Broadcasting : BuyerUiState()
    data class WaitingForQuotes(val requestId: String) : BuyerUiState()
    data class QuoteAccepted(val quoteId: String, val vendorId: String) : BuyerUiState()
    data class Error(val message: String) : BuyerUiState()
}

class BuyerViewModel(
    private val repository: BazaarLinkRepository = ServiceLocator.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BuyerUiState>(BuyerUiState.Idle)
    val uiState: StateFlow<BuyerUiState> = _uiState.asStateFlow()

    private val _quotes = MutableStateFlow<List<Quote>>(emptyList())
    val quotes: StateFlow<List<Quote>> = _quotes.asStateFlow()

    // Buyer's sent requests from last 5 days
    private val _buyerRequests = MutableStateFlow<List<Request>>(emptyList())
    val buyerRequests: StateFlow<List<Request>> = _buyerRequests.asStateFlow()

    // Currently focused single request details
    private val _activeRequest = MutableStateFlow<Request?>(null)
    val activeRequest: StateFlow<Request?> = _activeRequest.asStateFlow()

    // Suggested tags fetched from Firestore
    private val _suggestedTags = MutableStateFlow<List<TagModel>>(emptyList())
    val suggestedTags: StateFlow<List<TagModel>> = _suggestedTags.asStateFlow()

    // Labels of tags the buyer has selected for this request
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    // Vendor reviews
    private val _vendorReviews = MutableStateFlow<List<Review>>(emptyList())

    val vendorReviews: StateFlow<List<Review>> = _vendorReviews.asStateFlow()

    private val _hasReviewed = MutableStateFlow<Boolean>(false)
    val hasReviewed: StateFlow<Boolean> = _hasReviewed.asStateFlow()

    private var quotesJob: Job? = null
    private var requestsJob: Job? = null
    private var activeRequestJob: Job? = null
    private var reviewsJob: Job? = null
    private var hasReviewedJob: Job? = null



    init {
        seedAndLoadTags()
    }

    /** Seed the `tags` collection once then start listening for all tags. */
    private fun seedAndLoadTags() {
        viewModelScope.launch {
            repository.seedTagsIfEmpty()
        }
        viewModelScope.launch {
            repository.getSuggestedTags().collect { tags ->
                _suggestedTags.value = tags
            }
        }
    }

    /** Toggle a tag label in/out of the selected set. */
    fun toggleTag(label: String) {
        _selectedTags.value = if (_selectedTags.value.contains(label)) {
            _selectedTags.value - label
        } else {
            _selectedTags.value + label
        }
    }

    fun broadcastRequest(
        buyerId: String,
        rawQuery: String,
        category: String = "mobile phones accessories",
        location: GeoLocation = GeoLocation(marketName = "Star City Mall, Saddar"),
        localImageUris: List<String> = emptyList(),
        voiceNoteUrl: String? = null
    ) {
        _uiState.value = BuyerUiState.Broadcasting
        val requestId = UUID.randomUUID().toString()
        viewModelScope.launch {
            // Upload each reference photo to Firebase Storage first
            val uploadedUrls = localImageUris.mapNotNull { uriStr ->
                val uri = Uri.parse(uriStr)
                repository.uploadImage(uri, "request_images/$requestId/${UUID.randomUUID()}").getOrNull()
            }
            val newRequest = Request(
                requestId = requestId,
                buyerId = buyerId,
                rawQuery = rawQuery,
                category = category,
                location = location,
                imageUrls = uploadedUrls,
                voiceNoteUrl = voiceNoteUrl,
                aiTags = _selectedTags.value.toList()
            )
            repository.broadcastRequest(newRequest)
                .onSuccess {
                    _uiState.value = BuyerUiState.WaitingForQuotes(requestId)
                    listenForQuotes(requestId)
                }
                .onFailure {
                    _uiState.value = BuyerUiState.Error(it.message ?: "Failed to broadcast request")
                }
        }
    }

    fun loadBuyerRequests(buyerId: String, days: Int = 5) {
        requestsJob?.cancel()
        requestsJob = viewModelScope.launch {
            repository.getBuyerRequests(buyerId, days).collect { reqList ->
                _buyerRequests.value = reqList
            }
        }
    }

    fun loadRequestDetails(requestId: String) {
        activeRequestJob?.cancel()
        activeRequestJob = viewModelScope.launch {
            repository.getRequest(requestId).collect { req ->
                _activeRequest.value = req
            }
        }
        listenForQuotes(requestId)
    }

    fun listenForQuotes(requestId: String) {
        quotesJob?.cancel()
        quotesJob = viewModelScope.launch {
            repository.getQuotesForRequest(requestId).collect { quoteList ->
                _quotes.value = quoteList
            }
        }
    }

    fun acceptQuote(requestId: String, quoteId: String, vendorId: String) {
        viewModelScope.launch {
            repository.acceptQuote(requestId, quoteId, vendorId)
                .onSuccess {
                    _uiState.value = BuyerUiState.QuoteAccepted(quoteId, vendorId)
                }
                .onFailure {
                    _uiState.value = BuyerUiState.Error(it.message ?: "Failed to accept quote")
                }
        }
    }

    fun loadVendorReviews(vendorId: String) {
        reviewsJob?.cancel()
        reviewsJob = viewModelScope.launch {
            repository.getVendorReviews(vendorId).collect { revs ->
                _vendorReviews.value = revs
            }
        }
    }

    fun checkHasReviewed(requestId: String, buyerId: String) {
        hasReviewedJob?.cancel()
        hasReviewedJob = viewModelScope.launch {
            repository.hasBuyerReviewedRequest(requestId, buyerId).collect { hasRev ->
                _hasReviewed.value = hasRev
            }
        }
    }

    fun submitReview(
        vendorId: String,
        buyerId: String,
        buyerDisplayName: String,
        requestId: String,
        rating: Double,
        comment: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (buyerId.isNotBlank() && buyerId == vendorId) {
            onError("You cannot rate your own vendor account.")
            return
        }
        val review = Review(
            vendorId = vendorId,
            buyerId = buyerId,
            buyerDisplayName = buyerDisplayName,
            requestId = requestId,
            rating = rating,
            comment = comment
        )
        viewModelScope.launch {
            repository.submitReview(review)
                .onSuccess {
                    _hasReviewed.value = true
                    loadVendorReviews(vendorId)
                    onSuccess()
                }
                .onFailure {
                    onError(it.message ?: "Failed to submit review")
                }
        }
    }

    fun resetUiState() {
        _uiState.value = BuyerUiState.Idle
    }

    fun resetState() {
        quotesJob?.cancel()
        requestsJob?.cancel()
        activeRequestJob?.cancel()
        reviewsJob?.cancel()
        hasReviewedJob?.cancel()
        _quotes.value = emptyList()
        _selectedTags.value = emptySet()
        _activeRequest.value = null
        _vendorReviews.value = emptyList()
        _hasReviewed.value = false
        _uiState.value = BuyerUiState.Idle
    }

}
