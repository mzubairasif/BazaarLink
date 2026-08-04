package com.bazaarlink.app.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarlink.app.di.ServiceLocator
import com.bazaarlink.app.models.User
import com.bazaarlink.app.models.VendorProfile
import com.bazaarlink.app.repository.BazaarLinkRepository
import com.bazaarlink.app.util.UserSessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID


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

    /** Called on app startup. If a user session is active, auto-login straight to their screen. */
    fun checkExistingSession(context: Context? = null) {
        context?.let { ctx ->
            UserSessionManager.getActiveUserSession(ctx)?.let { user ->
                Log.d("BazaarLink", "checkExistingSession: restored active session from UserSessionManager for ${user.displayName}")
                repository.saveUserProfileLocally(user)
                _currentUser.value = user
                _uiState.value = AuthUiState.Success(user)
                return
            }
        }

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
                    context?.let { UserSessionManager.saveUserSession(it, profile) }
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

    /** Directly restore a saved user session (e.g. from fallback) */
    fun restoreSavedUser(user: User) {
        repository.saveUserProfileLocally(user)
        _currentUser.value = user
        _uiState.value = AuthUiState.Success(user)
    }

    /** Sign in using Phone Number and Password */
    fun signInWithPhoneAndPassword(context: Context, phoneNumber: String, password: String) {
        _uiState.value = AuthUiState.Loading
        val cleanPhone = phoneNumber.trim()
        viewModelScope.launch {
            try {
                // 1. Check local user registry first
                var profile = UserSessionManager.findUserByPhone(context, cleanPhone)

                // 2. Query cloud repository if not found locally
                if (profile == null) {
                    profile = repository.getUserProfileByPhone(cleanPhone).getOrNull()
                }

                if (profile == null) {
                    _uiState.value = AuthUiState.Error("No account found for this phone number")
                    return@launch
                }

                // Verify password (if password was set during registration)
                if (profile.password.isNotBlank() && profile.password != password) {
                    _uiState.value = AuthUiState.Error("Invalid phone number or password")
                    return@launch
                }

                // Successful login!
                UserSessionManager.saveUserSession(context, profile)
                repository.saveUserProfileLocally(profile)
                _currentUser.value = profile
                _uiState.value = AuthUiState.Success(profile)
            } catch (e: Exception) {
                Log.e("BazaarLink", "signInWithPhoneAndPassword failed: ${e.message}", e)
                _uiState.value = AuthUiState.Error(e.message ?: "Sign-in failed")
            }
        }
    }

    /** Register a new user account with Phone Number & Password */
    fun registerUser(
        context: Context,
        role: String,
        displayName: String,
        phoneNumber: String,
        password: String,
        cnic: String,
        vendorProfile: VendorProfile? = null
    ) {
        _uiState.value = AuthUiState.Loading
        val cleanPhone = phoneNumber.trim()
        val uid = "user_${UUID.randomUUID().toString().take(8)}"
        val user = User(
            userId = uid,
            email = "",
            password = password,
            role = role,
            registeredRoles = if (role == "VENDOR") listOf("VENDOR", "BUYER") else listOf("BUYER"),
            displayName = displayName,
            phoneNumber = cleanPhone,
            cnic = cnic,
            vendorProfile = if (role == "VENDOR") (vendorProfile ?: VendorProfile(
                categories = listOf("mobile parts")
            )) else null
        )

        UserSessionManager.saveUserSession(context, user)
        repository.saveUserProfileLocally(user)
        _currentUser.value = user
        _uiState.value = AuthUiState.Success(user)
        Log.d("BazaarLink", "registerUser: profile saved for $displayName ($cleanPhone, $role)")
        viewModelScope.launch { repository.syncUserProfileToCloud(user) }
    }

    /** Backwards compatibility alias for completeOnboarding */
    fun completeOnboarding(
        context: Context? = null,
        uid: String = "",
        email: String = "",
        role: String,
        displayName: String,
        phoneNumber: String,
        cnic: String,
        vendorProfile: VendorProfile? = null,
        password: String = ""
    ) {
        if (context != null) {
            registerUser(context, role, displayName, phoneNumber, password, cnic, vendorProfile)
        }
    }


    /** Toggle active role if user is already registered for both BUYER and VENDOR roles. */
    fun switchRole(newRole: String, context: Context? = null) {
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
        context?.let { UserSessionManager.saveUserSession(it, updatedUser) }
        repository.saveUserProfileLocally(updatedUser)
        _currentUser.value = updatedUser
        _uiState.value = AuthUiState.Success(updatedUser)
        Log.d("BazaarLink", "switchRole: user ${current.displayName} switched active role to $newRole")
        viewModelScope.launch { repository.syncUserProfileToCloud(updatedUser) }
    }

    /** Register secondary role (e.g. Buyer becoming a Vendor with shop details). */
    fun registerSecondaryRole(
        newRole: String,
        vendorProfile: VendorProfile? = null,
        context: Context? = null
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
        context?.let { UserSessionManager.saveUserSession(it, updatedUser) }
        repository.saveUserProfileLocally(updatedUser)
        _currentUser.value = updatedUser
        _uiState.value = AuthUiState.Success(updatedUser)
        Log.d("BazaarLink", "registerSecondaryRole: registered $newRole for ${current.displayName}")
        viewModelScope.launch { repository.syncUserProfileToCloud(updatedUser) }
    }

    fun signOut(context: Context? = null) {
        try {
            auth.signOut()
            context?.let { ctx ->
                UserSessionManager.clearActiveSession(ctx)
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
                GoogleSignIn.getClient(ctx, gso).signOut()
            }
        } catch (e: Exception) {
            Log.w("BazaarLink", "signOut error: ${e.message}")
        }
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
        completeOnboarding(null, userId, "", role, displayName, phoneNumber, "", vendorProfile)
    }
}
