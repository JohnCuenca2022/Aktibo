package com.example.aktibo

import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getSystemService
import com.elyeproj.loaderviewlibrary.LoaderTextView
import com.example.aktibo.LoginActivity.Companion.TAG
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ExerciseItemFragment : Fragment() {
    private lateinit var exerciseID: String

    private lateinit var videoView: VideoView
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var textViewExerciseName: TextView
    private lateinit var textViewExerciseNameLoader: LoaderTextView
    private lateinit var textViewRepSets: TextView
    private lateinit var textViewRepSetsLoader: LoaderTextView
    private lateinit var layoutInstructions: LinearLayout

    var displayedUI = false

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

        videoView = view.findViewById(R.id.videoView)
        textViewExerciseName = view.findViewById(R.id.textViewExerciseName)
        textViewExerciseNameLoader = view.findViewById(R.id.textViewExerciseNameLoader)
        textViewExerciseNameLoader.resetLoader()
        textViewRepSets = view.findViewById(R.id.textViewRepSets)
        textViewRepSetsLoader = view.findViewById(R.id.textViewRepSetsLoader)
        textViewRepSetsLoader.resetLoader()
        layoutInstructions = view.findViewById(R.id.layoutInstructions)
        mediaPlayer = MediaPlayer()

        return view
    }

    override fun onResume() {
        super.onResume()

        if (!displayedUI){
            displayedUI = true
            displayData()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun displayData() = runBlocking{
        val db = Firebase.firestore
        val docRef = db.collection("exercises").document(exerciseID)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {

                    // Exercise name/title
                    val exerciseName = document.data?.get("name").toString()
                    textViewExerciseName.text = exerciseName
                    textViewExerciseNameLoader.visibility = View.INVISIBLE
                    textViewExerciseName.visibility = View.VISIBLE

                    // Instructional video url
                    val videoUrl = document.data?.get("video").toString()
                    GlobalScope.launch(Dispatchers.IO) {
                        playVideo(videoUrl)
                    }

                    // Exercise reps and duration
                    val reps_duration = document.data?.get("reps_duration").toString()
                    val sets = document.data?.get("sets").toString()
                    if (containsOnlyDigits(reps_duration)){
                        val repSetsText = "$reps_duration repetitions (${sets} set/s)"
                        textViewRepSets.text = repSetsText
                        textViewRepSetsLoader.visibility = View.GONE
                        textViewRepSets.visibility = View.VISIBLE
                    } else {
                        val repSetsText = "$reps_duration (${sets} set/s)"
                        textViewRepSets.text = repSetsText
                        textViewRepSetsLoader.visibility = View.GONE
                        textViewRepSets.visibility = View.VISIBLE
                    }

                    // Exercise steps/instructions
                    val instructions = document.data?.get("instructions")
                    var items: ArrayList<String> = arrayListOf()
                    if (instructions != null){
                        items = instructions as ArrayList<String>
                        layoutInstructions.removeAllViews()

                        val inflater = LayoutInflater.from(requireContext())
                        for ((index, item) in items.withIndex()) {

                            if (item == ""){
                                continue
                            }

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
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
                Toast.makeText(requireContext(), "Failed to get resource", Toast.LENGTH_SHORT).show()
            }

//        if (videoUrl != ""){
//            playVideo(videoUrl)
//        }
    }


     private suspend fun playVideo(videoUrl: String) {

         val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
         val networkInfo = connectivityManager.activeNetworkInfo

         println("I got here")
         if (networkInfo != null && networkInfo.isConnected) {
             // Set the media controller (optional)
             val mediaController = android.widget.MediaController(requireContext())

             videoView.setMediaController(mediaController)

             withContext(Dispatchers.Main) {
                 val videoViewContainer = view?.findViewById<ConstraintLayout>(R.id.videoViewContainer)
                 val viewToRemove = view?.findViewById<ProgressBar>(R.id.progressBar2)
                 if (videoViewContainer != null) {
                     videoViewContainer.removeView(viewToRemove)
                 }

                 // Set the video URI using the provided URL
                 val videoUri = Uri.parse(videoUrl)
                 videoView.setVideoURI(videoUri)

                 // Start video playback
                 videoView.start()

                 // Initialize the MediaPlayer for additional control (optional)
                 mediaPlayer.setDataSource(requireContext(), videoUri)
                 mediaPlayer.prepare()
             }

             println("I got here too")
         } else {
             Toast.makeText(requireContext(), "Could not load video", Toast.LENGTH_SHORT).show()
         }
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