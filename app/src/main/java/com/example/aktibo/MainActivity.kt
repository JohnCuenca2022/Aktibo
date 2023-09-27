package com.example.aktibo

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    lateinit var bottomNavigation: BottomNavigationView
    private var doubleBackToExitPressedOnce = false

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

            // Check if user is signed in (non-null) and update UI accordingly.
            val currentUser = auth.currentUser
            if (currentUser == null) {
                // If user is not logged-in, redirect to Login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        checkAndCreateUserDocument()
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