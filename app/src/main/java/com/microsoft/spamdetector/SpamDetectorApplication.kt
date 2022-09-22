package com.microsoft.spamdetector

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception

private lateinit var _instance: SpamDetectorApplication

class SpamDetectorApplication : Application() {

    companion object {
        val Instance: SpamDetectorApplication
            get() = _instance
    }

    private lateinit var _firebaseStorage: FirebaseStorage
    val firebaseStorageRef: FirebaseStorage
        get() = _firebaseStorage

    private val _stopWords = HashSet<String>()
    val stopWords: Set<String>
        get() = _stopWords

    override fun onCreate() {
        super.onCreate()
        _instance = this
        FirebaseApp.initializeApp(this)
        _firebaseStorage = Firebase.storage("gs://anti-smisher.appspot.com")
        readAndStoreStopWords()
    }

    private fun readAndStoreStopWords() {
        _stopWords.clear()
        try {
            val reader = BufferedReader(InputStreamReader(assets.open("stopwords.json")))
            val jsonString = reader.readLine()
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                _stopWords.add(jsonArray.getString(i) ?: "")
            }
        } catch (e: Exception) {

        }
    }
}