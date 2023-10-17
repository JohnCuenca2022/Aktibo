package com.example.aktibo

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class NewUser4Fragment : Fragment() {

    private lateinit var sharedViewModel: NewUserSharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(NewUserSharedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_new_user4, container, false)

        val button_12days: Button = view.findViewById(R.id.button_12days)
        button_12days.setOnClickListener{
            Toast.makeText(context, "Creating your account", Toast.LENGTH_SHORT).show()
            checkAndCreateUserDocument()
        }

        val button_34days: Button = view.findViewById(R.id.button_34days)
        button_34days.setOnClickListener{


            val fragmentManager = getParentFragmentManager()
            val transaction = fragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.anim.slide_in_right, // Enter animation
                R.anim.slide_out_left, // Exit animation
                R.anim.slide_in_left, // Pop enter animation (for back navigation)
                R.anim.slide_out_right // Pop exit animation (for back navigation)
            )
            transaction.replace(R.id.fragment_container_new_user, NewUser3Fragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        val button_5days: Button = view.findViewById(R.id.button_5days)
        button_5days.setOnClickListener{

            sharedViewModel.targetWeight = sharedViewModel.weight

            val fragmentManager = getParentFragmentManager()
            val transaction = fragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.anim.slide_in_right, // Enter animation
                R.anim.slide_out_left, // Exit animation
                R.anim.slide_in_left, // Pop enter animation (for back navigation)
                R.anim.slide_out_right // Pop exit animation (for back navigation)
            )
            transaction.replace(R.id.fragment_container_new_user, NewUser4Fragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        return view
    }

    private fun checkAndCreateUserDocument() {
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
                                val googleSignInAccount = context?.let {
                                    GoogleSignIn.getLastSignedInAccount(
                                        it
                                    )
                                }
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
                                    goToMainActivity()
                                }
                                .addOnFailureListener { exception ->
                                    // Handle the error
                                    // You can add error handling here
                                    Toast.makeText(context, "Failed to create new user", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // Handle the error
                        // You can add error handling here
                    }
                }
        }
    }

    private fun goToMainActivity(){
        val intent = Intent(activity, MainActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

}