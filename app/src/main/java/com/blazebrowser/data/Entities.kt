package com.blazebrowser.data

import androidx.room.*
import java.util.Date

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val faviconUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "temp_profiles")
data class TempProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val password: String,
    val token: String = "",
    val domain: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "temp_messages")
data class TempMessage(
    @PrimaryKey val id: String,
    val profileId: Long,
    val fromAddress: String,
    val fromName: String,
    val subject: String,
    val preview: String = "",
    val bodyHtml: String = "",
    val bodyText: String = "",
    val isRead: Boolean = false,
    val receivedAt: Long = System.currentTimeMillis()
)
