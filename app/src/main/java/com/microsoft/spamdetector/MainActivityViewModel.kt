package com.microsoft.spamdetector

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel

class MainActivityViewModel(app: Application) : AndroidViewModel(app) {

    val maxlen = 80
    val messageListVisibility = ObservableInt(
        when (arePermissionsGranted()) {
            true -> View.VISIBLE
            false -> View.GONE
        }
    )
    val permissionButtonVisibility = ObservableInt(
        when (arePermissionsGranted()) {
            true -> View.GONE
            false -> View.VISIBLE
        }
    )

    val permissionList = listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)

    fun arePermissionsGranted(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            getApplication<Application>(),
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            getApplication<Application>(),
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        return granted
    }

    fun setVisibilities(granted: Boolean) {
        when (granted) {
            true -> {
                messageListVisibility.set(View.VISIBLE)
                permissionButtonVisibility.set(View.GONE)
            }
            false -> {
                messageListVisibility.set(View.GONE)
                permissionButtonVisibility.set(View.VISIBLE)
            }
        }
    }

    fun removePunctuations(input: String): String {
        return input.replace("[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]".toRegex(), "")
    }

    fun tokenize(message: String, vocabData: HashMap<String, Int>): IntArray {
        val parts: List<String> = message.split(" ")
            .filter { !getApplication<SpamDetectorApplication>().stopWords.contains(it) && it.length > 1 }
            .map(this::removePunctuations)

        val tokenizedMessage = ArrayList<Int>()
        for (part in parts) {
            if (part.trim() != "") {
                var index: Int? = 0
                index = if (vocabData[part] == null) {
                    0
                } else {
                    vocabData[part]
                }
                tokenizedMessage.add(index!!)
            }
        }
        return tokenizedMessage.toIntArray()
    }

    fun filterInput(input: String, vocabData: HashMap<String, Int>): IntArray {
        val tokenizedMessage = tokenize(input, vocabData)
        val paddedMessage = padSequence(tokenizedMessage)
        return paddedMessage
    }

    // Pad the given sequence to maxlen with zeros.
    fun padSequence(sequence: IntArray): IntArray {
        val maxlen = this.maxlen
        if (sequence.size > maxlen) {
            return sequence.sliceArray(0..maxlen)
        } else if (sequence.size < maxlen) {
            val array = ArrayList<Int>()
            array.addAll(sequence.asList())
            for (i in array.size until maxlen) {
                array.add(0)
            }
            return array.toIntArray()
        } else {
            return sequence
        }
    }
}