package com.microsoft.spamdetector

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

private lateinit var _instance: SpamDetectorApplication

class SpamDetectorApplication : Application() {

    companion object {
        val Instance: SpamDetectorApplication
            get() = _instance
    }

    private lateinit var _firebaseStorage: FirebaseStorage
    val firebaseStorageRef: FirebaseStorage
        get() = _firebaseStorage

    override fun onCreate() {
        super.onCreate()
        _instance = this
        FirebaseApp.initializeApp(this)
        _firebaseStorage = Firebase.storage("gs://anti-smisher.appspot.com")
    }
}