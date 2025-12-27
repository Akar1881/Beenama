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

package com.beenama.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.beenama.android.helper.ScheduledNotificationDatabaseHelper

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val dbHelper = ScheduledNotificationDatabaseHelper(context)
            val scheduledNotifications = dbHelper.getAllScheduledNotifications()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            for (notification in scheduledNotifications) {
                val notificationIntent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("title", notification.title)
                    putExtra("episodeName", notification.episodeName)
                    putExtra("episodeNumber", notification.episodeNumber)
                    putExtra("notificationKey", notification.notificationKey)
                    putExtra("type", notification.type)
                    putExtra("notificationId", notification.id)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notification.id.toInt(),
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notification.alarmTime, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notification.alarmTime, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notification.alarmTime, pendingIntent)
                }
            }
        }
    }
}
