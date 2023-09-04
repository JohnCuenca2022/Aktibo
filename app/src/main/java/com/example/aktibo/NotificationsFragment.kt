package com.example.aktibo

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class NotificationsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

                        val scrollContainer: LinearLayout = view.findViewById(R.id.scrollContainer)

                        val mapArray = document.get("notifications") as? List<Map<String, Any>>
                        if (mapArray != null) {
                            for (map in mapArray) {
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
                                scrollContainer.addView(presetView)
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