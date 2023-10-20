package com.example.aktibo

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    lateinit var bottomNavigation: BottomNavigationView
    private var doubleBackToExitPressedOnce = false

    private var alarmMgr: AlarmManager? = null
    private lateinit var alarmIntent: PendingIntent


    val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth: FirebaseAuth ->
        val user: FirebaseUser? = firebaseAuth.currentUser
        if (user == null) {
            // If user is not logged-in, redirect to Login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    };

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

        createNotificationChannel()

        val rnds = (0..10).random()
        // createNotification(rnds, "Hello", "world")

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            println("we made it here")
//            val calendar = Calendar.getInstance()
//            //10 is for how many seconds from now you want to schedule also you can create a custom instance of Callender to set on exact time
//            calendar.add(Calendar.SECOND, 10)
//            //function for Creating [Notification Channel][1]
//            createNotificationChannel()
//            //function for scheduling the notification
//            scheduleNotification(calendar, "Hello", "world 11")
//        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                "channel_id",
//                "Channel Name",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//            val notificationManager = getSystemService(NotificationManager::class.java)
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val intent = Intent(this, AlarmReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
//
//        val calendar = Calendar.getInstance()
//        calendar.set(Calendar.HOUR_OF_DAY, 6)
//        calendar.set(Calendar.MINUTE, 3)
//
//        // Set an alarm that triggers once
//        alarmManager.setExact(
//            AlarmManager.RTC_WAKEUP,
//            calendar.timeInMillis,
//            pendingIntent
//        )

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.food -> replaceFragment(FoodFragment())
                R.id.exercise -> replaceFragment(ExerciseFragment())
                R.id.moments -> replaceFragment(MomentsFragment())
                R.id.notifications -> replaceFragment(NotificationsFragment())
            }
            true
        }

        // Set the default fragment
        bottomNavigation.selectedItemId = R.id.home

    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    private fun scheduleNotification(calendar1: Calendar, title: String, text: String) {
//        println("we made it even further")
//        val intent = Intent(applicationContext, AlarmReceiver::class.java)
//        intent.putExtra("titleExtra", title)
//        intent.putExtra("textExtra", text)
//        val pendingIntent = PendingIntent.getBroadcast(
//            applicationContext,
//            1,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
//        alarmManager.setExactAndAllowWhileIdle(
//            AlarmManager.RTC_WAKEUP,
//            calendar.timeInMillis,
//            pendingIntent
//        )

        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("titleExtra", title)
        intent.putExtra("textExtra", text)
        val alarmIntent = intent.let { intent2 ->

            PendingIntent.getBroadcast(this, 0, intent2, PendingIntent.FLAG_IMMUTABLE)
        }


        // Set the alarm to start at approximately 2:00 p.m.
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 11)
        }

        // With setInexactRepeating(), you have to use one of the AlarmManager interval
        // constants--in this case, AlarmManager.INTERVAL_DAY.
        alarmMgr.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000*30,
            1000*30,
            alarmIntent
        )



        println("we made it even further 2")
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

    fun checkAndCreateUserDocument() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val firestore = FirebaseFirestore.getInstance()
        val usersCollection = firestore.collection("users")

        currentUser?.let { user ->
            val userId = user.uid

            // Check if the document exists for the current user
            usersCollection.document(userId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documentSnapshot = task.result

                        // If the document does not exist, create it
                        if (!documentSnapshot.exists()) {
                            val data = hashMapOf(
                                "notifications" to emptyList<String>(),
                                "dateJoined" to FieldValue.serverTimestamp()
                            )

                            // Check if the user is a Google user
                            if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
                                // If the user is signed in with Google, use their Google account name as the username
                                val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
                                val googleUserName = googleSignInAccount?.displayName

                                if (!googleUserName.isNullOrEmpty()) {
                                    data["username"] = googleUserName
                                } else {
                                    data["username"] =
                                        "AktiboUser" // Default username if Google name is unavailable
                                }

                                // Check if the Google user has a profile image
                                val googleUserPhotoUrl = googleSignInAccount?.photoUrl
                                if (googleUserPhotoUrl != null) {
                                    data["userImage"] = googleUserPhotoUrl.toString()
                                } else {
                                    data["userImage"] = "" // Set userImage to an empty string
                                }
                            } else {
                                // For non-Google users, set the username to "AktiboUser"
                                data["username"] = "AktiboUser"
                                data["userImage"] =
                                    "" // Set userImage to an empty string for non-Google users
                            }

                            // Create the document
                            usersCollection.document(userId).set(data)
                                .addOnSuccessListener {
                                    // Document created successfully
                                    // You can add any additional logic here
                                }
                                .addOnFailureListener { exception ->
                                    // Handle the error
                                    // You can add error handling here
                                }
                        }
                    } else {
                        // Handle the error
                        // You can add error handling here
                    }
                }
        }
    }
}