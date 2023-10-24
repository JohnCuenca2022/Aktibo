package com.example.aktibo

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.aktibo.LoginActivity.Companion.TAG
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ExerciseItemFragment : Fragment() {
    private lateinit var exerciseID: String
    private lateinit var videoView: VideoView
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = arguments
        if (bundle != null) {
            val data = bundle.getString("exerciseID")
            if (data != null) {
                exerciseID = data
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_exercise_item, container, false)

        val db = Firebase.firestore
        val docRef = db.collection("exercises").document(exerciseID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")

                    val textViewExerciseName = view.findViewById<TextView>(R.id.textViewExerciseName)
                    textViewExerciseName.text = document.data?.get("name").toString()

                    val reps_duration = document.data?.get("reps_duration").toString()
                    val sets = document.data?.get("sets").toString()
                    val textViewRepSets = view.findViewById<TextView>(R.id.textViewRepSets)
                    if (containsOnlyDigits(reps_duration)){
                        textViewRepSets.text = "${reps_duration} repetitions (${sets} set/s)"
                    } else {
                        textViewRepSets.text = "${reps_duration} (${sets} set/s)"
                    }
                    val videoUrl = document.data?.get("video").toString()
                    videoView = view.findViewById(R.id.videoView)
                    playVideo(videoUrl)

                    // Text Instruction
                    val items: ArrayList<String> = document.data?.get("instructions") as ArrayList<String>
//                    val items = arrayOf(
//                        "Start on your hands and toes, with your hands slightly wider than shoulder-width apart.",
//                        "Keep your body straight from head to toes, like a plank.",
//                        "Bend your arms and lower your body until your chest touches the ground or goes as low as you can comfortably.",
//                        "Push through your hands and straighten your arms to raise your body back up.",
//                        "Keep your body straight throughout the movement.",
//                        "Repeat for the desired number of repetitions, maintaining good form.",
//                        "Repeat for the desired number of repetitions, maintaining good form.",
//                        "Repeat for the desired number of repetitions, maintaining good form.",
//                        "Repeat for the desired number of repetitions, maintaining good form.",
//                        "Repeat for the desired number of repetitions, maintaining good form.")

                    val layoutInstructions: LinearLayout = view.findViewById(R.id.layoutInstructions)

                    for ((index, item) in items.withIndex()) {
                        val customItem = inflater.inflate(R.layout.ordered_list_item, null) as LinearLayout

                        val textViewNumber = customItem.findViewById<TextView>(R.id.textViewNumber)
                        val listNum = index+1
                        val textViewNumberText = "$listNum."
                        textViewNumber.text = textViewNumberText

                        val textViewStep = customItem.findViewById<TextView>(R.id.textViewStep)
                        textViewStep.text = item

                        layoutInstructions.addView(customItem)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }



        return view
    }

    private fun playVideo(videoUrl: String) {
        // Set the media controller (optional)
        val mediaController = android.widget.MediaController(requireContext())
        videoView.setMediaController(mediaController)

        // Set the video URI using the provided URL
        val videoUri = Uri.parse(videoUrl)
        videoView.setVideoURI(videoUri)

        // Start video playback
        videoView.start()

        // Initialize the MediaPlayer for additional control (optional)
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(requireContext(), videoUri)
        mediaPlayer.prepare()
    }

    override fun onPause() {
        super.onPause()
        // Release resources when the fragment is paused
        videoView.stopPlayback()
        mediaPlayer.release()
    }

    fun containsOnlyDigits(input: String): Boolean {
        for (char in input) {
            if (!char.isDigit()) {
                return false
            }
        }
        return true
    }

}