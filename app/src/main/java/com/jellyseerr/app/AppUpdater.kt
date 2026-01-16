package com.jellyseerr.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.HostnameVerifier

class AppUpdater(private val context: Context) {

    private val TAG = "AppUpdater"
    private val UPDATE_URL = "https://cdn-update.adelin.org/jellyseerr/app/update.json"
    private val currentVersionCode = BuildConfig.VERSION_CODE

    data class UpdateInfo(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val apkUrl: String,
        val whatsNew: String,
        val mandatory: Boolean = false
    )

    suspend fun checkForUpdate(showToast: Boolean = true): UpdateInfo? {
        Log.d(TAG, "🔍 Checking for update...")
        Log.d(TAG, "📱 Current version: $currentVersionCode")
        Log.d(TAG, "🔗 Update URL: $UPDATE_URL")

        return try {
            withContext(Dispatchers.IO) {
                // Crează SSL context care acceptă toate certificatele
                val sslContext = SSLContext.getInstance("TLS")

                // CORECTAT: Creează array de TrustManager
                val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                )

                sslContext.init(null, trustAllCerts, SecureRandom())

                val url = URL(UPDATE_URL)
                val connection = url.openConnection() as HttpsURLConnection

                // Folosește SSL context personalizat
                connection.sslSocketFactory = sslContext.socketFactory

                // CORECTAT: Folosește HostnameVerifier explicit
                connection.hostnameVerifier = HostnameVerifier { _, _ -> true }

                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Jellyseerr-App-Updater")

                Log.d(TAG, "📡 Connecting to server...")
                connection.connect()

                val responseCode = connection.responseCode
                Log.d(TAG, "📡 Response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "📦 JSON received: ${jsonText.take(200)}...")

                    val json = JSONObject(jsonText)
                    val serverVersion = json.getInt("version_code")

                    Log.d(TAG, "📊 Server version: $serverVersion, Current: $currentVersionCode")

                    if (serverVersion > currentVersionCode) {
                        val updateInfo = UpdateInfo(
                            latestVersionCode = serverVersion,
                            latestVersionName = json.getString("version_name"),
                            apkUrl = json.getString("apk_url"),
                            whatsNew = json.getString("whats_new"),
                            mandatory = json.optBoolean("mandatory", false)
                        )

                        Log.d(TAG, "🎉 Update available! v${updateInfo.latestVersionName}")

                        if (showToast) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Update disponibil: v${updateInfo.latestVersionName}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        return@withContext updateInfo
                    } else {
                        Log.d(TAG, "✅ App is up to date")
                        if (showToast) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Aplicația este la zi!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "❌ Server error: $responseCode")
                }

                connection.disconnect()
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking for update: ${e.message}")
            e.printStackTrace()

            if (showToast) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Eroare la verificarea update-ului",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            return null
        }
    }

