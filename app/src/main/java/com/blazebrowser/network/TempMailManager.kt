package com.blazebrowser.network

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class TempMailProfile(
    val id: String,
    val email: String,
    val token: String,
    val createdAt: Long
)

data class TempMailMessage(
    val id: String,
    val from: String,
    val subject: String,
    val body: String,
    val date: String
)

class TempMailManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blaze_temp_mail", Context.MODE_PRIVATE)
    
    companion object {
        private const val API_BASE = "https://api.mail.tm"
    }
    
    fun getProfiles(): List<TempMailProfile> {
        val json = prefs.getString("profiles", "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map {
            val obj = array.getJSONObject(it)
            TempMailProfile(
                obj.getString("id"),
                obj.getString("email"),
                obj.getString("token"),
                obj.getLong("createdAt")
            )
        }
    }
    
    private fun saveProfiles(profiles: List<TempMailProfile>) {
        val array = JSONArray()
        profiles.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("email", it.email)
            obj.put("token", it.token)
            obj.put("createdAt", it.createdAt)
            array.put(obj)
        }
        prefs.edit().putString("profiles", array.toString()).apply()
    }
    
    fun createProfile(password: String = "BlazeBrowser123!"): Result<TempMailProfile> {
        return try {
            // Get available domains
            val domains = getAvailableDomains()
            if (domains.isEmpty()) return Result.failure(Exception("No available domains"))
            
            val domain = domains.first()
            val username = generateUsername()
            val email = "$username@$domain"
            
            // Create account
            val createResult = createAccount(email, password)
            if (createResult.isFailure) return createResult
            
            val accountId = createResult.getOrThrow()
            
            // Get token
            val tokenResult = getToken(email, password)
            if (tokenResult.isFailure) return Result.failure(tokenResult.exceptionOrNull() ?: Exception("Token failed"))
            
            val token = tokenResult.getOrThrow()
            
            val profile = TempMailProfile(accountId, email, token, System.currentTimeMillis())
            val profiles = getProfiles().toMutableList()
            profiles.add(profile)
            saveProfiles(profiles)
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun deleteProfile(profile: TempMailProfile) {
        val profiles = getProfiles().filter { it.id != profile.id }
        saveProfiles(profiles)
    }
    
    fun getMessages(profile: TempMailProfile): Result<List<TempMailMessage>> {
        return try {
            val url = URL("$API_BASE/messages")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer ${profile.token}")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()
                
                val json = JSONObject(response)
                val messages = json.getJSONArray("hydra:member")
                val result = (0 until messages.length()).map {
                    val msg = messages.getJSONObject(it)
                    TempMailMessage(
                        msg.getString("id"),
                        msg.getJSONObject("from").getString("address"),
                        msg.getString("subject"),
                        msg.getString("intro"),
                        msg.getString("createdAt")
                    )
                }
                Result.success(result)
            } else {
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getMessage(profile: TempMailProfile, messageId: String): Result<TempMailMessage> {
        return try {
            val url = URL("$API_BASE/messages/$messageId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer ${profile.token}")
            
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()
                
                val msg = JSONObject(response)
                Result.success(TempMailMessage(
                    msg.getString("id"),
                    msg.getJSONObject("from").getString("address"),
                    msg.getString("subject"),
                    msg.getString("text") ?: msg.getString("html") ?: "",
                    msg.getString("createdAt")
                ))
            } else {
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getAvailableDomains(): List<String> {
        return try {
            val url = URL("$API_BASE/domains")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()
                
                val json = JSONObject(response)
                val domains = json.getJSONArray("hydra:member")
                (0 until domains.length()).map {
                    domains.getJSONObject(it).getString("domain")
                }
            } else {
                conn.disconnect()
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun createAccount(email: String, password: String): Result<String> {
        return try {
            val url = URL("$API_BASE/accounts")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            
            val body = JSONObject()
            body.put("address", email)
            body.put("password", password)
            
            conn.outputStream.write(body.toString().toByteArray())
            
            val responseCode = conn.responseCode
            if (responseCode == 201) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()
                
                val json = JSONObject(response)
                Result.success(json.getString("id"))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getToken(email: String, password: String): Result<String> {
        return try {
            val url = URL("$API_BASE/token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            
            val body = JSONObject()
            body.put("address", email)
            body.put("password", password)
            
            conn.outputStream.write(body.toString().toByteArray())
            
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()
                
                val json = JSONObject(response)
                Result.success(json.getString("token"))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateUsername(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10).map { chars.random() }.joinToString("")
    }
}