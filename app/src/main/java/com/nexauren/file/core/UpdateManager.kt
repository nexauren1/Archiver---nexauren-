package com.nexauren.file.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val releaseName: String,
    val notes: String,
    val downloadUrl: String
)

object UpdateManager {
    const val CURRENT_VERSION = "1.0.0"
    private const val API_URL =
        "https://api.github.com/repos/nexauren1/Archiver---nexauren-/releases/latest"

    suspend fun check(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Nexauren-File")
            }
            connection.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                val version = json.optString("tag_name").removePrefix("v")
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        if (asset.optString("name").endsWith(".apk", true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }
                if (version.isBlank() || apkUrl.isNullOrBlank() ||
                    compare(version, CURRENT_VERSION) <= 0) {
                    null
                } else {
                    UpdateInfo(
                        version,
                        json.optString("name", "Nova versão"),
                        json.optString("body", ""),
                        apkUrl
                    )
                }
            }
        }
    }

    suspend fun downloadApk(context: Context, info: UpdateInfo,
                            onProgress: (Int) -> Unit): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Nexauren-File")
                }
                val total = connection.contentLengthLong
                val apk = File(context.cacheDir, "NexaurenFile-" + info.version + ".apk")
                connection.inputStream.use { input ->
                    apk.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read = 0L
                        var last = -1
                        while (true) {
                            val n = input.read(buffer)
                            if (n <= 0) break
                            output.write(buffer, 0, n)
                            read += n
                            if (total > 0) {
                                val percent = ((read * 100) / total).toInt().coerceIn(0, 100)
                                if (percent != last) {
                                    last = percent
                                    onProgress(percent)
                                }
                            }
                        }
                    }
                }
                FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    apk
                )
            }
        }

    fun install(context: Context, apkUri: Uri) {
        if (Build.VERSION.SDK_INT >= 26 &&
            !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + context.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun compare(a: String, b: String): Int {
        val pa = a.split('.', '-', '_')
        val pb = b.split('.', '-', '_')
        val size = maxOf(pa.size, pb.size)
        for (i in 0 until size) {
            val ai = pa.getOrNull(i)?.toIntOrNull() ?: 0
            val bi = pb.getOrNull(i)?.toIntOrNull() ?: 0
            if (ai != bi) return ai.compareTo(bi)
        }
        return 0
    }
}
