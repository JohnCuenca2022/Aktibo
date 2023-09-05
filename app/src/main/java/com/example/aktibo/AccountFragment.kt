package com.example.aktibo

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso


class AccountFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY, FitnessOptions.ACCESS_READ)
        .build()

    override fun onStart() {
        super.onStart()

        //username and user image
        val usernameEditText = view?.findViewById<EditText>(R.id.username)
        val imageView = view?.findViewById<ImageView>(R.id.userProfileImage)
        getCurrentUserDetails { username, userImage ->
            if (username != null && userImage != null) {
                // Use the retrieved username and userImage
                if (usernameEditText != null) {
                    usernameEditText.setText(username)
                }

                if (userImage != "") {
                    Picasso.get()
                        .load(userImage)
                        .placeholder(R.drawable.placeholder_image) // Optional placeholder image
                        .into(imageView)
                }

            } else {
                // Handle the case where the document doesn't exist or is missing fields
                println("User details not found or incomplete.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        // Logout
        val logoutButton = view.findViewById<Button>(R.id.button_logout);

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