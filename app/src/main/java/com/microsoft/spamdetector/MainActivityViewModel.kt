package com.microsoft.spamdetector

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel

class MainActivityViewModel(app: Application) : AndroidViewModel(app) {

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
}