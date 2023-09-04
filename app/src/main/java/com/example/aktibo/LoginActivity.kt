package com.example.aktibo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    public override fun onStart() {
        super.onStart()

        // Redirect user to MainActivity if they are logged-in.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance();

        val loginButton = findViewById<Button>(R.id.button_login);

        loginButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.user_email).text.toString();
            val password = findViewById<EditText>(R.id.user_password).text.toString();

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success
                        Toast.makeText(
                            baseContext,
                            "Login Success!",
                            Toast.LENGTH_SHORT,
                        ).show()

                        //Redirect user to Main Activity
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }

        }
    }

}

