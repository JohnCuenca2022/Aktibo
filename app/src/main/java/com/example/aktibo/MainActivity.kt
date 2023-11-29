package com.example.aktibo

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    lateinit var bottomNavigation: BottomNavigationView
    private var doubleBackToExitPressedOnce = false

    private val PREFS_NAME = "MyPrefsFile"
    private val PREF_KEY_SHOW_DIALOG = "showDialog"

    val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth: FirebaseAuth ->
        val user: FirebaseUser? = firebaseAuth.currentUser
        if (user == null) {
            // If user is not logged-in, redirect to Login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    };

    @RequiresApi(Build.VERSION_CODES.O)
    public override fun onStart() {
        super.onStart()

        if (::auth.isInitialized) {
            auth.addAuthStateListener(authStateListener);

            // Check if user is signed
            val currentUser = auth.currentUser
            if (currentUser == null) {
                // If user is not logged-in, redirect to Login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.food -> replaceFragment(FoodFragment())
                R.id.exercise -> replaceFragment(ExerciseFragment())
                R.id.moments -> {
                    // Check if the dialog should be shown
                    if (shouldShowDialog()) {
                        replaceFragment(TermsOfServiceFragment())
                    } else {
                        replaceFragment(MomentsFragment())
                    }
                }
                R.id.notifications -> replaceFragment(NotificationsFragment())
            }
            true
        }

        // Set the default fragment
        bottomNavigation.selectedItemId = R.id.home

        //schedule notifications
        createNotificationChannel()

        scheduleNotification(getTriggerTime(9, 0),
            "Good Morning!", "Remember to record your morning meal.", "morning")
        scheduleNotification(getTriggerTime(15, 0),
            "Good Afternoon!", "Remember to record your afternoon meal.", "afternoon")
        scheduleNotification(getTriggerTime(20, 0),
            "Good Evening!", "Remember to record your evening meal.", "evening")

    }

    fun navigationInterface(navItem: Int){
        when (navItem) {
            1 -> bottomNavigation.selectedItemId = R.id.home
            2 -> bottomNavigation.selectedItemId = R.id.food
            3 -> bottomNavigation.selectedItemId = R.id.exercise
            4 -> bottomNavigation.selectedItemId = R.id.moments
            5 -> bottomNavigation.selectedItemId = R.id.notifications
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("aktibo", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(id: Int, title: String, text: String){
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        var builder = NotificationCompat.Builder(this, "aktibo")
            .setSmallIcon(R.drawable.aktibo_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that fires when the user taps the notification.
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)


        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define.
            notify(id, builder.build())
        }
    }

    private fun scheduleNotification(triggerTime: Long, title: String, text: String, setTime:String) {
        val intent = Intent(applicationContext, AlarmReceiver::class.java)
        intent.putExtra("titleExtra", title)
        intent.putExtra("textExtra", text)
        intent.putExtra("setTime", setTime)
        val rnds = (0..7777).random()
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            rnds,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
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

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager

        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager

        // if current fragment is one of the main fragments except home, return home
        val currentFragment = fragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is FoodFragment ||
            currentFragment is ExerciseFragment ||
            currentFragment is MomentsFragment ||
            currentFragment is NotificationsFragment) {

            replaceFragment(HomeFragment())
            bottomNavigation.selectedItemId = R.id.home

            return
        }

        // if current fragment is home, user can tap back twice to exit
        if (currentFragment is HomeFragment){
            if (doubleBackToExitPressedOnce) {
                finish()
                return
            }

            this.doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Tap again to exit", Toast.LENGTH_SHORT).show()

            // reset double tap timer when it has exceeded 2 seconds
            Handler(Looper.getMainLooper()).postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
            return
        }

        super.onBackPressed()
    }

    // Terms of Service
    private fun shouldShowDialog(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_SHOW_DIALOG, true)
    }

    private fun resetPreferences() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.clear() // This will remove all preferences
        editor.apply()
    }

}