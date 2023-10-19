package com.example.aktibo

import android.R.attr.data
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso


class AccountFragment : Fragment() {

    private lateinit var forceResendingToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var verificationId: String
    private lateinit var credential: PhoneAuthCredential
    private lateinit var auth: FirebaseAuth
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY, FitnessOptions.ACCESS_READ)
        .build()
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var userName: String
    private lateinit var imageURL: String

    override fun onStart() {
        super.onStart()

        //username and user image
        val usernameTextView = view?.findViewById<TextView>(R.id.username)
        val imageView = view?.findViewById<ImageView>(R.id.userProfileImage)
        getCurrentUserDetails { username, userImage ->
            if (username != null && userImage != null) {
                // Use the retrieved username and userImage
                if (usernameTextView != null) {
                    usernameTextView.text = username
                    userName = username
                }

                if (userImage != "") {
                    Picasso.get()
                        .load(userImage)
                        .placeholder(R.drawable.placeholder_image) // Optional placeholder image
                        .into(imageView)
                    imageURL = userImage
                }

            } else {
                // Handle the case where the document doesn't exist or is missing fields
                println("User details not found or incomplete.")
            }
        }

        //Re-authentication for 2FA
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.your_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        // Edit Profile
        val editProfile = view.findViewById<Button>(R.id.button_editProfile)
        editProfile.setOnClickListener{
            replaceFragmentWithAnimAndData(imageURL, userName)
        }

        // Two-Factor Authentication
        val twoFAButton = view.findViewById<Button>(R.id.button_twoFA)
        twoFAButton.setOnClickListener{
            signIn()
        }

        // Logout
        val logoutButton = view.findViewById<Button>(R.id.button_logout)

        auth = FirebaseAuth.getInstance();
        logoutButton.setOnClickListener {
            auth.signOut();
            Fitness.getConfigClient(requireActivity(),  GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions))
                .disableFit()
                .addOnSuccessListener {
                    Log.i(TAG,"Disabled Google Fit")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error disabling Google Fit", e)
                }

            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    private fun replaceFragmentWithAnimAndData(imageURL: String, username: String) {
        val fragment = EditAccountFragment()
        val args = Bundle()
        args.putString("username", username)
        args.putString("imageURL", imageURL)
        fragment.setArguments(args)

        val fragmentManager = getParentFragmentManager()
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            R.anim.slide_in_right, // Enter animation
            R.anim.slide_out_left, // Exit animation
            R.anim.slide_in_left, // Pop enter animation (for back navigation)
            R.anim.slide_out_right // Pop exit animation (for back navigation)
        )
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, LoginActivity.RC_SIGN_IN)
    }

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == LoginActivity.RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(LoginActivity.TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed
                Log.w(LoginActivity.TAG, "Google sign in failed", e)
                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(LoginActivity.TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    val userID = user?.uid

                    var phoneNumber = "";

                    val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Input your Phone Number")

                    // Set up the input
                    val input = EditText(requireContext())
                    input.inputType =
                        InputType.TYPE_CLASS_PHONE
                    builder.setView(input)

                    builder.setPositiveButton(
                        "OK"
                    ) { dialog, which ->

//                        phoneNumber = input.getText().toString()
//                        Toast.makeText(context, phoneNumber, Toast.LENGTH_SHORT).show()
//                        val phoneAuthOptions = PhoneAuthOptions.newBuilder()
//                            .setPhoneNumber(phoneNumber)
//                            .setTimeout(30L, TimeUnit.SECONDS)
//                            .setMultiFactorSession(MultiFactorSession)
//                            .setCallbacks(callbacks)
//                            .build()
//
//                        PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions)

                    }
                    builder.setNegativeButton(
                        "Cancel"
                    ) { dialog, which -> dialog.cancel() }

                    builder.show();

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(LoginActivity.TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
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
            this@AccountFragment.credential = credential
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in response to invalid requests for
            // verification, like an incorrect phone number.
            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                // ...
            } else if (e is FirebaseTooManyRequestsException) {
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
            this@AccountFragment.verificationId = verificationId
            this@AccountFragment.forceResendingToken = forceResendingToken
            // ...
        }
    }


    fun getCurrentUserDetails(callback: (username: String?, userImage: String?) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val firestore = FirebaseFirestore.getInstance()
        val usersCollection = firestore.collection("users")

        currentUser?.let { user ->
            val userId = user.uid

            // Get the user document from Firestore
            usersCollection.document(userId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documentSnapshot = task.result

                        // Check if the document exists and has the required fields
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            val username = documentSnapshot.getString("username")
                            val userImage = documentSnapshot.getString("userImage")

                            // Invoke the callback function with the retrieved fields
                            callback(username, userImage)
                        } else {
                            // Document doesn't exist or doesn't have the required fields
                            callback(null, null)
                        }
                    } else {
                        // Handle the error
                        callback(null, null)
                    }
                }
        }
    }

}