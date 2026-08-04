package com.bazaarlink.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.bazaarlink.app.ui.navigation.NavGraph
import com.bazaarlink.app.ui.theme.BazaarLinkTheme
import com.bazaarlink.app.util.LocaleHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val localizedContext = LocaleHelper.applyLocale(newBase)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Verify Firebase is connected
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        Log.d("BazaarLink", "=== APP STARTUP ===")
        Log.d("BazaarLink", "Firebase Auth current user: ${auth.currentUser?.uid ?: "NONE"}")
        Log.d("BazaarLink", "Firestore app: ${db.app.name}")

        // Ensure valid Firebase Auth session
        if (auth.currentUser == null) {
            Log.d("BazaarLink", "No auth session, signing in anonymously...")
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("BazaarLink", "Anonymous Auth SUCCESS: uid=${it.user?.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e("BazaarLink", "Anonymous Auth FAILED: ${e.message}", e)
                }
        } else {
            Log.d("BazaarLink", "Existing auth session: uid=${auth.currentUser?.uid}")
        }

        // Quick connectivity test: write a heartbeat document
        db.collection("_heartbeat").document("test")
            .set(mapOf("timestamp" to System.currentTimeMillis(), "source" to "android"))
            .addOnSuccessListener {
                Log.d("BazaarLink", "Firestore CONNECTIVITY TEST: SUCCESS - Cloud is reachable")
            }
            .addOnFailureListener { e ->
                Log.e("BazaarLink", "Firestore CONNECTIVITY TEST: FAILED - ${e.message}", e)
            }

        setContent {
            val context = LocalContext.current
            val currentLang = remember { LocaleHelper.getLanguage(context) }
            val layoutDirection = if (currentLang == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                BazaarLinkTheme {
                    NavGraph()
                }
            }
        }
    }
}

