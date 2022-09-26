package com.microsoft.spamdetector

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageUIModel(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "sender") val sender: String,
    @ColumnInfo(name = "isSmishMessage") val isSmishMessage: Boolean
)