package com.bazaarlink.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bazaarlink.app.R
import com.bazaarlink.app.models.VendorProfile
import com.bazaarlink.app.viewmodels.AuthUiState
import com.bazaarlink.app.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: (isBuyer: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Toggle between Registration mode (default) and Sign In mode
    var isSignInMode by remember { mutableStateOf(false) }

    // Form states
    var selectedRole by remember { mutableStateOf("BUYER") }
    var displayName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var cnic by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var marketZone by remember { mutableStateOf("Star City Mall, Saddar") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val categories = remember { mutableStateListOf("mobile parts", "accessories") }

    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            val user = (uiState as AuthUiState.Success).user
            onAuthSuccess(user.role == "BUYER")
        }
        if (uiState is AuthUiState.Error) {
            errorMessage = (uiState as AuthUiState.Error).message
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

        if (uiState is AuthUiState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // App logo Header Card (Prominent & Balanced)
                Card(

                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(14.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.auth_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = stringResource(id = R.string.auth_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.90f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                if (isSignInMode) {
                    // ─── MODE 1: SIGN IN FORM (COMPACT) ─────────────────────────
                    Text(
                        text = stringResource(id = R.string.sign_in_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(text = stringResource(id = R.string.phone_number)) },
                        leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(text = stringResource(id = R.string.password_label)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (phoneNumber.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter phone number and password"
                                return@Button
                            }
                            errorMessage = null
                            viewModel.signInWithPhoneAndPassword(context, phoneNumber, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = stringResource(id = R.string.login), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Toggle Link: Don't have an account? Register
                    Text(
                        text = stringResource(id = R.string.dont_have_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                errorMessage = null
                                isSignInMode = false
                            }
                            .padding(6.dp)
                    )

                } else {
                    // ─── MODE 2: REGISTRATION FORM (COMPACT DEFAULT) ────────────
                    Text(
                        text = stringResource(id = R.string.onboarding_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Role selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == "BUYER",
                            onClick = { selectedRole = "BUYER" },
                            label = { Text(text = stringResource(id = R.string.role_buyer), style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedRole == "VENDOR",
                            onClick = { selectedRole = "VENDOR" },
                            label = { Text(text = stringResource(id = R.string.role_vendor), style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(text = stringResource(id = R.string.display_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(text = stringResource(id = R.string.phone_number)) },
                        leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(text = stringResource(id = R.string.password_label)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = cnic,
                        onValueChange = { cnic = it },
                        label = { Text(text = stringResource(id = R.string.cnic)) },
                        leadingIcon = { Icon(Icons.Default.Badge, null, modifier = Modifier.size(20.dp)) },
                        placeholder = { Text("12345-1234567-1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedRole == "VENDOR") {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text(text = stringResource(id = R.string.shop_name)) },
                            leadingIcon = { Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = marketZone,
                            onValueChange = { marketZone = it },
                            label = { Text(text = stringResource(id = R.string.market_zone)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = categories.contains("mobile parts"),
                                onClick = {
                                    if (categories.contains("mobile parts")) categories.remove("mobile parts")
                                    else categories.add("mobile parts")
                                },
                                label = { Text(text = stringResource(id = R.string.category_mobile_parts), style = MaterialTheme.typography.bodySmall) }
                            )
                            FilterChip(
                                selected = categories.contains("accessories"),
                                onClick = {
                                    if (categories.contains("accessories")) categories.remove("accessories")
                                    else categories.add("accessories")
                                },
                                label = { Text(text = stringResource(id = R.string.category_accessories), style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (displayName.isBlank() || phoneNumber.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill in all required fields including password"
                                return@Button
                            }
                            errorMessage = null
                            val vendorProfile = if (selectedRole == "VENDOR") VendorProfile(
                                shopName = shopName.ifBlank { "My Shop" },
                                marketZone = marketZone,
                                categories = categories.toList(),
                                connectsBalance = 50
                            ) else null

                            viewModel.registerUser(
                                context = context,
                                role = selectedRole,
                                displayName = displayName,
                                phoneNumber = phoneNumber,
                                password = password,
                                cnic = cnic,
                                vendorProfile = vendorProfile
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = stringResource(id = R.string.complete_profile), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Toggle Link: Already have an account? Sign in
                    Text(
                        text = stringResource(id = R.string.already_have_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                errorMessage = null
                                isSignInMode = true
                            }
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}
