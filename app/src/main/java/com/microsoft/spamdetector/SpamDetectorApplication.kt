package com.microsoft.spamdetector

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

private lateinit var _instance: SpamDetectorApplication

@Suppress("DEPRECATION")
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

    private val _dict = HashMap<String, Int>()
    val dict: HashMap<String, Int>
        get() = _dict

    private lateinit var _dB: MessageDatabase

    val dao: MessageDao
        get() = _dB.getMessageDao()

    private lateinit var _interpreter: Interpreter
    val interpreter: Interpreter
        get() = _interpreter

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "nf_channel"
            val descriptionText = "nf_desc"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel("channel_id", name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
        _dB = MessageDatabase.getInstance(this)
        _instance = this
        FirebaseApp.initializeApp(this)
        _firebaseStorage = Firebase.storage("gs://anti-smisher.appspot.com")
        readAndStoreStopWords()
        loadVocabData()
        initializeModel()
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

    fun removePunctuations(input: String): String {
        return input.replace("[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]".toRegex(), "")
    }

    fun tokenize(message: String, vocabData: HashMap<String, Int>): IntArray {
        val parts: List<String> = message.split(" ")
            .filter { stopWords.contains(it) && it.length > 1 }
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
        val maxlen = 80
        if (sequence.size > maxlen) {
            return sequence.sliceArray(0..maxlen)
        } else if (sequence.size < maxlen) {
            val array = ArrayList<Int>()
            array.addAll(sequence.asList())
            val size = array.size
            for (i in size until maxlen) {
                array.add(0, 0)
            }
            return array.toIntArray()
        } else {
            return sequence
        }
    }

    private fun loadVocabData() {
        _dict.clear()
        try {
            val reader = BufferedReader(InputStreamReader(assets.open("word_dict.json")))
            val jsonString = reader.readLine()
            _dict.putAll(loadVocab(jsonString))
        } catch (e: Exception) {

        }
    }

    private fun loadVocab(json: String): HashMap<String, Int> {
        var vocabData = HashMap<String, Int>()
        val jsonObject = JSONObject(json)
        val iterator: Iterator<String> = jsonObject.keys()
        val data = HashMap<String, Int>()
        while (iterator.hasNext()) {
            val key = iterator.next()
            data[key] = jsonObject.get(key) as Int
        }
        vocabData = data
        return vocabData
    }

    private fun initializeModel() {
        _interpreter = Interpreter(loadModelFile())
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd("classifier.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    fun classifySequence(sequence: IntArray): FloatArray {
        val inputs: Array<FloatArray> = arrayOf(sequence.map { it.toFloat() }.toFloatArray())
        val outputs: Array<FloatArray> = arrayOf(floatArrayOf(0.0f, 0.0f))
        interpreter.run(inputs, outputs)
        return outputs[0]
    }
}