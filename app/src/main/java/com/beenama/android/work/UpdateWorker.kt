/*
 *     This file is part of "Beenama" formerly Movie DB. <https://github.com/Akar1881/MovieDB>
 *     forked from <https://notabug.org/nvb/MovieDB>
 *
 *     Copyright (C) 2024  Akar1881 <https://github.com/Akar1881>
 *
 *     Beenama is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Beenama is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with "Beenama".  If not, see <https://www.gnu.org/licenses/>.
 */

package com.beenama.android.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.beenama.android.data.Release
import com.beenama.android.helper.ReleaseNotificationHelper
import com.beenama.android.helper.UpdateUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val release = getLatestRelease()
            if (release != null) {
                val installedVersion = UpdateUtils.getInstalledVersionName(applicationContext)
                if (UpdateUtils.isNewVersionAvailable(installedVersion, release.version)) {
                    val prefs = applicationContext.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                    with(prefs.edit()) {
                        putString("release_version", release.version)
                        putString("download_url", release.downloadUrl)
                        putString("plus_download_url", release.plusDownloadUrl)
                        putString("changelog", release.changelog)
                        apply()
                    }
                    ReleaseNotificationHelper(applicationContext).showUpdateNotification(release)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("UpdateWorker", "Error checking for updates", e)
            Result.failure()
        }
    }

    private fun getLatestRelease(): Release? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.github.com/repos/Akar1881/MovieDB/releases/latest")
            .build()
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val releaseInfo = Gson().fromJson(responseBody, GithubRelease::class.java)
                val assets = releaseInfo.assets
                val downloadUrl = assets.find { it.name.endsWith(".apk") && !it.name.contains("-plus") }?.browser_download_url
                val plusDownloadUrl = assets.find { it.name.endsWith("-plus.apk") }?.browser_download_url
                if (downloadUrl != null && plusDownloadUrl != null) {
                    Release(releaseInfo.tag_name.replace("v", ""), downloadUrl, plusDownloadUrl, releaseInfo.body)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e("UpdateWorker", "Network error while fetching latest release", e)
            null
        }
    }

    private data class GithubRelease(
        val tag_name: String,
        val assets: List<Asset>,
        val body: String
    )

    private data class Asset(
        val name: String,
        val browser_download_url: String
    )
}
