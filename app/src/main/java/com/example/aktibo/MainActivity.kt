package com.example.aktibo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.dynamic.SupportFragmentWrapper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth: FirebaseAuth ->
        val user: FirebaseUser? = firebaseAuth.currentUser
        if (user == null) {
            // If user is not logged-in, redirect to Login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    };

    lateinit var bottomNavigation: BottomNavigationView

    public override fun onStart() {
        super.onStart()

        if (::auth.isInitialized){
            auth.addAuthStateListener(authStateListener);

            // Check if user is signed in (non-null) and update UI accordingly.
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

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null) // Optional: Add fragments to the back stack
        transaction.commit()
    }

}