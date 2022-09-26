package com.microsoft.spamdetector

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addMessages(list: List<MessageUIModel>)

    @Query("SELECT * FROM messages WHERE isSmishMessage == 1")
    fun getSpamMessages(): LiveData<List<MessageUIModel>>

    @Query("SELECT * FROM messages WHERE isSmishMessage == 0")
    fun getHamMessages(): LiveData<List<MessageUIModel>>

}