package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class CloudBackupData(
    val user: User,
    val portfolios: List<PortfolioAccount>,
    val trades: List<Trade>,
    val mistakes: List<Mistake>,
    val deletedTradeKeys: List<String> = emptyList(),
    val deletedMistakeKeys: List<String> = emptyList(),
    val deletedPortfolioKeys: List<String> = emptyList(),
    val lastUpdatedPortfolioKeys: Map<String, Long> = emptyMap(),
    val lastUpdatedTradeKeys: Map<String, Long> = emptyMap(),
    val lastUpdatedMistakeKeys: Map<String, Long> = emptyMap(),
    val lastUpdatedUser: Long = 0L
)

object CloudSyncManager {
    private const val BUCKET_ID = "L4f3GGBK1bgW7nVjFNaZAq"
    private const val BASE_URL = "https://kvdb.io/$BUCKET_ID/"
    
    // Fallback SharedPreferences database in case the device is offline or kvdb is unreachable
    private const val PREFS_NAME = "bookfx_cloud_sync_ledger"
    
    @Volatile
    var lastError: String? = null
        private set
    
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        
    private val adapter = moshi.adapter(CloudBackupData::class.java)

    private fun hashEmail(email: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(email.lowercase().trim().toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Safe fallback if SHA-256 is somehow unavailable
            email.lowercase().trim().replace(Regex("[^a-zA-Z0-9]"), "_")
        }
    }

    /**
     * Saves or updates the user profile and all associated data inside our "KVdb Cloud Server".
     */
    suspend fun saveToCloud(
        context: Context,
        user: User,
        portfolios: List<PortfolioAccount>,
        trades: List<Trade>,
        mistakes: List<Mistake>,
        deletedTradeKeys: List<String> = emptyList(),
        deletedMistakeKeys: List<String> = emptyList(),
        deletedPortfolioKeys: List<String> = emptyList(),
        lastUpdatedPortfolioKeys: Map<String, Long> = emptyMap(),
        lastUpdatedTradeKeys: Map<String, Long> = emptyMap(),
        lastUpdatedMistakeKeys: Map<String, Long> = emptyMap(),
        lastUpdatedUser: Long = 0L
    ): Boolean = withContext(Dispatchers.IO) {
        val emailKey = hashEmail(user.email)
        val backupData = CloudBackupData(
            user = user,
            portfolios = portfolios,
            trades = trades,
            mistakes = mistakes,
            deletedTradeKeys = deletedTradeKeys,
            deletedMistakeKeys = deletedMistakeKeys,
            deletedPortfolioKeys = deletedPortfolioKeys,
            lastUpdatedPortfolioKeys = lastUpdatedPortfolioKeys,
            lastUpdatedTradeKeys = lastUpdatedTradeKeys,
            lastUpdatedMistakeKeys = lastUpdatedMistakeKeys,
            lastUpdatedUser = lastUpdatedUser
        )
        val json = adapter.toJson(backupData)
        
        // 1. First, save to local fallback preferences so there's always an offline copy
        try {
            val localPrefsKey = "user_cloud_${user.email.lowercase().trim()}"
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(localPrefsKey, json).apply()
            Log.d("CloudSyncManager", "Successfully backed up locally for offline use: ${user.email}")
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to save offline copy", e)
        }

        var isSuccess = false
        // 2. Perform background write to global KVdb cloud storage
        try {
            val url = URL("$BASE_URL$emailKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            val out = conn.outputStream
            out.write(json.toByteArray(Charsets.UTF_8))
            out.close()
            
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Log.d("CloudSyncManager", "Successfully backed up data to KVdb cloud ledger for: ${user.email}")
                isSuccess = true
                lastError = null
            } else {
                val errorMsg = "HTTP $responseCode: ${conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Empty error response"}"
                lastError = errorMsg
                Log.e("CloudSyncManager", "Network returned non-success response code: $responseCode when uploading sync ledger")
            }
        } catch (e: Exception) {
            lastError = e.toString()
            Log.e("CloudSyncManager", "Error backing up to KVdb cloud over network", e)
        }
        return@withContext isSuccess
    }

    /**
     * Retrieves the cloud-stored backup data for the specified email address if it exists.
     */
    suspend fun findInCloud(context: Context, email: String): CloudBackupData? = withContext(Dispatchers.IO) {
        val emailKey = hashEmail(email)
        
        // 1. Attempt to fetch from network KVdb cloud storage
        try {
            val url = URL("$BASE_URL$emailKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val json = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val data = adapter.fromJson(json)
                if (data != null) {
                    Log.d("CloudSyncManager", "Successfully restored data from global KVdb cloud for: $email")
                    // Also cache locally to keep everything in sync
                    try {
                        val localPrefsKey = "user_cloud_${email.lowercase().trim()}"
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(localPrefsKey, json).apply()
                    } catch (e: Exception) {
                        Log.e("CloudSyncManager", "Failed to cache fetched copy locally", e)
                    }
                    return@withContext data
                }
            } else {
                Log.d("CloudSyncManager", "No KVdb data found over network (code: $responseCode) for: $email")
            }
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to restore from KVdb cloud over network, will fall back to local database", e)
        }

        // 2. Fall back to local backup file/preference cache if network was down or unreachable
        try {
            val localPrefsKey = "user_cloud_${email.lowercase().trim()}"
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(localPrefsKey, null)
            if (json != null) {
                Log.d("CloudSyncManager", "Found offline cached backup copy for: $email")
                return@withContext adapter.fromJson(json)
            }
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Error reading local cache fallback for: $email", e)
        }
        
        return@withContext null
    }

    /**
     * Checks if an account with this email exists in our cloud server or locally.
     */
    suspend fun hasCloudAccount(context: Context, email: String): Boolean = withContext(Dispatchers.IO) {
        val emailKey = hashEmail(email)
        try {
            val url = URL("$BASE_URL$emailKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val responseCode = conn.responseCode
            if (responseCode == 200) return@withContext true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Error checking account existence over network", e)
        }
        
        val localPrefsKey = "user_cloud_${email.lowercase().trim()}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return@withContext prefs.contains(localPrefsKey)
    }
}
