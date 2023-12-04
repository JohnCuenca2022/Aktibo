package com.example.aktibo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.aktibo.LoginActivity.Companion.TAG
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val message = intent.getStringExtra("textExtra").toString()
        val title = intent.getStringExtra("titleExtra").toString()
        val setTime = intent.getStringExtra("setTime").toString()

        val helper = MyHelperFunctions()
        val remindersPref = helper.getPreferenceString("RemindersPref", context)

        if (remindersPref){
            when (setTime) {
                "morning" -> {
                    val scheduleTime = getTriggerTime(9, 0)
                    createNotification(1010, title, message, context, scheduleTime, 1010)
                }
                "afternoon" -> {
                    val scheduleTime = getTriggerTime(15, 0)
                    createNotification(1011, title, message, context, scheduleTime, 1011)
                }
                "evening" -> {
                    val scheduleTime = getTriggerTime(20, 0)
                    createNotification(1012, title, message, context, scheduleTime, 1012)
                }
            }
        }

    }

    private fun getTriggerTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // If the specified time has already passed today, set it for the next day
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val calendar2 = Calendar.getInstance()
        System.out.println("Current Date and Time: " + calendar2.getTime());
        System.out.println("Notif Date and Time: " + calendar.getTime());

        return calendar.timeInMillis
    }

    private fun createNotification(id: Int, title: String, text: String, context: Context, scheduleTime: Long, requestCode: Int){
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = context.let {
            NotificationCompat.Builder(it, "aktibo")
                .setSmallIcon(R.drawable.aktibo_icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that fires when the user taps the notification.
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        }

        with(context.let { NotificationManagerCompat.from(it) }) {
            this.notify(id, builder.build())
        }

        updateNotificationLog(title, text)
        scheduleNotification(context, requestCode, scheduleTime, title, text)
    }

    private fun updateNotificationLog(title: String, text: String){
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        val db = FirebaseFirestore.getInstance()
        val documentReference = db.collection("users").document(uid.toString())

        val newElement = hashMapOf(
            "message" to "${title}\n${text}",
            "time" to Timestamp.now()
        )

        documentReference.update("notifications", FieldValue.arrayUnion(newElement))
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully updated!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating document", e)
            }
    }

    private fun scheduleNotification(context: Context, requestCode: Int, triggerTime: Long, title: String, text: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("titleExtra", title)
        intent.putExtra("textExtra", text)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}
