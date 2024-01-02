package com.example.aktibo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ModalBottomSheet(val documentID: String, val momentCommentCount: TextView) : BottomSheetDialogFragment() {

    private lateinit var textViewComments: TextView
    private lateinit var linearLayout: LinearLayout
    private lateinit var TextInputLayoutNewComment: TextInputLayout
    private lateinit var textInputEditTextComment: TextInputEditText
    private lateinit var textWarningTextView: TextView
    private lateinit var textWarningCardView: CardView

    var canPostComment = true

    // Sight Engine
    val apiUser = "1227574749"
    val apiSecret = "NLvWeA9iUfYBqg6rMyx6VsaJXy"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.moments_comments, container, false)

        textViewComments = view.findViewById(R.id.textViewComments)
        linearLayout = view.findViewById(R.id.commentsLinearLayout)
        TextInputLayoutNewComment = view.findViewById(R.id.TextInputLayoutNewComment)
        textInputEditTextComment = view.findViewById(R.id.textInputEditTextComment)
        textWarningTextView = view.findViewById(R.id.textWarningTextView)
        textWarningCardView =  view.findViewById(R.id.textWarningCardView)

        val db = Firebase.firestore
        val docRef = db.collection("moments").document(documentID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val commentsList = (document.get("commentsList") as? ArrayList<Map<String, Any>>).orEmpty()
                    if (commentsList.isEmpty()){
                        textViewComments.visibility = View.VISIBLE
                    }

                    val inflater = layoutInflater
                    for (data in commentsList){
                        val comment = data["comment"]
                        val userImageSrc = data["userImageSrc"]
                        val username = data["username"]

                        val itemLayout = inflater.inflate(R.layout.moment_comment_item, null)

                        val marginLayoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        marginLayoutParams.setMargins(
                            0,
                            0,
                            0,
                            resources.getDimensionPixelSize(R.dimen.bottom_margin)
                        ) // Adjust the margin as needed
                        itemLayout.layoutParams = marginLayoutParams

                        val userProfileImage = itemLayout.findViewById<ImageView>(R.id.userProfileImage)
                        if (userImageSrc != "") {
                            Picasso.get()
                                .load(userImageSrc.toString())
                                .into(userProfileImage)
                        }

                        val textViewUsername = itemLayout.findViewById<TextView>(R.id.textViewUsername)
                        textViewUsername.text = username.toString()

                        val momentComment = itemLayout.findViewById<TextView>(R.id.momentComment)
                        momentComment.text = comment.toString()

                        linearLayout.addView(itemLayout, 0)
                    }

                } else {
                    dismiss()
                    Toast.makeText(context, "Can't load comments", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                dismiss()
                Toast.makeText(context, "Can't load comments", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "get failed with ", exception)
            }

        TextInputLayoutNewComment.setEndIconOnClickListener {
            if (canPostComment){
                canPostComment = false
                Toast.makeText(context, "Verifying Comment", Toast.LENGTH_SHORT).show()

                textWarningCardView.visibility = View.GONE // hide profanity warning message
                val newComment = textInputEditTextComment.text.toString()

                if (newComment == ""){
                    Toast.makeText(context, "Write a comment", Toast.LENGTH_SHORT).show()
                    TextInputLayoutNewComment.endIconMode = TextInputLayout.END_ICON_CUSTOM
                } else {
                    CoroutineScope(Dispatchers.IO).launch{
                        val isSafeEnglish = async {
                            checkProfanity(newComment, "en", apiUser, apiSecret)
                        }.await()

                        if (!isSafeEnglish){
                            withContext(Dispatchers.Main) {
                                canPostComment = true
                            }

                            return@launch
                        }

                        val isSafeFilipino = async {
                            checkProfanity(newComment, "tl", apiUser, apiSecret)
                        }.await()

                        if (!isSafeFilipino){
                            withContext(Dispatchers.Main) {
                                canPostComment = true
                            }

                            return@launch
                        }

                        withContext(Dispatchers.Main) {
                            uploadComment(newComment, documentID) // database
                        }

                    }

                }
            }

        }

        return view
    }

    private fun postComment(newComment: String, userImage: String, username: String){
        // remove "There are no comments"
        textViewComments.visibility = View.GONE

        // clear focus
        textInputEditTextComment.setText("")
        textInputEditTextComment.clearFocus()
        TextInputLayoutNewComment.clearFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(TextInputLayoutNewComment.windowToken, 0)

        // add comment count
        val currentCommentCount = momentCommentCount.text.toString().toInt()
        val newCommentCount = currentCommentCount + 1
        momentCommentCount.text = newCommentCount.toString()

        // add comment to comment section
        val inflater = layoutInflater
        val itemLayout = inflater.inflate(R.layout.moment_comment_item, null)

        val marginLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        marginLayoutParams.setMargins(
            0,
            0,
            0,
            resources.getDimensionPixelSize(R.dimen.bottom_margin)
        ) // Adjust the margin as needed
        itemLayout.layoutParams = marginLayoutParams

        val userProfileImage = itemLayout.findViewById<ImageView>(R.id.userProfileImage)
        if (userImage != "") {
            Picasso.get()
                .load(userImage)
                .into(userProfileImage)
        }

        val textViewUsername = itemLayout.findViewById<TextView>(R.id.textViewUsername)
        textViewUsername.text = username

        val momentComment = itemLayout.findViewById<TextView>(R.id.momentComment)
        momentComment.text = newComment

        linearLayout.addView(itemLayout, 0)
    }

    private fun uploadComment(newComment: String, documentID: String){
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userID = currentUser?.uid

        val db = Firebase.firestore
        val docRef = userID?.let { db.collection("users").document(it) }
        if (docRef != null) {
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val username = document.getString("username")
                        val userImage = document.getString("userImage")

                        val data = mapOf(
                            "comment" to newComment,
                            "time" to Timestamp.now(),
                            "userImageSrc" to userImage,
                            "username" to username
                        )

                        val momentRef = db.collection("moments").document(documentID)
                        momentRef.update(
                            "commentsList", FieldValue.arrayUnion(data),
                            "comments", FieldValue.increment(1)
                        )

                        if (userImage != null) {
                            if (username != null) {
                                postComment(newComment, userImage, username) // UI
                            }
                        }

                        canPostComment = true
                    } else {
                        Toast.makeText(context, "Error posting comment", Toast.LENGTH_SHORT).show()
                        canPostComment = true
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Error posting comment", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "get failed with ", exception)
                    canPostComment = true
                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set a consistent peek height (adjust the value as needed)
        val layoutParams = (view.parent as View).layoutParams
        layoutParams.height = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
        (view.parent as View).layoutParams = layoutParams

    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }

    private suspend fun checkProfanity(text: String, lang: String, apiKey: String, apiSecret: String): Boolean {
        val isPassing = CompletableDeferred<Boolean>()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.sightengine.com/1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(SightEngineServiceText::class.java)

        val call = service.checkText(text, "standard", lang, "us,gb,fr,ph", apiKey, apiSecret)
        call.enqueue(object : Callback<SightEngineResponseText> {
            override fun onResponse(call: Call<SightEngineResponseText>, response: Response<SightEngineResponseText>) {
                if (response.isSuccessful) {
                    val sightEngineResponseText = response.body()
                    if (sightEngineResponseText != null) {
                        val status = sightEngineResponseText.status
                        val profanityMatches = sightEngineResponseText.profanity.matches

                        // Handle the profanity check results here
                        Log.e("SightEngine API Text Status", status)
                        Log.e("SightEngine API Text Profanity Matches", profanityMatches.toString())

                        if (profanityMatches.isEmpty()){
                            Log.e("SightEngine API Text Profanity", "no profanity")
                            isPassing.complete(true)
                        } else {
                            // text has flagged words
                            var warningMessage = "User Guidelines Violation.\nInappropriate words:"
                            profanityMatches.forEach { item ->
                                val reasonText = " ${item.match},"
                                warningMessage += reasonText
                                println(warningMessage)
                            }
                            warningMessage = warningMessage.removeSuffix(",")

                             textWarningTextView.text = warningMessage
                             textWarningCardView.visibility = View.VISIBLE

                            isPassing.complete(false)
                        }

                    }
                } else {
                    Log.e("SightEngine API Text", "Empty response body")
                    Toast.makeText(requireContext(), "Failed to scan text. Please try again.", Toast.LENGTH_SHORT).show()
                    isPassing.complete(false)
                }
            }

            override fun onFailure(call: Call<SightEngineResponseText>, t: Throwable) {
                Log.e("SightEngine API Text", "Network error: ${t.message}")
                Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                isPassing.complete(false)
            }
        })

        return isPassing.await()
    }
}