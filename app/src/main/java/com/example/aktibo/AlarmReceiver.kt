package com.example.aktibo

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Code to send your notification here
        // You can use NotificationCompat.Builder to create the notification

        val message = intent?.getStringExtra("textExtra").toString()
        println("message")
        println(message)
        val title = intent?.getStringExtra("titleExtra").toString()
//        val notification =
//            context?.let {
//                NotificationCompat.Builder(it, "aktibo").setSmallIcon(R.drawable.aktibo_icon)
//                    .setContentText(message).setContentTitle(title).build()
//            }
//        val manager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        manager.notify(1, notification)
        val rnds = (0..7777).random()
        createNotification(rnds,title,message,context,intent)

        println("we made it even further than ever")
    }

    private fun createNotification(id: Int, title: String, text: String, context: Context?, intent: Intent?){
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        var builder = context?.let {
            NotificationCompat.Builder(it, "aktibo")
                .setSmallIcon(R.drawable.aktibo_icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that fires when the user taps the notification.
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        }


        if (builder != null) {
            with(context?.let { NotificationManagerCompat.from(it) }) {
                // notificationId is a unique int for each notification that you must define.
                this?.notify(id, builder.build())
            }
        }
    }
}
