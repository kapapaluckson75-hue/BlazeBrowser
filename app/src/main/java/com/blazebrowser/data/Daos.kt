package com.blazebrowser.data

import androidx.room.*

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAll(): List<Bookmark>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    fun findByUrl(url: String): Bookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookmark: Bookmark): Long

    @Delete
    fun delete(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    fun deleteById(id: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): List<HistoryEntry>

    @Query("SELECT * FROM history ORDER BY visitCount DESC, timestamp DESC LIMIT 100")
    fun getFrequent(): List<HistoryEntry>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    fun findByUrl(url: String): HistoryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: HistoryEntry): Long

    @Update
    fun update(entry: HistoryEntry)

    @Query("UPDATE history SET visitCount = visitCount + 1, timestamp = :timestamp WHERE url = :url")
    fun incrementVisit(url: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM history")
    fun clear()

    @Query("DELETE FROM history WHERE id = :id")
    fun deleteById(id: Long)
}

@Dao
interface TempProfileDao {
    @Query("SELECT * FROM temp_profiles ORDER BY createdAt DESC")
    fun getAll(): List<TempProfile>

    @Query("SELECT * FROM temp_profiles WHERE isDefault = 1 LIMIT 1")
    fun getDefault(): TempProfile?

    @Query("SELECT * FROM temp_profiles WHERE id = :id")
    fun getById(id: Long): TempProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(profile: TempProfile): Long

    @Update
    fun update(profile: TempProfile)

    @Query("DELETE FROM temp_profiles WHERE id = :id")
    fun deleteById(id: Long)
}

@Dao
interface TempMessageDao {
    @Query("SELECT * FROM temp_messages WHERE profileId = :profileId ORDER BY receivedAt DESC")
    fun getForProfile(profileId: Long): List<TempMessage>

    @Query("SELECT * FROM temp_messages WHERE id = :limit")
    fun getById(id: String): TempMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: TempMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(messages: List<TempMessage>)

    @Query("UPDATE temp_messages SET isRead = 1 WHERE id = :id")
    fun markRead(id: String)

    @Query("DELETE FROM temp_messages WHERE id = :id")
    fun deleteById(id: String)
}
