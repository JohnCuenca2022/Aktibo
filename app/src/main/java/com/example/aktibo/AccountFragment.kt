package com.example.aktibo

import android.R.attr.data
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.MultiFactor
import com.google.firebase.auth.MultiFactorAssertion
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import java.util.concurrent.TimeUnit


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

    @RequiresApi(Build.VERSION_CODES.O)
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
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.TFAtitle))
                .setMessage(resources.getString(R.string.TFAsupporting_text))
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                    // Respond to neutral button press
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    // Respond to positive button press
                    signIn()
                }
                .show()

        }

        // Settings
        val helper = MyHelperFunctions()
        val remindersPref = helper.getPreferenceString("RemindersPref", requireContext())
        val NotifsPref = helper.getPreferenceString("NotifsPref", requireContext())
        Log.e("remindersPref", remindersPref.toString())
        Log.e("NotifsPref", NotifsPref.toString())

        val switchReminders: SwitchCompat = view.findViewById(R.id.switchReminders)
        switchReminders.isChecked = remindersPref

        switchReminders.setOnCheckedChangeListener { buttonView, isChecked ->
            val helper = MyHelperFunctions()
            val key = "RemindersPref"
            if (isChecked) {
                helper.setPreferenceString(key, true, requireContext())
                createNotificationChannel()
                scheduleNotification(getTriggerTime(9, 0),
                    "Good Morning!", "Remember to record your morning meal.", "morning")
                scheduleNotification(getTriggerTime(15, 0),
                    "Good Afternoon!", "Remember to record your afternoon meal.", "afternoon")
                scheduleNotification(getTriggerTime(20, 0),
                    "Good Evening!", "Remember to record your evening meal.", "evening")
            } else {
                // Switch is unchecked
                helper.setPreferenceString(key, false, requireContext())
                helper.cancelNotificationAlarm(requireContext(), 1010)
                helper.cancelNotificationAlarm(requireContext(), 1011)
                helper.cancelNotificationAlarm(requireContext(), 1012)
            }
        }

        val switchNotifs: SwitchCompat = view.findViewById(R.id.switchNotifs)
        switchNotifs.isChecked = NotifsPref

        switchNotifs.setOnCheckedChangeListener { buttonView, isChecked ->
            val helper = MyHelperFunctions()
            val key = "NotifsPref"
            if (isChecked) {
                helper.setPreferenceString(key, true, requireContext())
            } else {
                // Switch is unchecked
                helper.setPreferenceString(key, false, requireContext())
                helper.cancelNotificationAlarm(requireContext(), 2020)
            }
        }

        // Logout
        val logoutButton = view.findViewById<Button>(R.id.button_logout)

        auth = FirebaseAuth.getInstance();
        logoutButton.setOnClickListener {
            auth.signOut();
            Firebase.auth.signOut()
            Fitness.getConfigClient(requireActivity(),  GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions))
                .disableFit()
                .addOnSuccessListener {
                    Log.i(TAG,"Disabled Google Fit")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG,"There was an error disabling Google Fit", e)
                }
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInClient.signOut().addOnCompleteListener(requireActivity()) { signOutTask ->
                if (signOutTask.isSuccessful) {
                    val intent = Intent(requireActivity(), LoginActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    // Sign out failed
                }
            }

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

                    if (user != null) {
                        user.multiFactor.session.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val multiFactorSession: MultiFactorSession = task.result

                                val textView = TextView(requireContext())
                                val inputField = EditText(requireContext())

                                textView.setText("+63")
                                textView.setTextSize(20F)
                                textView.layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                textView.gravity = Gravity.CENTER

                                inputField.layoutParams = ViewGroup.LayoutParams(
                                    300, // Set the width to MATCH_PARENT
                                    ViewGroup.LayoutParams.MATCH_PARENT // Use WRAP_CONTENT for height
                                )

                                inputField.inputType = InputType.TYPE_CLASS_PHONE

                                val layout = LinearLayout(context)
                                layout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, // Set the width to MATCH_PARENT
                                    LinearLayout.LayoutParams.WRAP_CONTENT // Use WRAP_CONTENT for height
                                )
                                layout.gravity = Gravity.CENTER // center content
                                layout.addView(textView)
                                layout.addView(inputField)

                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Enter your Phone Number")
                                    .setView(layout)
                                    .setPositiveButton("OK") { dialog, _ ->
                                        val userInput = inputField.text.toString().trim()
                                        val regex = Regex("^0+(?!$)") // remove leading zeros ex. 09215427766 -> 9215427766
                                        userInput.replace(regex, "")

                                        //send OTP
                                        val phoneNumber = "+63${userInput}";
                                        val phoneAuthOptions = PhoneAuthOptions.newBuilder()
                                            .setActivity(requireActivity())
                                        .setPhoneNumber(phoneNumber)
                                        .setTimeout(30L, TimeUnit.SECONDS)
                                        .setMultiFactorSession(multiFactorSession)
                                        .setCallbacks(callbacks)
                                        .build()

                                        PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions)

                                        dialog.dismiss()

                                        // Input OTP code
                                        val inputField = EditText(requireContext())

                                        inputField.layoutParams = ViewGroup.LayoutParams(
                                            300, // Set the width to MATCH_PARENT
                                            ViewGroup.LayoutParams.MATCH_PARENT // Use WRAP_CONTENT for height
                                        )

                                        inputField.gravity = Gravity.CENTER
                                        inputField.inputType = InputType.TYPE_CLASS_NUMBER

                                        val layout = LinearLayout(context)
                                        layout.layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT, // Set the width to MATCH_PARENT
                                            LinearLayout.LayoutParams.WRAP_CONTENT // Use WRAP_CONTENT for height
                                        )
                                        layout.gravity = Gravity.CENTER // center content

                                        layout.addView(inputField)

                                        MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Please enter your OTP code")
                                            .setView(layout)
                                            .setPositiveButton("OK") { dialog, _ ->
                                                val verificationCode = inputField.text.toString().trim()
                                                val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)

                                                val multiFactorAssertion
                                                        = PhoneMultiFactorGenerator.getAssertion(credential)

                                                FirebaseAuth.getInstance()
                                                    .currentUser
                                                    ?.multiFactor
                                                    ?.enroll(multiFactorAssertion, "My personal phone number")
                                                    ?.addOnCompleteListener {
                                                        Toast.makeText(context, "Phone number successfully saved.", Toast.LENGTH_SHORT).show()
                                                    }


                                                dialog.dismiss()
                                            }
                                            .show()
                                    }
                                    .setNegativeButton("Cancel") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .show()

                            }
                        }
                    }

                }
                else if (task.exception is FirebaseAuthMultiFactorException) {
                    // The user is a multi-factor user. Second factor challenge is required.
                    val multiFactorResolver = (task.exception as FirebaseAuthMultiFactorException).resolver

                    val items = mutableListOf<String>()

                    for ((index, item) in multiFactorResolver.hints.withIndex()) {
                        val selectedHint = multiFactorResolver.hints[index] as PhoneMultiFactorInfo
                        items.add(selectedHint.phoneNumber)
                    }

                    val itemsArray = items.toTypedArray()

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Choose a phone number")
                        .setItems(itemsArray) { dialog, which ->

                            val selectedHint = multiFactorResolver.hints[which] as PhoneMultiFactorInfo
                            val phoneAuthOptions = PhoneAuthOptions.newBuilder()
                                .setActivity(requireActivity())
                                .setMultiFactorHint(selectedHint)
                                .setTimeout(30L, TimeUnit.SECONDS)
                                .setMultiFactorSession(multiFactorResolver.session)
                                .setCallbacks(callbacks) // Optionally disable instant verification.
                                // .requireSmsValidation(true)
                                .build()
                            PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions)

                            // input OTP
                            val inputField = EditText(requireContext())

                            inputField.layoutParams = ViewGroup.LayoutParams(
                                300, // Set the width to MATCH_PARENT
                                ViewGroup.LayoutParams.MATCH_PARENT // Use WRAP_CONTENT for height
                            )

                            inputField.gravity = Gravity.CENTER
                            inputField.inputType = InputType.TYPE_CLASS_NUMBER

                            val layout = LinearLayout(context)
                            layout.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, // Set the width to MATCH_PARENT
                                LinearLayout.LayoutParams.WRAP_CONTENT // Use WRAP_CONTENT for height
                            )
                            layout.gravity = Gravity.CENTER // center content

                            layout.addView(inputField)

                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Please enter your OTP code")
                                .setView(layout)
                                .setPositiveButton("OK") { dialog, _ ->
                                    val verificationCode = inputField.text.toString().trim()
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

                                                val user = auth.currentUser

                                                if (user != null) {
                                                    user.multiFactor.session.addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            val multiFactorSession: MultiFactorSession = task.result

                                                            val textView = TextView(requireContext())
                                                            val inputField = EditText(requireContext())

                                                            textView.setText("+63")
                                                            textView.setTextSize(20F)
                                                            textView.layoutParams = ViewGroup.LayoutParams(
                                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                ViewGroup.LayoutParams.MATCH_PARENT
                                                            )
                                                            textView.gravity = Gravity.CENTER

                                                            inputField.layoutParams = ViewGroup.LayoutParams(
                                                                300, // Set the width to MATCH_PARENT
                                                                ViewGroup.LayoutParams.MATCH_PARENT // Use WRAP_CONTENT for height
                                                            )

                                                            inputField.inputType = InputType.TYPE_CLASS_PHONE

                                                            val layout = LinearLayout(context)
                                                            layout.layoutParams = LinearLayout.LayoutParams(
                                                                LinearLayout.LayoutParams.MATCH_PARENT, // Set the width to MATCH_PARENT
                                                                LinearLayout.LayoutParams.WRAP_CONTENT // Use WRAP_CONTENT for height
                                                            )
                                                            layout.gravity = Gravity.CENTER // center content
                                                            layout.addView(textView)
                                                            layout.addView(inputField)

                                                            MaterialAlertDialogBuilder(requireContext())
                                                                .setTitle("Enter your Phone Number")
                                                                .setView(layout)
                                                                .setPositiveButton("OK") { dialog, _ ->
                                                                    val userInput = inputField.text.toString().trim()
                                                                    val regex = Regex("^0+(?!$)") // remove leading zeros ex. 09215427766 -> 9215427766
                                                                    userInput.replace(regex, "")

                                                                    //send OTP
                                                                    val phoneNumber = "+63${userInput}";
                                                                    val phoneAuthOptions = PhoneAuthOptions.newBuilder()
                                                                        .setActivity(requireActivity())
                                                                        .setPhoneNumber(phoneNumber)
                                                                        .setTimeout(30L, TimeUnit.SECONDS)
                                                                        .setMultiFactorSession(multiFactorSession)
                                                                        .setCallbacks(callbacks)
                                                                        .build()

                                                                    PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions)

                                                                    dialog.dismiss()

                                                                    // Input OTP code
                                                                    val inputField = EditText(requireContext())

                                                                    inputField.layoutParams = ViewGroup.LayoutParams(
                                                                        300, // Set the width to MATCH_PARENT
                                                                        ViewGroup.LayoutParams.MATCH_PARENT // Use WRAP_CONTENT for height
                                                                    )

                                                                    inputField.gravity = Gravity.CENTER
                                                                    inputField.inputType = InputType.TYPE_CLASS_NUMBER

                                                                    val layout = LinearLayout(context)
                                                                    layout.layoutParams = LinearLayout.LayoutParams(
                                                                        LinearLayout.LayoutParams.MATCH_PARENT, // Set the width to MATCH_PARENT
                                                                        LinearLayout.LayoutParams.WRAP_CONTENT // Use WRAP_CONTENT for height
                                                                    )
                                                                    layout.gravity = Gravity.CENTER // center content

                                                                    layout.addView(inputField)

                                                                    MaterialAlertDialogBuilder(requireContext())
                                                                        .setTitle("Please enter your OTP code")
                                                                        .setView(layout)
                                                                        .setPositiveButton("OK") { dialog, _ ->
                                                                            val verificationCode = inputField.text.toString().trim()
                                                                            val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)

                                                                            val multiFactorAssertion
                                                                                    = PhoneMultiFactorGenerator.getAssertion(credential)

                                                                            FirebaseAuth.getInstance()
                                                                                .currentUser
                                                                                ?.multiFactor
                                                                                ?.enroll(multiFactorAssertion, "My personal phone number")
                                                                                ?.addOnCompleteListener {
                                                                                    Toast.makeText(context, "Phone number successfully saved.", Toast.LENGTH_SHORT).show()
                                                                                }


                                                                            dialog.dismiss()
                                                                        }
                                                                        .show()
                                                                }
                                                                .setNegativeButton("Cancel") { dialog, _ ->
                                                                    dialog.dismiss()
                                                                }
                                                                .show()

                                                        }
                                                    }
                                                }
                                            }

                                        }

                                    dialog.dismiss()
                                }
                                .show()
                        }
                        .show()

                }
                else {
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
                Toast.makeText(context, "Invalid Phone Number", Toast.LENGTH_SHORT).show()
                // Invalid request
                // ...
            } else if (e is FirebaseTooManyRequestsException) {
                Toast.makeText(context, "Too Many Requests", Toast.LENGTH_SHORT).show()
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
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleNotification(triggerTime: Long, title: String, text: String, setTime:String) {
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        intent.putExtra("titleExtra", title)
        intent.putExtra("textExtra", text)
        intent.putExtra("setTime", setTime)

        var notificationID = 0

        notificationID = when (setTime) {
            "morning" -> 1010
            "afternoon" -> 1011
            "evening" -> 1012
            else -> 2020
        }

        // val rnds = (0..7777).random()
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = requireContext().getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    private fun getTriggerTime(hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }

        // If the specified time has already passed today, set it for the next day
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        val calendar2 = java.util.Calendar.getInstance()
        System.out.println("Current Date and Time: " + calendar2.getTime());
        System.out.println("Notif Date and Time: " + calendar.getTime());

        return calendar.timeInMillis
    }

}