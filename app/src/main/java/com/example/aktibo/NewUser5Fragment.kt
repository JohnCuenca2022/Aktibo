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

class NewUser5Fragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_new_user5, container, false)

        val sedentary: Button = view.findViewById(R.id.button_sedentary)
        sedentary.setOnClickListener{
            sharedViewModel.physicalActivityLevel = 0
            Toast.makeText(context, "Creating your account", Toast.LENGTH_SHORT).show()
            checkAndCreateUserDocument()
        }

        val light: Button = view.findViewById(R.id.button_light)
        light.setOnClickListener{
            sharedViewModel.physicalActivityLevel = 1
            Toast.makeText(context, "Creating your account", Toast.LENGTH_SHORT).show()
            checkAndCreateUserDocument()
        }

        val moderate: Button = view.findViewById(R.id.button_moderate)
        moderate.setOnClickListener{
            sharedViewModel.physicalActivityLevel = 2
            Toast.makeText(context, "Creating your account", Toast.LENGTH_SHORT).show()
            checkAndCreateUserDocument()
        }

        val active: Button = view.findViewById(R.id.button_active)
        active.setOnClickListener{
            sharedViewModel.physicalActivityLevel = 3
            Toast.makeText(context, "Creating your account", Toast.LENGTH_SHORT).show()
            checkAndCreateUserDocument()
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

                            // Check if the user is a Google user (name and image)
                            if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
                                // If the user is signed in with Google, use their Google account name as the username
                                val googleSignInAccount = context?.let {
                                    GoogleSignIn.getLastSignedInAccount(it)
                                }
                                val googleUserName = googleSignInAccount?.displayName

                                if (!googleUserName.isNullOrEmpty()) {
                                    data["username"] = googleUserName
                                } else {
                                    data["username"] = "AktiboUser" // Default username if Google name is unavailable
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
                                data["userImage"] = "" // Set userImage to an empty string for non-Google users
                            }

                            data["weight"] = sharedViewModel.weight.toDouble()
                            data["height"] = sharedViewModel.height.toDouble()
                            data["targetWeight"] = sharedViewModel.targetWeight.toDouble()
                            data["weightGoal"] = sharedViewModel.weightGoal
                            data["exerciseGoal"] = sharedViewModel.exerciseGoal
                            data["physicalActivityLevel"] = sharedViewModel.physicalActivityLevel

                            // Create the document
                            usersCollection.document(userId).set(data)
                                .addOnSuccessListener {
                                    // Document created successfully, redirect to Main Activity
                                    goToMainActivity()
                                }
                                .addOnFailureListener { exception ->
                                    // Handle the error
                                    Toast.makeText(context, "Failed to create new user", Toast.LENGTH_SHORT).show()
                                }
                        }

                    }
                    else {
                        // Handle the error
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