package com.example.aktibo

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.elyeproj.loaderviewlibrary.LoaderTextView
import com.example.aktibo.LoginActivity.Companion.TAG
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import java.util.regex.Pattern


class ExerciseItemFragment : Fragment() {
    private lateinit var exerciseID: String

    private lateinit var videoURL: String
    private lateinit var videoView: VideoView
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var timerTextView: TextView
    private lateinit var buttonExerciseStart: Button
    private lateinit var buttonExerciseCancel: Button
    private lateinit var buttonExerciseFinish: Button
    private lateinit var countDownTimer: CountDownTimer

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

        timerTextView = view.findViewById(R.id.timerTextView)
        buttonExerciseStart = view.findViewById(R.id.buttonExerciseStart)
        buttonExerciseCancel = view.findViewById(R.id.buttonExerciseCancel)
        buttonExerciseFinish = view.findViewById(R.id.buttonExerciseFinish)

        return view
    }

    // Function to start the timer
    fun startTimer() {
        // Start the CountDownTimer
        countDownTimer.start()
    }

    private fun updateTimer(millisUntilFinished: Long) {
        val seconds = (millisUntilFinished / 1000).toInt() % 60
        val minutes = (millisUntilFinished / (1000 * 60)).toInt()

        val timeString = String.format("%02d:%02d", minutes, seconds)

        // Update the TextView with the formatted time
        timerTextView.text = timeString
    }

    private fun createCountDownTimer(totalTimeInMillis: Long): CountDownTimer {
        return object : CountDownTimer(totalTimeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update the TextView with the remaining time
                updateTimer(millisUntilFinished)
            }

            override fun onFinish() {
                // Timer finished, you can handle any desired actions here
                buttonExerciseFinish.visibility = View.VISIBLE
                timerTextView.text = "00:00"
            }
        }
    }

    // Override onStop to cancel the CountDownTimer when the activity is stopped
    override fun onStop() {
        super.onStop()
        countDownTimer.cancel()
    }

    override fun onResume() {
        super.onResume()

        if (!displayedUI){
            displayedUI = true
            displayData()
        }
    }

    private fun displayData() = runBlocking{
        val db = Firebase.firestore
        val docRef = db.collection("exercises").document(exerciseID)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // exercise record button
                    val name = document.getString("name")?: ""
                    val est_time = document.get("est_time").toString()
                    val timeInMills = parseTimeStringToMillis(est_time)

                    countDownTimer = createCountDownTimer(timeInMills)
                    val timeString = formatMillisecondsToTimeString(timeInMills)
                    timerTextView.text = timeString

                    buttonExerciseStart.setOnClickListener{
                        buttonExerciseStart.visibility = View.GONE
                        buttonExerciseFinish.visibility = View.GONE
                        buttonExerciseCancel.visibility = View.VISIBLE

                        startTimer()
                    }

                    buttonExerciseCancel.setOnClickListener{
                        buttonExerciseCancel.visibility = View.GONE
                        buttonExerciseFinish.visibility = View.GONE
                        buttonExerciseStart.visibility = View.VISIBLE

                        countDownTimer.cancel()
                        timerTextView.text = timeString
                    }

                    buttonExerciseFinish.setOnClickListener{
                        buttonExerciseFinish.visibility = View.GONE
                        buttonExerciseCancel.visibility = View.GONE
                        buttonExerciseStart.visibility = View.VISIBLE

                        timerTextView.text = timeString
                        recordExercise(name)
                    }

                    // Exercise name/title
                    val exerciseName = document.data?.get("name").toString()
                    textViewExerciseName.text = exerciseName
                    textViewExerciseNameLoader.visibility = View.INVISIBLE
                    textViewExerciseName.visibility = View.VISIBLE

                    // Instructional video url
                    videoURL = document.data?.get("video").toString()
                    setupVideoView(videoView, videoURL)

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

    }

    private fun setupVideoView(videoView: VideoView, videoUrl: String) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Create a MediaController
            val mediaController = MediaController(requireContext())
            mediaController.setAnchorView(videoView)

            // Set the MediaController to the VideoView
            videoView.setMediaController(mediaController)

            mediaController.addOnUnhandledKeyEventListener { v: View?, event: KeyEvent ->
                //Handle BACK button
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    mediaController.hide() //Hide mediaController,according to your needs, you can also called here onBackPressed() or finish()
                }
                true
            }
        } else {
            val mediaController = (object : MediaController(requireContext()) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                        parentFragmentManager.popBackStack()
                    }
                    return super.dispatchKeyEvent(event)
                }
            })

            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
        }

        // Set the video URI and start the video
        val videoUri = Uri.parse(videoUrl)
        videoView.setVideoURI(videoUri)
        videoView.start()

        // remove loading animation
        val videoViewContainer = view?.findViewById<ConstraintLayout>(R.id.videoViewContainer)
        val viewToRemove = view?.findViewById<ProgressBar>(R.id.progressBar2)
        if (videoViewContainer != null) {
            videoViewContainer.removeView(viewToRemove)
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

    fun parseTimeStringToMillis(timeString: String): Long {
        // Regular expression to extract hours, minutes, and seconds
        val pattern = Pattern.compile("(?:(\\d+)\\s*(?:hours?|hrs?|h)\\s*)?(?:(\\d+)\\s*(?:minutes?|mins?|m)\\s*)?(?:(\\d+)\\s*(?:seconds?|secs?|s)\\s*)?")
        val matcher = pattern.matcher(timeString)

        var hours = 0
        var minutes = 0
        var seconds = 0

        if (matcher.find()) {
            hours = matcher.group(1)?.toInt() ?: 0
            minutes = matcher.group(2)?.toInt() ?: 0
            seconds = matcher.group(3)?.toInt() ?: 0
        }

        // Calculate the total time in milliseconds
        return hours * 60 * 60 * 1000L + minutes * 60 * 1000L + seconds * 1000L
    }

    fun formatMillisecondsToTimeString(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun recordExercise(name: String) {
        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid
            val db = Firebase.firestore

            val exerciseRecord = mapOf(
                "date" to Timestamp.now(),
                "exerciseID" to exerciseID,
                "exerciseName" to name
            )

            val userRef = db.collection("users").document(uid)
            userRef
                .update("exerciseRecords", FieldValue.arrayUnion(exerciseRecord))
                .addOnSuccessListener {
                    Toast.makeText(context, "Exercise Recorded", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to record exercise", Toast.LENGTH_SHORT).show()
                }
        }
    }

}