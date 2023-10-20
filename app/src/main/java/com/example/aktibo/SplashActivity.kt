package com.example.aktibo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()

        // Check if user is already logged-in
        val user = auth.currentUser
        val userID = user?.uid

        if (userID != null) {
            hasUserEntry(userID) { isComplete ->
                when (isComplete) {
                    true -> { // send logged-in user to MainActivity if user entry exists
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    false -> { // send logged-in user to NewUserActivity to create new entry
                        val intent = Intent(this, NewUserActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun hasUserEntry(userID: String, onComplete: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userDocumentRef: DocumentReference = db.collection("users").document(userID)

        userDocumentRef.get()
            .addOnCompleteListener { task: Task<DocumentSnapshot> ->
                if (task.isSuccessful) {
                    val snapshot = task.result

                    if (snapshot == null || !snapshot.exists()) {
                        onComplete(false)
                        return@addOnCompleteListener
                    } else {
                        onComplete(true)
                        return@addOnCompleteListener
                    }
                }
            }
    }
}