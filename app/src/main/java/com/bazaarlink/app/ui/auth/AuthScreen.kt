package com.bazaarlink.app.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bazaarlink.app.R
import com.bazaarlink.app.models.VendorProfile
import com.bazaarlink.app.viewmodels.AuthUiState
import com.bazaarlink.app.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: (isBuyer: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Onboarding form state
    var selectedRole by remember { mutableStateOf("BUYER") }
    var displayName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var marketZone by remember { mutableStateOf("Star City Mall, Saddar") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val categories = remember { mutableStateListOf("mobile parts", "accessories") }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val email = account.email ?: ""
                val googleName = account.displayName ?: ""
                val uid = account.id ?: (if (email.isNotBlank()) email.replace(".", "_") else UUID.randomUUID().toString())

                viewModel.onGoogleAccountSelected(
                    uid = uid,
                    email = email,
                    displayName = googleName
                )
            } catch (e: Exception) {
                errorMessage = "Google Account error: ${e.message}"
            }
        }
    }

    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            val user = (uiState as AuthUiState.Success).user
            onAuthSuccess(user.role == "BUYER")
        }
        if (uiState is AuthUiState.Error) {
            errorMessage = (uiState as AuthUiState.Error).message
        }
        // Pre-fill name from Google when onboarding
        if (uiState is AuthUiState.NeedsOnboarding) {
            val name = (uiState as AuthUiState.NeedsOnboarding).googleDisplayName
            if (displayName.isBlank() && name.isNotBlank()) displayName = name
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

        when (uiState) {
            is AuthUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is AuthUiState.NeedsOnboarding -> {
                // ─── Onboarding form ────────────────────────────────────────
                val onboarding = uiState as AuthUiState.NeedsOnboarding
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(id = R.string.onboarding_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = onboarding.email.ifBlank { "Google Account Verified" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Role selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == "BUYER",
                            onClick = { selectedRole = "BUYER" },
                            label = { Text(text = stringResource(id = R.string.role_buyer)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedRole == "VENDOR",
                            onClick = { selectedRole = "VENDOR" },
                            label = { Text(text = stringResource(id = R.string.role_vendor)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(text = stringResource(id = R.string.display_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(text = stringResource(id = R.string.phone_number)) },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cnic,
                        onValueChange = { cnic = it },
                        label = { Text(text = stringResource(id = R.string.cnic)) },
                        leadingIcon = { Icon(Icons.Default.Badge, null) },
                        placeholder = { Text("12345-1234567-1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedRole == "VENDOR") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text(text = stringResource(id = R.string.shop_name)) },
                            leadingIcon = { Icon(Icons.Default.ShoppingCart, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = marketZone,
                            onValueChange = { marketZone = it },
                            label = { Text(text = stringResource(id = R.string.market_zone)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.select_categories),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = categories.contains("mobile parts"),
                                onClick = {
                                    if (categories.contains("mobile parts")) categories.remove("mobile parts")
                                    else categories.add("mobile parts")
                                },
                                label = { Text(text = stringResource(id = R.string.category_mobile_parts)) }
                            )
                            FilterChip(
                                selected = categories.contains("accessories"),
                                onClick = {
                                    if (categories.contains("accessories")) categories.remove("accessories")
                                    else categories.add("accessories")
                                },
                                label = { Text(text = stringResource(id = R.string.category_accessories)) }
                            )
                        }
                    }

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (displayName.isBlank() || phoneNumber.isBlank()) {
                                errorMessage = "Please fill in all required fields"
                                return@Button
                            }
                            val vendorProfile = if (selectedRole == "VENDOR") VendorProfile(
                                shopName = shopName.ifBlank { "My Shop" },
                                marketZone = marketZone,
                                categories = categories.toList(),
                                connectsBalance = 50
                            ) else null
                            viewModel.completeOnboarding(
                                uid = onboarding.uid,
                                email = onboarding.email,
                                role = selectedRole,
                                displayName = displayName,
                                phoneNumber = phoneNumber,
                                cnic = cnic,
                                vendorProfile = vendorProfile
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = stringResource(id = R.string.complete_profile), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            else -> {
                // ─── Landing / Sign-In screen ────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.auth_title),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = stringResource(id = R.string.auth_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = stringResource(id = R.string.welcome_headline),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    errorMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Google Sign-In button
                    Button(
                        onClick = {
                            errorMessage = null
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(client.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "G", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = stringResource(id = R.string.sign_in_with_google), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
