package com.microsoft.spamdetector

import android.Manifest
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.microsoft.spamdetector.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.File

class MainActivity : AppCompatActivity() {

    val maxlen = 171
    val requestPermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) {
        if (it[Manifest.permission.RECEIVE_SMS] != null && it[Manifest.permission.READ_SMS] != null) {
            _viewModel.setVisibilities(true)
            fetchMessages()
        } else {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Permissions are required")
                .setMessage("Can proceed without required permissions")
                .setPositiveButton(
                    "OK!"
                ) { dialog, which ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }

    private lateinit var _binding: ActivityMainBinding
    private lateinit var _viewModel: MainActivityViewModel
    private lateinit var _adapter: MessageListAdapter
    private var interpreter: Interpreter? = null
    private var tempFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ref =
            SpamDetectorApplication.Instance.firebaseStorageRef.reference.child("/word_dict.json")
        val localFile = File.createTempFile("jsonFiles", "json")
        ref.getFile(localFile).addOnSuccessListener {
            tempFile = localFile
            _adapter = MessageListAdapter(ArrayList())
            _viewModel = ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory(application)
            )[MainActivityViewModel::class.java]
            _binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
            _binding.viewModel = _viewModel
            checkForPermissions()
            setupUI()
            downloadModel()
        }.addOnFailureListener {}
    }

    private fun downloadModel() {
        val downloadConditions = CustomModelDownloadConditions.Builder()
            .build()
        FirebaseModelDownloader.getInstance()
            .getModel(
                "SpamDetector",
                DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND,
                downloadConditions
            )
            .addOnSuccessListener { model: CustomModel? ->
                val modelFile = model?.file
                if (modelFile != null) {
                    interpreter = Interpreter(modelFile)
                }
            }

    }

    private fun checkForPermissions() {
        if (_viewModel.arePermissionsGranted()) {
            _viewModel.setVisibilities(true)
            fetchMessages()
        } else {
            _viewModel.setVisibilities(false)
        }
    }

    private fun setupUI() {
        _binding.permissionButton.setOnClickListener {
            requestPermissionLauncher.launch(_viewModel.permissionList.toTypedArray())
        }
        _binding.messageView.layoutManager = LinearLayoutManager(this)
        _binding.messageView.adapter = _adapter
    }

    private suspend fun loadVocab(json: String): HashMap<String, Int> {
        var vocabData = HashMap<String, Int>()
        withContext(Dispatchers.Default) {
            val jsonObject = JSONObject(json)
            val iterator: Iterator<String> = jsonObject.keys()
            val data = HashMap<String, Int>()
            while (iterator.hasNext()) {
                val key = iterator.next()
                data[key] = jsonObject.get(key) as Int
            }
            vocabData = data
        }
        return vocabData
    }


    // Tokenize the given sentence
    fun tokenize(message: String, vocabData: HashMap<String, Int>): IntArray {
        val parts: List<String> = message.split(" ")
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


    fun classifySequence(sequence: IntArray): FloatArray {
        val inputs: Array<FloatArray> = arrayOf(sequence.map { it.toFloat() }.toFloatArray())
        val outputs: Array<FloatArray> = arrayOf(floatArrayOf(0.0f, 0.0f))
        interpreter!!.run(inputs, outputs)
        return outputs[0]
    }

    private fun fetchMessages() {
        lifecycleScope.launch {
            try {
                val list = ArrayList<MessageUIModel>()
                withContext(Dispatchers.IO) {
                    val dictData = tempFile!!.readText()
                    val vocab = loadVocab(dictData)

                    val uriSms = Uri.parse("content://sms")
                    val cursor = contentResolver.query(uriSms, null, null, null, null)!!
                    cursor.moveToFirst()
                    val itemCount = cursor.count
                    for (i in 0 until itemCount) {
                        val message = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                        val tokenizedMessage = tokenize(message.lowercase().trim(), vocab)
                        val paddedMessage = padSequence(tokenizedMessage)
                        val results = classifySequence(paddedMessage)
                        val item = MessageUIModel(
                            id = cursor.getString(cursor.getColumnIndexOrThrow("_id")),
                            title = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                            sender = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                            message = message,
                            readState = cursor.getString(cursor.getColumnIndexOrThrow("read")),
                            time = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                            folderName = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                            isSmishMessage = results[0] >= 0.8f,
                        )
                        list.add(item)
                        Log.d("MainActivity", "${results[0]} ${results[1]} $item")
                        cursor.moveToNext()
                    }
                    cursor.close()
                }
                _adapter.updateList(list)
            } catch (e: Exception) {
                _adapter.updateList(ArrayList())
            }
        }
    }
}