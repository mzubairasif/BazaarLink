package com.bazaarlink.app.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarlink.app.di.ServiceLocator
import com.bazaarlink.app.models.User
import com.bazaarlink.app.models.VendorProfile
import com.bazaarlink.app.repository.BazaarLinkRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    /** Google sign-in succeeded but no profile exists in Firestore yet — show onboarding form */
    data class NeedsOnboarding(val uid: String, val email: String, val googleDisplayName: String) : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val auth: FirebaseAuth = ServiceLocator.firebaseAuth,
    private val repository: BazaarLinkRepository = ServiceLocator.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkExistingSession()
    }

    /** Called on app startup. If Firebase already has a logged-in user with a Firestore
     *  profile, skip auth entirely and navigate straight to their active role screen. */
    fun checkExistingSession() {
        val firebaseUser = auth.currentUser ?: return   // No session — stay on auth screen
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                var profile = repository.getUserProfile(firebaseUser.uid).getOrNull()
                if (profile == null && !firebaseUser.email.isNullOrBlank()) {
                    profile = repository.getUserProfileByEmail(firebaseUser.email!!).getOrNull()
                }

                if (profile != null) {
                    Log.d("BazaarLink", "checkExistingSession: restored session for ${profile.displayName} (${profile.role})")
                    _currentUser.value = profile
                    _uiState.value = AuthUiState.Success(profile)
                } else {
                    // Logged into Firebase but no profile yet → onboarding
                    _uiState.value = AuthUiState.NeedsOnboarding(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        googleDisplayName = firebaseUser.displayName ?: ""
                    )
                }
            } catch (e: Exception) {
                Log.e("BazaarLink", "checkExistingSession failed: ${e.message}", e)
                _uiState.value = AuthUiState.Idle
            }
        }
    }

    /** Called after Google sign-in returns an account. Checks Firestore for existing user profile by UID and Email. */
    fun onGoogleAccountSelected(uid: String, email: String, displayName: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                var profile = repository.getUserProfile(uid).getOrNull()
                if (profile == null && email.isNotBlank()) {
                    profile = repository.getUserProfileByEmail(email).getOrNull()
                }

                if (profile != null) {
                    Log.d("BazaarLink", "onGoogleAccountSelected: existing user ${profile.displayName} (${profile.role})")
                    _currentUser.value = profile
                    _uiState.value = AuthUiState.Success(profile)
                } else {
                    Log.d("BazaarLink", "onGoogleAccountSelected: new user, needs onboarding ($email)")
                    _uiState.value = AuthUiState.NeedsOnboarding(
                        uid = uid,
                        email = email,
                        googleDisplayName = displayName
                    )
                }
            } catch (e: Exception) {
                Log.e("BazaarLink", "onGoogleAccountSelected failed: ${e.message}", e)
                _uiState.value = AuthUiState.NeedsOnboarding(
                    uid = uid,
                    email = email,
                    googleDisplayName = displayName
                )
            }
        }
    }

    /** Called after Google sign-in returns an AuthCredential. */
    fun signInWithGoogleCredential(credential: AuthCredential) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                if (firebaseUser == null) {
                    _uiState.value = AuthUiState.Error("Google Sign-In returned no user")
                    return@launch
                }
                onGoogleAccountSelected(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: ""
                )
            } catch (e: Exception) {
                Log.e("BazaarLink", "signInWithGoogle failed: ${e.message}", e)
                _uiState.value = AuthUiState.Error(e.message ?: "Google Sign-In failed")
            }
        }
    }

    /** Called after the user fills in the onboarding form for the first time. */
    fun completeOnboarding(
        uid: String,
        email: String,
        role: String,
        displayName: String,
        phoneNumber: String,
        cnic: String,
        vendorProfile: VendorProfile? = null
    ) {
        val user = User(
            userId = uid,
            email = email,
            role = role,
            registeredRoles = listOf(role),
            displayName = displayName,
            phoneNumber = phoneNumber,
            cnic = cnic,
            vendorProfile = if (role == "VENDOR") (vendorProfile ?: VendorProfile(
                categories = listOf("mobile parts")
            )) else null
        )
        repository.saveUserProfileLocally(user)
        _currentUser.value = user
        _uiState.value = AuthUiState.Success(user)
        Log.d("BazaarLink", "completeOnboarding: profile saved for $displayName ($email, $role)")
        viewModelScope.launch { repository.syncUserProfileToCloud(user) }
    }

    /** Toggle active role if user is already registered for both BUYER and VENDOR roles. */
    fun switchRole(newRole: String) {
        val current = _currentUser.value ?: return
        val updatedRoles = if (current.registeredRoles.contains(newRole)) {
            current.registeredRoles
        } else {
            current.registeredRoles + newRole
        }
        val updatedUser = current.copy(
            role = newRole,
            registeredRoles = updatedRoles,
            vendorProfile = if (newRole == "VENDOR" && current.vendorProfile == null) {
                VendorProfile(shopName = "${current.displayName}'s Shop", categories = listOf("mobile parts"))
            } else current.vendorProfile
        )
        repository.saveUserProfileLocally(updatedUser)
        _currentUser.value = updatedUser
        _uiState.value = AuthUiState.Success(updatedUser)
        Log.d("BazaarLink", "switchRole: user ${current.displayName} switched active role to $newRole")
        viewModelScope.launch { repository.syncUserProfileToCloud(updatedUser) }
    }

    /** Register secondary role (e.g. Buyer becoming a Vendor with shop details). */
    fun registerSecondaryRole(
        newRole: String,
        vendorProfile: VendorProfile? = null
    ) {
        val current = _currentUser.value ?: return
        val updatedRoles = (current.registeredRoles + newRole).distinct()
        val updatedUser = current.copy(
            role = newRole,
            registeredRoles = updatedRoles,
            vendorProfile = if (newRole == "VENDOR") (vendorProfile ?: current.vendorProfile ?: VendorProfile(
                shopName = "${current.displayName}'s Shop",
                categories = listOf("mobile parts")
            )) else current.vendorProfile
        )
        repository.saveUserProfileLocally(updatedUser)
        _currentUser.value = updatedUser
        _uiState.value = AuthUiState.Success(updatedUser)
        Log.d("BazaarLink", "registerSecondaryRole: registered $newRole for ${current.displayName}")
        viewModelScope.launch { repository.syncUserProfileToCloud(updatedUser) }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _uiState.value = AuthUiState.Idle
    }

    // Backwards compat
    fun createOrUpdateUserProfile(
        userId: String,
        role: String,
        displayName: String,
        phoneNumber: String,
        vendorProfile: VendorProfile? = null
    ) {
        completeOnboarding(userId, "", role, displayName, phoneNumber, "", vendorProfile)
    }
}
