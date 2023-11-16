package com.example.aktibo

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException

private val PREFS_NAME = "MyPrefsFile"

class MyHelperFunctions {

    fun getImageSizeInMB(contentResolver: ContentResolver, imageUri: Uri): Double? {
        try {
            // Open an input stream from the ContentResolver
            val inputStream = contentResolver.openInputStream(imageUri)

            // Get the length of the stream (image size in bytes)
            val imageSizeBytes = inputStream?.available()?.toLong() ?: 0

            // Close the input stream
            inputStream?.close()

            // Convert bytes to megabytes
            val imageSizeMB = imageSizeBytes / (1024.0 * 1024.0)

            return imageSizeMB

        } catch (e: IOException) {
            // Handle IOException
            e.printStackTrace()
        }

        return null
    }

    fun getBitmapSizeInMB(bitmap: Bitmap): Double {
        // Calculate the size of the bitmap in bytes
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bitmapData = stream.toByteArray()
        val bitmapSizeBytes = bitmapData.size.toDouble()

        // Convert bytes to megabytes
        val bitmapSizeMB = bitmapSizeBytes / (1024.0 * 1024.0)

        return bitmapSizeMB
    }

    fun setPreferenceString(key: String, value: Boolean, context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getPreferenceString(key: String, context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key, true)
    }

    fun cancelNotificationAlarm(context: Context, NOTIFICATION_ID: Int) {
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            alarmIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        // Also, cancel any existing notifications if applicable
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun startOfDay(calendar: Calendar): Calendar {
        // Set the time to the start of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar
    }

    fun endOfDay(calendar: Calendar): Calendar {
        // Set the time to the end of the day
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        return calendar
    }
}