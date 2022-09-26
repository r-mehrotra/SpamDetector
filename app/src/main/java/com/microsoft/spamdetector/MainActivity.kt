package com.microsoft.spamdetector

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
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

val tabs = arrayOf("Spam Messages", "Ham Messages")

class MainActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        _viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[MainActivityViewModel::class.java]
        _binding.viewModel = _viewModel
        checkForPermissions()
        setupUI()
        _binding.viewPager.adapter = MessageViewPagerAdapter(this)
        TabLayoutMediator(_binding.tabLayout, _binding.viewPager) { tab, positions ->
            tab.text = tabs[positions]
        }
        _binding.tabLayout.setTabTextColors(
            R.color.black,
            androidx.appcompat.R.color.material_blue_grey_800
        )
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
    }


    private fun fetchMessages() {
        lifecycleScope.launch {
            try {
                val list = ArrayList<MessageUIModel>()
                var hamCount = 0
                var totalCount = 0
                var spamCount = 0
                withContext(Dispatchers.IO) {
                    val uriSms = Uri.parse("content://sms")
                    val cursor = contentResolver.query(uriSms, null, null, null, null)!!
                    cursor.moveToFirst()
                    val itemCount = cursor.count
                    for (i in 0 until itemCount) {
                        val message = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                        val filteredMessage =
                            _viewModel.filterInput(
                                message.lowercase().trim(),
                                SpamDetectorApplication.Instance.dict
                            )
                        val results =
                            SpamDetectorApplication.Instance.classifySequence(filteredMessage)
                        val item = MessageUIModel(
                            id = cursor.getString(cursor.getColumnIndexOrThrow("_id")),
                            sender = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                            message = message,
                            isSmishMessage = results[1] >= results[0],
                        )
                        if (item.isSmishMessage) {
                            spamCount++
                        } else {
                            hamCount++
                        }
                        list.add(item)
                        Log.d("MainActivity", "${results[0]} $item")
                        cursor.moveToNext()
                    }
                    cursor.close()
                    totalCount = list.size
                    SpamDetectorApplication.Instance.dao.addMessages(list)
                }
                _viewModel.updateMessageCount(totalCount)
                _viewModel.updateSpamCount(spamCount)
                _viewModel.updateNormalMessageCounrt(hamCount)
            } catch (e: Exception) {
                Log.e("MainActivity", "$e")
            }
        }
    }
}