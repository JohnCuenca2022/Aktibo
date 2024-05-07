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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    lateinit var bottomNavigation: BottomNavigationView
    private var doubleBackToExitPressedOnce = false

    private val PREFS_NAME = "MyPrefsFile"
    private val PREF_KEY_SHOW_DIALOG = "showDialog"
    private val PREF_KEY_SHOW_DATA_PRIVACY_DIALOG = "showDataPrivacyDialog"

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

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val helper = MyHelperFunctions()
        val NightModeInt = helper.getPreferenceInt("NightModeInt", this)

        if (NightModeInt == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

//        val sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE)
//        val NightMode = sharedPreferences.getInt("NightModeInt", 1);
//        AppCompatDelegate.setDefaultNightMode(NightMode)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> replaceFragment(HomeFragment())
                R.id.menu_food -> replaceFragment(FoodFragment())
                R.id.menu_exercise -> replaceFragment(ExerciseFragment())
                R.id.menu_moments -> {
                    //replaceFragment(MomentsFragment())
                    // Check if user is restricted
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val userID = currentUser?.uid
                    val db = Firebase.firestore
                    val docRef = userID?.let { db.collection("users").document(it) }
                    if (docRef != null) {
                        docRef.get()
                            .addOnSuccessListener { document ->
                                if (document != null) {
                                    var reportsCount = document.getDouble("reportsCount") ?: 0
                                    reportsCount = reportsCount.toInt()

                                    if (reportsCount >= 3){
                                        val builder = AlertDialog.Builder(this)
                                        builder.setTitle("Feature Restricted")
                                        builder.setMessage(R.string.banned_message)
                                        builder.setPositiveButton("I understand") { dialog, which ->

                                        }
                                        val dialog = builder.create()
                                        dialog.show()
                                    } else {
                                        // Check if the dialog should be shown
                                        if (shouldShowDialog()) {
                                            replaceFragment(TermsOfServiceFragment())
                                        } else {
                                            replaceFragment(MomentsFragment())
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                // Check if the dialog should be shown
                                if (shouldShowDialog()) {
                                    replaceFragment(TermsOfServiceFragment())
                                } else {
                                    replaceFragment(MomentsFragment())
                                }
                            }
                    }

                }
                R.id.menu_notifications -> replaceFragment(NotificationsFragment())
            }
            true
        }
        // Set the default fragment
        //shouldShowDataPrivacyDialog()
        if(shouldShowDataPrivacyDialog()){
            bottomNavigation.menu.forEach { it.isEnabled = false }
            replaceFragment(DataPrivacyFragment())
        } else {
            bottomNavigation.menu.forEach { it.isEnabled = true }
            bottomNavigation.selectedItemId = R.id.menu_home
        }


    }

    fun navigationInterface(navItem: Int){
        when (navItem) {
            1 -> bottomNavigation.selectedItemId = R.id.menu_home
            2 -> bottomNavigation.selectedItemId = R.id.menu_food
            3 -> bottomNavigation.selectedItemId = R.id.menu_exercise
            4 -> bottomNavigation.selectedItemId = R.id.menu_moments
            5 -> bottomNavigation.selectedItemId = R.id.menu_notifications
        }
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
            bottomNavigation.selectedItemId = R.id.menu_home

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

    private fun shouldShowDataPrivacyDialog(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_SHOW_DATA_PRIVACY_DIALOG, true)
    }

}