package com.example.aktibo

import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class NotificationsFragment : Fragment() {

    private lateinit var notificationsArrayList: List<Map<String, Any>>

    private lateinit var scrollContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var progressBar: ProgressBar

    var canLoadMoreNotifications = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollView = view.findViewById(R.id.scrollView)
        progressBar = view.findViewById(R.id.progressBar)

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (isAtBottom(scrollView) && canLoadMoreNotifications) {
                // Show loading ProgressBar
                progressBar.visibility = View.VISIBLE
                canLoadMoreNotifications = false

                showNotifications(5)
            }
        }

        val user = Firebase.auth.currentUser
        if (user == null) {
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }

        user?.let {
            val uid = it.uid

            val db = Firebase.firestore
            val docRef = db.collection("users").document(uid)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {

                        scrollContainer = view.findViewById(R.id.scrollContainer)

                        val mapArray = document.get("notifications") as? List<Map<String, Any>>
                        if (mapArray != null) {
                            if (mapArray.isEmpty()){
                                val textViewMessage = view.findViewById<TextView>(R.id.textViewMessage)
                                textViewMessage.visibility = View.VISIBLE
                            } else {
                                val mapArrayReversed = mapArray.reversed()
                                notificationsArrayList = mapArrayReversed

                                showNotifications(10)
                            }

                        }

                    } else {
                        Log.d(ContentValues.TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(ContentValues.TAG, "get failed with ", exception)
                }


        }
    }

    private fun isAtBottom(scrollView: ScrollView): Boolean {
        val scrollY = scrollView.scrollY
        val height = scrollView.height
        val scrollViewChild = scrollView.getChildAt(0)
        return scrollY + height >= scrollViewChild.height
    }

    private fun showNotifications(limit: Int) {
        if (!::notificationsArrayList.isInitialized){
            progressBar.visibility = View.GONE
            return
        }

        if (notificationsArrayList.isEmpty()){
            Toast.makeText(requireContext(), "There are no more notifications", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }

        for ((index, map) in notificationsArrayList.withIndex()) {
            if (index == notificationsArrayList.size-1){
                notificationsArrayList = notificationsArrayList.subList(notificationsArrayList.size, notificationsArrayList.size)
                canLoadMoreNotifications = true
            } else if (index >= limit){
                notificationsArrayList = notificationsArrayList.subList(index, notificationsArrayList.size)
                canLoadMoreNotifications = true
                return
            }

            val presetView: View =
                LayoutInflater.from(requireActivity())
                    .inflate(R.layout.notification_item, null)
            val params =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            params.setMargins(0, 0, 0, 20);
            presetView.layoutParams = params
            val presetTitle =
                presetView.findViewById<TextView>(R.id.notificationHeader)
            val timeDiff = getTimeAgoFromTimestamp(map["time"] as Timestamp)
            presetTitle.text = timeDiff
            val message =
                presetView.findViewById<TextView>(R.id.notificationBody)
            message.text = map["message"].toString()

            val notifImageView = presetView.findViewById<ImageView>(R.id.notifImageView)
            if (map["message"].toString().contains("Remember to record")){
                notifImageView.setImageResource(R.drawable.food_notif_icon)
            } else {
                notifImageView.setImageResource(R.drawable.star_notif_icon)
            }

            scrollContainer.addView(presetView)
        }
        canLoadMoreNotifications = true
    }

    fun getTimeAgoFromTimestamp(timestamp: Timestamp): String {
        val currentTime = Calendar.getInstance().time
        val timestampDate = timestamp.toDate()

        val diff = currentTime.time - timestampDate.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> {
                if (days == 1L) {
                    "1 day ago"
                } else {
                    "$days days ago"
                }
            }

            hours > 0 -> {
                if (hours == 1L) {
                    "1 hour ago"
                } else {
                    "$hours hours ago"
                }
            }

            minutes > 0 -> {
                if (minutes == 1L) {
                    "1 minute ago"
                } else {
                    "$minutes minutes ago"
                }
            }

            else -> "Just now"
        }
    }

}