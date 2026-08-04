package com.bazaarlink.app.ui.account

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bazaarlink.app.R
import com.bazaarlink.app.models.User
import com.bazaarlink.app.models.VendorProfile
import com.bazaarlink.app.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    authViewModel: AuthViewModel,
    user: User,
    onSignOut: () -> Unit,
    onRoleSwitched: () -> Unit
) {
    var showVendorRegDialog by remember { mutableStateOf(false) }
    var shopNameInput by remember { mutableStateOf("") }
    var marketZoneInput by remember { mutableStateOf("Star City Mall, Saddar") }

    val context = androidx.compose.ui.platform.LocalContext.current
    var currentLang by remember { mutableStateOf(com.bazaarlink.app.util.LocaleHelper.getLanguage(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.account_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Profile Card ────────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = user.displayName.ifBlank { "BazaarLink User" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = if (user.role == "VENDOR") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (user.role == "VENDOR") stringResource(id = R.string.active_role_vendor) else stringResource(id = R.string.active_role_buyer),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (user.role == "VENDOR") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // ── Info Details Card ──────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (user.email.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = user.email, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (user.phoneNumber.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = user.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (user.cnic.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "${stringResource(id = R.string.cnic)}: ${user.cnic}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (user.role == "VENDOR" && user.vendorProfile != null) {
                            val vp = user.vendorProfile
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "${stringResource(id = R.string.vendor_shop)}: ${vp.shopName} (${vp.marketZone})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = stringResource(id = R.string.connects_balance, vp.connectsBalance), style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (vp.totalRatings < 3) "Rating: 🆕 New Merchant"
                                           else "Rating: ⭐ ${String.format("%.1f", vp.rating)} (${vp.totalRatings} reviews)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.height(10.dp))

                // ── Switch Role Button ──────────────────────────────────────────
                val targetRole = if (user.role == "BUYER") "VENDOR" else "BUYER"
                val isAlreadyRegistered = user.registeredRoles.contains(targetRole)

                Button(
                    onClick = {
                        if (targetRole == "VENDOR" && !isAlreadyRegistered) {
                            showVendorRegDialog = true
                        } else {
                            authViewModel.switchRole(targetRole, context)
                            onRoleSwitched()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (targetRole == "VENDOR") {
                                if (isAlreadyRegistered) stringResource(id = R.string.switch_to_vendor) else stringResource(id = R.string.become_vendor)
                            } else stringResource(id = R.string.switch_to_buyer),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Language Switcher Button (English / Urdu) ────────────────
                OutlinedButton(
                    onClick = {
                        val newLang = if (currentLang == "en") "ur" else "en"
                        com.bazaarlink.app.util.LocaleHelper.setLocale(context, newLang)
                        currentLang = newLang
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLang == "en") stringResource(id = R.string.lang_english_btn) else stringResource(id = R.string.lang_urdu_btn),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Sign Out Button ─────────────────────────────────────────────
                OutlinedButton(
                    onClick = {
                        authViewModel.signOut(context)
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.log_out_account), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }


    // ── Secondary Vendor Registration Dialog ────────────────────────────────
    if (showVendorRegDialog) {
        AlertDialog(
            onDismissRequest = { showVendorRegDialog = false },
            title = { Text(text = stringResource(id = R.string.register_vendor_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = stringResource(id = R.string.register_vendor_subtitle), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = shopNameInput,
                        onValueChange = { shopNameInput = it },
                        label = { Text(stringResource(id = R.string.shop_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = marketZoneInput,
                        onValueChange = { marketZoneInput = it },
                        label = { Text(stringResource(id = R.string.market_zone)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val vp = VendorProfile(
                            shopName = shopNameInput.ifBlank { "${user.displayName}'s Shop" },
                            marketZone = marketZoneInput.ifBlank { "Saddar, Karachi" },
                            categories = listOf("mobile parts"),
                            connectsBalance = 50
                        )
                        authViewModel.registerSecondaryRole("VENDOR", vp, context)
                        showVendorRegDialog = false
                        onRoleSwitched()
                    }
                ) {

                    Text(text = stringResource(id = R.string.save_and_switch))
                }
            },
            dismissButton = {
                TextButton(onClick = { showVendorRegDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }

}
