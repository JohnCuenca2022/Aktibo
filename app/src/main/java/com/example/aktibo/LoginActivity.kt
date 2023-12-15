package com.example.aktibo

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.MultiFactorAssertion
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
//    private val REQ_ONE_TAP = 2  // Can be any integer unique to the Activity
//    private var showOneTapUI = true

    private lateinit var forceResendingToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var verificationId: String
    private lateinit var credential: PhoneAuthCredential
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    public override fun onStart() {
        super.onStart()

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
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.your_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance();

        val googleButton = findViewById<Button>(R.id.button_google)
        googleButton.setOnClickListener {
            signIn()
        }

    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    companion object {
        const val TAG = "GoogleActivity"
        const val RC_SIGN_IN = 9001
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                logout()
                // Google Sign In failed
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign in success
                Log.d(TAG, "signInWithCredential:success")
                val user = auth.currentUser ?: return@addOnCompleteListener
                val userID = user.uid

                hasUserEntry(userID) { isComplete ->
                    when (isComplete) {
                        true -> {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }

                        false -> {
                            val intent = Intent(this, NewUserActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
            else if (task.exception is FirebaseAuthMultiFactorException) {
                // The user is a multi-factor user. Second factor challenge is required.

                val multiFactorResolver = (task.exception as FirebaseAuthMultiFactorException).resolver
                val items = mutableListOf<String>()

                for ((index, _) in multiFactorResolver.hints.withIndex()) {
                    val selectedHint = multiFactorResolver.hints[index] as PhoneMultiFactorInfo
                    items.add(selectedHint.phoneNumber)
                }

                val itemsArray = items.toTypedArray()

                // Select phone number
                MaterialAlertDialogBuilder(this)
                    .setTitle("Choose a phone number")
                    .setItems(itemsArray) { dialog, which ->
                        Toast.makeText(this, "Sending OTP code. Please wait.", Toast.LENGTH_SHORT).show()
                        val selectedHint = multiFactorResolver.hints[which] as PhoneMultiFactorInfo
                        val phoneAuthOptions = PhoneAuthOptions.newBuilder()
                            .setActivity(this)
                            .setMultiFactorHint(selectedHint)
                            .setTimeout(30L, TimeUnit.SECONDS)
                            .setMultiFactorSession(multiFactorResolver.session)
                            .setCallbacks(callbacks) // Optionally disable instant verification.
                            // .requireSmsValidation(true)
                            .build()

                            PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions)

                            // input OTP
                            val inputField = EditText(this)
                            inputField.layoutParams = ViewGroup.LayoutParams(
                                300, // Set the width to MATCH_PARENT
                                ViewGroup.LayoutParams.MATCH_PARENT // Use WRAP_CONTENT for height
                            )
                            inputField.gravity = Gravity.CENTER
                            inputField.inputType = InputType.TYPE_CLASS_NUMBER

                            val layout = LinearLayout(this)
                            layout.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, // Set the width to MATCH_PARENT
                                LinearLayout.LayoutParams.WRAP_CONTENT // Use WRAP_CONTENT for height
                            )
                            layout.gravity = Gravity.CENTER // center content
                            layout.addView(inputField)

                            // input OTP Code
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Please enter your OTP code")
                                .setView(layout)
                                .setPositiveButton("OK") { dialog, _ ->
                                    val verificationCode = inputField.text.toString().trim()

                                    if (!this::verificationId.isInitialized){
                                        Toast.makeText(this, "Invalid OTP code.", Toast.LENGTH_SHORT).show()
                                        return@setPositiveButton
                                    }
                                    val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)

                                    val multiFactorAssertion: MultiFactorAssertion =
                                        PhoneMultiFactorGenerator.getAssertion(credential)

                                    // Complete sign-in.
                                    multiFactorResolver
                                        .resolveSignIn(multiFactorAssertion)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                // User successfully signed in with the
                                                // second factor phone number.

                                                val user = auth.currentUser ?: return@addOnCompleteListener

                                                user.multiFactor.session.addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {

                                                        Log.d(TAG, "signInWithCredential:success")
                                                        val user = auth.currentUser ?: return@addOnCompleteListener
                                                        val userID = user.uid

                                                        hasUserEntry(userID) { isComplete ->
                                                            when (isComplete) {
                                                                true -> {
                                                                    val intent = Intent(this, MainActivity::class.java)
                                                                    startActivity(intent)
                                                                    finish()
                                                                }

                                                                false -> {
                                                                    val intent = Intent(this, NewUserActivity::class.java)
                                                                    startActivity(intent)
                                                                    finish()
                                                                }
                                                            }
                                                        }

                                                    } else {
                                                        logout()
                                                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                                                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                logout()
                                                Log.w(TAG, "signInWithCredential:failure", task.exception)
                                                Toast.makeText(this, "Invalid OTP Code.", Toast.LENGTH_SHORT).show()
                                            }

                                        }

                                    dialog.dismiss()
                                }
                                .setOnCancelListener() {
                                    logout()
                                }
                                .show()
                        }
                    .setOnCancelListener() {
                        logout()
                    }
                    .show()

                }
                else {
                    logout()
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1) Instant verification. In some cases, the phone number can be
            //    instantly verified without needing to send or enter a verification
            //    code. You can disable this feature by calling
            //    PhoneAuthOptions.builder#requireSmsValidation(true) when building
            //    the options to pass to PhoneAuthProvider#verifyPhoneNumber().
            // 2) Auto-retrieval. On some devices, Google Play services can
            //    automatically detect the incoming verification SMS and perform
            //    verification without user action.
            this@LoginActivity.credential = credential
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in response to invalid requests for
            // verification, like an incorrect phone number.
            if (e is FirebaseAuthInvalidCredentialsException) {
                Toast.makeText(this@LoginActivity, "Invalid Phone Number", Toast.LENGTH_SHORT).show()
                // Invalid request
                // ...
            } else if (e is FirebaseTooManyRequestsException) {
                Toast.makeText(this@LoginActivity, "Too Many Requests", Toast.LENGTH_SHORT).show()
                // The SMS quota for the project has been exceeded
                // ...
            }
            // Show a message and update the UI
            // ...
        }

        override fun onCodeSent(
            verificationId: String, forceResendingToken: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number.
            // We now need to ask the user to enter the code and then construct a
            // credential by combining the code with a verification ID.
            // Save the verification ID and resending token for later use.

            this@LoginActivity.verificationId = verificationId
            this@LoginActivity.forceResendingToken = forceResendingToken
            // ...
        }
    }

    private fun logout(){
        try {
            Firebase.auth.signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut()
            println("logout success")
        } catch(e: Exception) {
            println("logout failed")
            println(e)
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

