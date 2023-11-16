package com.example.aktibo

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MomentsFragment : Fragment() {

    private lateinit var scrollView: ScrollView
    private lateinit var linearLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var imageButton: ImageButton

    var canLoadMoreMoments = true
    var canShowEndOfMomentsMessage = true

    val db = Firebase.firestore
    val momentsRef = db.collection("moments")

    val query = momentsRef
        .orderBy("datePosted", Query.Direction.DESCENDING)
        .limit(5)

    private lateinit var lastVisible: DocumentSnapshot

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_moments, container, false)

        scrollView = view.findViewById(R.id.scrollView)
        linearLayout = view.findViewById(R.id.scrollContainer)
        progressBar = view.findViewById(R.id.progressBar)

        // New Moment Button
        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        imageButton = view.findViewById(R.id.addNewMomentButton)
        imageButton.setOnClickListener {
            // Apply fadeOut animation when pressed
            imageButton.startAnimation(fadeOut)

            replaceFragmentWithAnim(NewMomentFragment())

            // Apply fadeIn animation when released
            imageButton.startAnimation(fadeIn)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // first moments
        firstContent()

        // add more content when user has scrolled to bottom
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (isAtBottom(scrollView) && canLoadMoreMoments) {
                // Show loading ProgressBar
                progressBar.visibility = View.VISIBLE

                addNewContent()
            }
        }
    }

    private fun replaceFragmentWithAnim(fragment: Fragment) {
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

    private fun isAtBottom(scrollView: ScrollView): Boolean {
        val scrollY = scrollView.scrollY
        val height = scrollView.height
        val scrollViewChild = scrollView.getChildAt(0)
        return scrollY + height >= scrollViewChild.height
    }

    private fun firstContent(){
        query.get()
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

        for (data in documentSnapshots){
            val userImageSrc = data.getString("userImageSrc")
            val username = data.getString("username")
            val caption = data.getString("caption")
            val imageSrc = data.getString("imageSrc")
            val likes = data.getDouble("likes")?.toInt()
            val comments = data.getDouble("comments")?.toInt()
            val commentsList = (data.get("commentsList") as? ArrayList<Map<String, Any>>).orEmpty()
            val usersLiked = data.get("usersLiked") as? List<String>

            val itemLayout = inflater.inflate(R.layout.moments_item, null)

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

            // like moment
            val momentLikeButton = itemLayout.findViewById<ImageView>(R.id.momentLikeButton)
            if (usersLiked != null){
                if (usersLiked.contains(userID)) {
                    momentLikeButton.setImageResource(R.drawable.like_icon_filled)
                }
            }
            momentLikeButton.setOnClickListener{
                if (userID != null) {
                    interactWithLike(data.id, userID, momentLikeCount, momentLikeButton)
                }
            }

            val momentCommentCount = itemLayout.findViewById<TextView>(R.id.momentCommentCount)
            momentCommentCount.text = comments.toString()

            // comment on moment
            val momentCommentButton = itemLayout.findViewById<ImageView>(R.id.momentCommentButton)
            momentCommentButton.setOnClickListener{
                val modalBottomSheet = ModalBottomSheet(data.id, momentCommentCount)

                modalBottomSheet.show(parentFragmentManager, ModalBottomSheet.TAG)

            }

            linearLayout.addView(itemLayout)
        }
    }

    private fun interactWithLike(momentDocID: String, userID:String, likesCount: TextView, likeButton: ImageView){
        // check if like icon is NOT filled
        if (likeButton.drawable.constantState ==
            resources.getDrawable(R.drawable.like_icon, null).constantState) {
            likeButton.setImageResource(R.drawable.like_icon_filled) // change like icon state
            val currentCount = likesCount.text.toString().toInt()
            val newCount = (currentCount + 1).toString()
            likesCount.text = newCount // change like count
            updateLikeCount(momentDocID, userID, (1).toDouble()) // add 1 like
        } else {
            likeButton.setImageResource(R.drawable.like_icon) // change like icon state
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
}