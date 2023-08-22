package com.example.aktibo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    public override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener);

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth: FirebaseAuth ->
        val user: FirebaseUser? = firebaseAuth.currentUser
        if (user != null) {
            // User is signed in
            // Perform actions when user is signed in
        } else {
            // User is signed out
            // Perform actions when user is signed out
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance();

        val logoutButton = findViewById<Button>(R.id.button_logout);

        logoutButton.setOnClickListener {
            auth.signOut();
        }

        // Initialize and assign variable
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set Home selected
        bottomNavigationView.selectedItemId = R.id.home

        // Perform item selected listener

        // Perform item selected listener
        bottomNavigationView.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> return@OnNavigationItemSelectedListener true
                R.id.food -> {
                    startActivity(Intent(applicationContext, FoodActivity::class.java))
                    overridePendingTransition(0, 0)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.exercise -> {
                    startActivity(Intent(applicationContext, ExerciseActivity::class.java))
                    overridePendingTransition(0, 0)
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })

    }
}
