package com.bazaarlink.app.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarlink.app.di.ServiceLocator
import com.bazaarlink.app.models.GeoLocation
import com.bazaarlink.app.models.Quote
import com.bazaarlink.app.models.Request
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

    // Suggested tags fetched from Firestore
    private val _suggestedTags = MutableStateFlow<List<TagModel>>(emptyList())
    val suggestedTags: StateFlow<List<TagModel>> = _suggestedTags.asStateFlow()

    // Labels of tags the buyer has selected for this request
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    private var quotesJob: Job? = null

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

    private fun listenForQuotes(requestId: String) {
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

    fun resetState() {
        quotesJob?.cancel()
        _quotes.value = emptyList()
        _selectedTags.value = emptySet()
        _uiState.value = BuyerUiState.Idle
    }
}
