package com.microsoft.spamdetector

data class MessageUIModel(
    val id: String,
    val title: String,
    val message: String,
    val sender: String,
    val isSmishMessage: Boolean,
    val time: String,
    val folderName: String,
    val readState: String,
)