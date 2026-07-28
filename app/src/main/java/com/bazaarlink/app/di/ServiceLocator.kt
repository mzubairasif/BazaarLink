package com.bazaarlink.app.di

import android.util.Log
import com.bazaarlink.app.repository.BazaarLinkRepository
import com.bazaarlink.app.repository.BazaarLinkRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object ServiceLocator {

    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val firebaseFirestore: FirebaseFirestore by lazy {
        val db = FirebaseFirestore.getInstance()
        Log.d("BazaarLink", "Firestore instance initialized: ${db.app.name}")
        db
    }

    val repository: BazaarLinkRepository by lazy {
        BazaarLinkRepositoryImpl(firebaseFirestore)
    }
}
