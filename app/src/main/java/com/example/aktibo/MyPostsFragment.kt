package com.example.aktibo

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MyPostsFragment : Fragment() {

    private lateinit var scrollView: ScrollView
    private lateinit var linearLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userID = currentUser?.uid

    var canLoadMoreMoments = true
    var canShowEndOfMomentsMessage = true

    val db = Firebase.firestore
    val momentsRef = db.collection("moments")

    val query = momentsRef
        // don't show moments(posts) with more than 5 reports
        //.whereLessThan("reportsCount", 5)
        .whereEqualTo("userID", userID)
        .orderBy("reportsCount", Query.Direction.ASCENDING)
        .orderBy("datePosted", Query.Direction.DESCENDING)

    private lateinit var lastVisible: DocumentSnapshot

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_posts, container, false)

        //println("USERID: " + userID)

        scrollView = view.findViewById(R.id.scrollView)
        linearLayout = view.findViewById(R.id.scrollContainer)
        progressBar = view.findViewById(R.id.progressBar)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // first moments
        val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels
        val numOfPosts = (screenHeight / 200).toInt().toLong()

        val dataList = listOf("Item 1", "Item 2", "Item 3") // Example data

        firstContent(numOfPosts)

        // add more content when user has scrolled to bottom
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (isAtBottom(scrollView) && canLoadMoreMoments) {
                // Show loading ProgressBar
                progressBar.visibility = View.VISIBLE

                addNewContent()
            }
        }

    }

    private fun isAtBottom(scrollView: ScrollView): Boolean {
        val scrollY = scrollView.scrollY
        val height = scrollView.height
        val scrollViewChild = scrollView.getChildAt(0)
        return scrollY + height >= scrollViewChild.height
    }

    private fun firstContent(numOfPosts: Long){
        query
            .limit(numOfPosts)
            .get()
            .addOnSuccessListener { documentSnapshots ->

                showMoments(documentSnapshots)

                if (documentSnapshots.size() - 1 < 0){
                    if (canShowEndOfMomentsMessage){
                        canShowEndOfMomentsMessage = false
                        Toast.makeText(requireContext(), "No more recent moments", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    lastVisible = documentSnapshots.documents[documentSnapshots.size() - 1]
                }

            }
    }

    private fun addNewContent() {
        if (!::lastVisible.isInitialized){
            return
        }

        if (!canLoadMoreMoments) {
            return
        }

        canLoadMoreMoments = false

        val query = query
            .startAfter(lastVisible)

        query.get()
            .addOnSuccessListener { documentSnapshots ->

                showMoments(documentSnapshots)

                canLoadMoreMoments = true
                progressBar.visibility = View.GONE

                if (documentSnapshots.size() - 1 < 0){
                    if (canShowEndOfMomentsMessage){
                        canShowEndOfMomentsMessage = false
                        Toast.makeText(requireContext(), "No more recent moments", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    lastVisible = documentSnapshots.documents[documentSnapshots.size() - 1]
                }
            }
            .addOnFailureListener {

                canLoadMoreMoments = true
                progressBar.visibility = View.GONE
            }
    }

    private fun showMoments(documentSnapshots: QuerySnapshot){
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userID = currentUser?.uid

        val inflater = layoutInflater

        if (documentSnapshots.size() < 1){
            Toast.makeText(context, "There are no more posts", Toast.LENGTH_SHORT).show()
        }

        for (data in documentSnapshots){
            val id = data.id
            val userImageSrc = data.getString("userImageSrc")
            val username = data.getString("username")
            val caption = data.getString("caption")
            val imageSrc = data.getString("imageSrc")
            val likes = data.getDouble("likes")?.toInt()
            val comments = data.getDouble("comments")?.toInt()
            val reports = data.get("reports") as? ArrayList<Map<Any, Any>> ?: ArrayList()
            val usersLiked = data.get("usersLiked") as? List<String>
            val usersDisliked = data.get("usersDisliked") as? List<String>
            val isDeleted = data.getBoolean("isDeleted") ?: false

            if (isDeleted){
                continue
            }

            println(data.toString())

            var skip = false
            for (reportData in reports){
                val reportUserID = reportData["userID"].toString()
                if (userID == reportUserID){
                    skip = true
                }
            }

            if (skip){ // don't show moment if user has reported it
                continue
            }

            val itemLayout = inflater.inflate(R.layout.moments_item, null)

            itemLayout.setTag(id)

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
            if (userImageSrc != ""){
                Picasso.get()
                    .load(userImageSrc)
                    .into(userProfileImage)
            }

            val userName = itemLayout.findViewById<TextView>(R.id.username)
            userName.text = username

            val momentCaption = itemLayout.findViewById<TextView>(R.id.momentCaption)
            if (caption == ""){
                momentCaption.visibility = View.GONE
            } else {
                momentCaption.text = caption
            }

            val imageView = itemLayout.findViewById<ImageView>(R.id.momentImg)
            if (imageSrc == "") {
                imageView.visibility = ImageView.GONE // Set visibility to "gone"
            } else {
                Picasso.get()
                    .load(imageSrc)
                    .into(imageView)
            }

            val momentLikeCount = itemLayout.findViewById<TextView>(R.id.momentLikeCount)
            momentLikeCount.text = likes.toString()


            val momentLikeButton = itemLayout.findViewById<ImageView>(R.id.momentLikeButton)
            val momentDislikeButton = itemLayout.findViewById<ImageView>(R.id.momentDislikeButton)

            // upvote moment
            if (usersLiked != null){
                if (usersLiked.contains(userID)) {
                    momentLikeButton.setImageResource(R.drawable.upvote_filled)
                }
            }
            momentLikeButton.setOnClickListener{
                if (userID != null) {
                    interactWithLike(data.id, userID, momentLikeCount, momentLikeButton, momentDislikeButton)
                }
            }

            // downvote moment
            if (usersDisliked != null){
                if (usersDisliked.contains(userID)) {
                    momentDislikeButton.setImageResource(R.drawable.downvote_filled)
                }
            }
            momentDislikeButton.setOnClickListener{
                if (userID != null) {
                    interactWithDislike(data.id, userID, momentLikeCount, momentDislikeButton, momentLikeButton)
                }
            }

            // comments
            val momentCommentCount = itemLayout.findViewById<TextView>(R.id.momentCommentCount)
            momentCommentCount.text = comments.toString()

            // comment on moment
            val momentCommentButton = itemLayout.findViewById<ImageView>(R.id.momentCommentButton)
            momentCommentButton.setOnClickListener{
                val modalBottomSheet = ModalBottomSheet(data.id, momentCommentCount)

                modalBottomSheet.show(parentFragmentManager, ModalBottomSheet.TAG)

            }

            // more button(vertical ellipsis)
            val moreButton = itemLayout.findViewById<ImageButton>(R.id.moreButton)
            moreButton.setOnClickListener{
                showPopupMenu(moreButton, itemLayout, id)
            }

            linearLayout.addView(itemLayout)
        }
    }

    private fun interactWithLike(momentDocID: String, userID:String, likesCount: TextView, likeButton: ImageView, otherButton: ImageView){
        // check if dislike button is filled
        if (otherButton.drawable.constantState != resources.getDrawable(R.drawable.downvote, null).constantState){
            otherButton.setImageResource(R.drawable.downvote) // change like icon state
            val currentCount = likesCount.text.toString().toInt()
            val newCount = (currentCount + 1).toString()
            likesCount.text = newCount // change like count
            updateDislikeCount(momentDocID, userID, (-1).toDouble()) // take away 1 dislike
        }

        // check if like icon is NOT filled
        if (likeButton.drawable.constantState ==
            resources.getDrawable(R.drawable.upvote, null).constantState) {
            likeButton.setImageResource(R.drawable.upvote_filled) // change like icon state
            val currentCount = likesCount.text.toString().toInt()
            val newCount = (currentCount + 1).toString()
            likesCount.text = newCount // change like count
            updateLikeCount(momentDocID, userID, (1).toDouble()) // add 1 like
        } else {
            likeButton.setImageResource(R.drawable.upvote) // change like icon state
            val currentCount = likesCount.text.toString().toInt()
            val newCount = (currentCount - 1).toString()
            likesCount.text = newCount // change like count
            updateLikeCount(momentDocID, userID, (-1).toDouble()) // take away 1 like
        }
    }

    private fun updateLikeCount(momentID: String, userID: String, value: Double){
        val momentRef = db.collection("moments").document(momentID)
        if (value > 0){
            momentRef.update(
                "likes", FieldValue.increment(value),
                "usersLiked", FieldValue.arrayUnion(userID)
            )
        } else {
            momentRef.update(
                "likes", FieldValue.increment(value),
                "usersLiked", FieldValue.arrayRemove(userID)
            )
        }

    }

    private fun interactWithDislike(momentDocID: String, userID:String, likesCount: TextView, likeButton: ImageView, otherButton: ImageView){
        // check if like button is filled
        if (otherButton.drawable.constantState != resources.getDrawable(R.drawable.upvote, null).constantState){
            otherButton.setImageResource(R.drawable.upvote) // change like icon state
            val currentCount = likesCount.text.toString().toInt()
            val newCount = (currentCount - 1).toString()
            likesCount.text = newCount // change like count
            updateLikeCount(momentDocID, userID, (-1).toDouble()) // take away 1 like
        }

        // check if like icon is NOT filled
        if (likeButton.drawable.constantState ==
            resources.getDrawable(R.drawable.downvote, null).constantState) {
            likeButton.setImageResource(R.drawable.downvote_filled) // change like icon state
            val currentCount = likesCount.text.toString().toInt()
            val newCount = (currentCount - 1).toString()
            likesCount.text = newCount // change like count
            updateDislikeCount(momentDocID, userID, (1).toDouble()) // add 1 dislike
        } else {
            likeButton.setImageResource(R.drawable.downvote) // change like icon state
            val currentCount = likesCount.text.toString().toInt()
            val newCount = (currentCount + 1).toString()
            likesCount.text = newCount // change like count
            updateDislikeCount(momentDocID, userID, (-1).toDouble()) // take away 1 dislike
        }
    }

    private fun updateDislikeCount(momentID: String, userID: String, value: Double){
        val momentRef = db.collection("moments").document(momentID)
        if (value > 0){
            momentRef.update(
                "likes", FieldValue.increment((-1)*value),
                "usersDisliked", FieldValue.arrayUnion(userID)
            )
        } else {
            momentRef.update(
                "likes", FieldValue.increment((-1)*value),
                "usersDisliked", FieldValue.arrayRemove(userID)
            )
        }

    }

    private fun showPopupMenu(view: View, momentView: View, momentID: String) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.self_popup_menu) // Create a menu resource file (e.g., res/menu/popup_menu.xml)
        0

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_option1 -> {
                    showEditDialog(momentView, momentID)
                    true
                }
                R.id.menu_option2 -> {
                    showDeleteDialog(momentView, momentID)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showDeleteDialog(momentView: View, momentID: String) {
        // Set up the dialog builder
        val builder = MaterialAlertDialogBuilder(requireContext(),R.style.MaterialAlertDialog_App)
            .setTitle("Are you sure you want to delete this post?")
            .setPositiveButton("Confirm", null)
            .setNegativeButton("Cancel") { dialog, which ->
                // Handle negative button click
                dialog.dismiss()
            }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                deletePost(momentID)
                momentView.visibility = View.GONE
                dialog.dismiss() // Close the dialog for non-"Other" options
            }
        }
        dialog.show()
    }

    private fun deletePost(momentID: String){
        db.collection("moments").document(momentID)
            .update("isDeleted", true)
            .addOnSuccessListener { Toast.makeText(context, "Post successfully deleted", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(context, "Delete failed. Please try again later.", Toast.LENGTH_SHORT).show() }
    }

    private fun showEditDialog(momentView: View, momentID: String){
        // Inflate the custom layout
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.edit_dialog_layout, null)
        val otherTextInputLayout: TextInputLayout = view.findViewById(R.id.otherTextInputLayout)
        val momentCaption = momentView.findViewById<TextView>(R.id.momentCaption)
        otherTextInputLayout.editText?.setText(momentCaption.text)

        val builder = MaterialAlertDialogBuilder(requireContext(),R.style.MaterialAlertDialog_App)
            .setTitle("Edit your caption")
            .setView(view)
            .setPositiveButton("Confirm", null)
            .setNegativeButton("Cancel") { dialog, which ->
                // Handle negative button click
                dialog.dismiss()
            }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                // Handle positive button click

                editMoment(otherTextInputLayout.editText?.text.toString().trim(), momentID, momentView)
                dialog.dismiss() // Close the dialog for non-"Other" options
            }
        }
        dialog.show()
    }

    private fun editMoment(newCaption: String, momentID: String, momentView: View){
        val caption: TextView = momentView.findViewById(R.id.momentCaption)
        caption.text = newCaption

        val momentRef = db.collection("moments").document(momentID)

        momentRef
            .update("caption", newCaption)
            .addOnSuccessListener { Toast.makeText(context, "Post successfully updated", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(context, "Update failed. Please try again.", Toast.LENGTH_SHORT).show() }
    }

    private fun showReportDialog(momentView: View, momentID: String) {
        // Inflate the custom layout
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.report_dialog_layout, null)

        // Get references to views in the custom layout
        val optionsRadioGroup: RadioGroup = view.findViewById(R.id.optionsRadioGroup)
        val otherTextInputLayout: TextInputLayout = view.findViewById(R.id.otherTextInputLayout)

        // Set up the dialog builder
        val builder = MaterialAlertDialogBuilder(requireContext(),R.style.MaterialAlertDialog_App)
            .setTitle("Select a reason")
            .setView(view)
            .setPositiveButton("Confirm", null)
            .setNegativeButton("Cancel") { dialog, which ->
                // Handle negative button click
                dialog.dismiss()
            }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                // Handle positive button click

                // Check if the selected option is "Other"
                val selectedOptionId = optionsRadioGroup.checkedRadioButtonId

                if (selectedOptionId == -1) {
                    Toast.makeText(context, "Please select a reason", Toast.LENGTH_SHORT).show()
                } else {
                    val selectedOption = view.findViewById<RadioButton>(selectedOptionId)

                    if (selectedOption != null && selectedOption.text == "Other") {
                        // User selected "Other," handle the custom input

                        val otherText = otherTextInputLayout.editText?.text.toString().trim()

                        if (otherText != "") {
                            createReport(otherText, momentID)
                            momentView.visibility = View.GONE
                            dialog.dismiss() // Close the dialog if validation passes
                        } else {
                            otherTextInputLayout.error = "Please input a reason"
                        }
                    } else {
                        createReport(selectedOption.text as String, momentID)
                        momentView.visibility = View.GONE
                        dialog.dismiss() // Close the dialog for non-"Other" options
                    }
                }
            }
        }
        dialog.show()
    }

    private fun createReport(violation: String, momentID: String){
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

                        val reportData = hashMapOf(
                            "violation" to violation,
                            "time" to Timestamp.now(),
                            "userImageSrc" to userImage,
                            "username" to username,
                            "userID" to userID
                        )

                        val momentsRef = db.collection("moments").document(momentID)
                        momentsRef.update(
                            "reports", FieldValue.arrayUnion(reportData),
                            "reportsCount", FieldValue.increment(1)
                        )
                            .addOnSuccessListener {
                                Toast.makeText(context, "Your Report has been recorded.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to submit report", Toast.LENGTH_SHORT).show()
                            }

                    }
                }
        }
    }

}