    fun downloadAndInstall(apkUrl: String) {
        try {
            Log.d(TAG, "⬇️ Starting download: $apkUrl")

            // Crează nume unic pentru fișier
            val timestamp = System.currentTimeMillis()
            val fileName = "jellyseerr_update_$timestamp.apk"

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // Șterge fișierele vechi de update
            deleteOldApkFiles()

            val request = DownloadManager.Request(Uri.parse(apkUrl))

            // Setări download
            request.setTitle("Jellyseerr Update")
            request.setDescription("Se descarcă actualizarea...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            request.setMimeType("application/vnd.android.package-archive")
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(false)
            request.allowScanningByMediaScanner()

            // Adaugă header-uri pentru User-Agent
            request.addRequestHeader("User-Agent", "Jellyseerr-App-Updater/1.0")

            Log.d(TAG, "📝 File will be saved as: $fileName")

            // Începe download
            val downloadId = downloadManager.enqueue(request)
            Log.d(TAG, "📥 Download ID: $downloadId")

            // Salvează downloadId pentru verificare
            val prefs = context.getSharedPreferences("app_updater", Context.MODE_PRIVATE)
            prefs.edit().putLong("last_download_id", downloadId).apply()

            Toast.makeText(context, "Se descarcă actualizarea... ⬇️", Toast.LENGTH_SHORT).show()

            // Monitorizează finalizarea download-ului
            setupDownloadReceiver(downloadId, fileName)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Download error: ${e.message}")
            Toast.makeText(context, "Eroare la descărcare: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun setupDownloadReceiver(downloadId: Long, fileName: String) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                if (id == downloadId) {
                    Log.d(TAG, "📊 Download completed with ID: $id")

                    try {
                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val query = DownloadManager.Query()
                        query.setFilterById(downloadId)

                        val cursor = downloadManager.query(query)

                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    Log.d(TAG, "✅ Download successful!")

                                    // Obține calea fișierului
                                    val uriString = cursor.getString(
                                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                                    )

                                    val fileUri = Uri.parse(uriString)
                                    Log.d(TAG, "📁 File URI: $fileUri")

                                    // Așteaptă puțin pentru ca fișierul să fie complet scris
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        installApk(fileUri)
                                    }, 1000)
                                }

                                DownloadManager.STATUS_FAILED -> {
                                    val reason = cursor.getInt(
                                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                                    )
                                    Log.e(TAG, "❌ Download failed. Reason code: $reason")
                                    Toast.makeText(
                                        context,
                                        "Descărcarea a eșuat! Cod eroare: $reason",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                else -> {
                                    Log.d(TAG, "ℹ️ Download status: $status")
                                }
                            }
                        } else {
                            Log.e(TAG, "❌ Cursor is empty!")
                        }

                        cursor.close()

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error processing download: ${e.message}")
                        Toast.makeText(context, "Eroare procesare download", Toast.LENGTH_SHORT).show()
                    }

                    // Dezînregistrează receiver-ul
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // Ignoră eroarea dacă receiver-ul nu este înregistrat
                    }
                }
            }
        }

        // Înregistrează receiver-ul
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        Log.d(TAG, "👂 Download receiver registered")
    }

    private fun installApk(apkUri: Uri) {
        try {
            Log.d(TAG, "🔧 Starting installation...")

            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Pentru Android 7+ (Nougat și mai sus)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    // Extrage calea fișierului din URI
                    val filePath = apkUri.path ?: throw Exception("Invalid file path")
                    val apkFile = File(filePath)

                    Log.d(TAG, "📁 APK File: ${apkFile.absolutePath}")
                    Log.d(TAG, "📁 File exists: ${apkFile.exists()}, Size: ${apkFile.length()} bytes")

                    if (!apkFile.exists()) {
                        throw Exception("APK file not found")
                    }

                    // Folosește FileProvider pentru a partaja fișierul
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        apkFile
                    )

                    Log.d(TAG, "📦 Content URI: $contentUri")

                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
                    intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)

                } catch (e: Exception) {
                    Log.e(TAG, "❌ FileProvider error: ${e.message}")
                    // Încearcă cu URI direct
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                }
            } else {
                // Pentru Android vechi
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            }

            // Verifică dacă există o activitate care să gestioneze acest intent
            if (intent.resolveActivity(context.packageManager) != null) {
                Log.d(TAG, "🚀 Starting installer activity...")
                Toast.makeText(context, "Se instalează actualizarea... 🔄", Toast.LENGTH_SHORT).show()

                context.startActivity(intent)

                // După 3 secunde, verifică dacă trebuie să închidă aplicația
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "🔄 Installation should be complete")
                }, 3000)

            } else {
                Log.e(TAG, "❌ No activity found to handle installation")
                Toast.makeText(context, "Nu s-a găsit installer-ul! Instalează manual din Downloads.", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Installation error: ${e.message}")
            Toast.makeText(context, "Eroare instalare: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun deleteOldApkFiles() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                val apkFiles = downloadsDir.listFiles { file ->
                    file.name.startsWith("jellyseerr_update_") && file.name.endsWith(".apk")
                }

                apkFiles?.forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "🗑️ Deleted old APK: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting old APK files: ${e.message}")
        }
    }
}