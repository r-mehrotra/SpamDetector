package com.microsoft.spamdetector

import android.content.Context
import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

private lateinit var _instance: MessageDatabase

@Database(
    entities = [MessageUIModel::class],
    version = 1,
    exportSchema = true
)
abstract class MessageDatabase : RoomDatabase() {


    abstract fun getMessageDao(): MessageDao

    companion object {
        fun getInstance(context: Context): MessageDatabase {
            if (!::_instance.isInitialized) {
                synchronized(MessageDatabase::class) {
                    if (!::_instance.isInitialized) {
                        _instance = Room.databaseBuilder(
                            context.applicationContext,
                            MessageDatabase::class.java,
                            "message_db"
                        ).build()
                    }
                }
            }
            return _instance
        }
    }
}