package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class CloudBackupData(
    val user: User,
    val portfolios: List<PortfolioAccount>,
    val trades: List<Trade>,
    val mistakes: List<Mistake>
)

object CloudSyncManager {
    private const val PREFS_NAME = "bookfx_cloud_sync_ledger"
    
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        
    private val adapter = moshi.adapter(CloudBackupData::class.java)

    /**
     * Saves or updates the user profile and all associated data inside our "Simulated Cloud Server".
     */
    fun saveToCloud(
        context: Context,
        user: User,
        portfolios: List<PortfolioAccount>,
        trades: List<Trade>,
        mistakes: List<Mistake>
    ) {
        try {
            val key = "user_cloud_${user.email.lowercase().trim()}"
            val backupData = CloudBackupData(user, portfolios, trades, mistakes)
            val json = adapter.toJson(backupData)
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(key, json).apply()
            Log.d("CloudSyncManager", "Successfully backed up data to cloud ledger for: ${user.email}")
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Error backing up to cloud", e)
        }
    }

    /**
     * Retrieves the cloud-stored backup data for the specified email address if it exists.
     */
    fun findInCloud(context: Context, email: String): CloudBackupData? {
        val key = "user_cloud_${email.lowercase().trim()}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(key, null) ?: return null
        
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Error restoring from cloud for: $email", e)
            null
        }
    }

    /**
     * Checks if an account with this email exists in our "Simulated Cloud Server".
     */
    fun hasCloudAccount(context: Context, email: String): Boolean {
        val key = "user_cloud_${email.lowercase().trim()}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(key)
    }
}
