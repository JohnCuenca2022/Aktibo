package com.example.aktibo

import android.annotation.SuppressLint
import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.elyeproj.loaderviewlibrary.LoaderTextView
import com.example.aktibo.LoginActivity.Companion.TAG
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import java.util.regex.Pattern


class ExerciseItemFragment : Fragment() {
    private lateinit var exerciseID: String

    private lateinit var backToRoutineListButton: ImageButton
    private lateinit var previousExerciseButton: ImageButton
    private lateinit var nextExerciseButton: ImageButton

    var canShowExitDialog = true

    var isPartOfRoutine = false
    var exerciseIndex = 0
    var exerciseItemList = ArrayList<Map<String, Any>>()

    private lateinit var videoURL: String
    lateinit var player: ExoPlayer
    lateinit var playerView: StyledPlayerView
    lateinit var progressBar2: ProgressBar

    private lateinit var timerTextView: TextView
    private lateinit var buttonExerciseStart: Button
    private lateinit var buttonExerciseCancel: Button
    private lateinit var buttonExerciseFinish: Button
    private lateinit var countDownTimer: CountDownTimer

    private lateinit var textViewTitle: TextView
    private lateinit var textViewTitleLoader: LoaderTextView
    private lateinit var textViewExerciseName: TextView
    private lateinit var textViewExerciseNameLoader: LoaderTextView
    private lateinit var textViewRepSets: TextView
    private lateinit var textViewRepSetsLoader: LoaderTextView
    private lateinit var layoutInstructions: LinearLayout

    var displayedUI = false
    var millisUntilFinished = 0L
    var timerStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen on while the fragment is active
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val bundle = arguments
        if (bundle != null) {
            val data = bundle.getString("exerciseID")
            if (data != null) {
                exerciseID = data
            }
            val isPartOfRoutineData = bundle.getBoolean("isPartOfRoutine")
            val exerciseIndexData = bundle.getInt("exerciseIndex")
            val exerciseItemListData = bundle.getSerializable("exerciseItemList")
            isPartOfRoutine = isPartOfRoutineData
            exerciseIndex = exerciseIndexData
            if (exerciseItemListData != null){
                exerciseItemList = exerciseItemListData as ArrayList<Map<String, Any>>
            }

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_exercise_item, container, false)

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPartOfRoutine){
                    if (canShowExitDialog) {
                        showExitDialog()
                    }

                } else {
                    activity?.supportFragmentManager?.popBackStack()
                }

            }
        })

        backToRoutineListButton = view.findViewById(R.id.backToRoutineListButton)
        previousExerciseButton = view.findViewById(R.id.previousExerciseButton)
        nextExerciseButton = view.findViewById(R.id.nextExerciseButton)

        if (isPartOfRoutine) {
            backToRoutineListButton.visibility = View.VISIBLE
            if (exerciseIndex != 0){
                previousExerciseButton.visibility = View.VISIBLE
            }
            if (exerciseIndex != exerciseItemList.size-1){
                nextExerciseButton.visibility = View.VISIBLE
            }

            backToRoutineListButton.setOnClickListener{
                showExitDialog()
            }
            previousExerciseButton.setOnClickListener{
                val exerciseID = exerciseItemList[exerciseIndex-1]["exerciseID"].toString()
                replaceFragmentWithAnimWithDataLeft(ExerciseItemFragment(), exerciseID, exerciseIndex-1, exerciseItemList)
            }
            nextExerciseButton.setOnClickListener{
                val exerciseID = exerciseItemList[exerciseIndex+1]["exerciseID"].toString()
                replaceFragmentWithAnimWithData(ExerciseItemFragment(), exerciseID, exerciseIndex+1, exerciseItemList)
            }
        }

        textViewTitle = view.findViewById(R.id.textViewTitle)
        textViewTitleLoader = view.findViewById(R.id.textViewTitleLoader)
        textViewTitleLoader.resetLoader()
        textViewExerciseName = view.findViewById(R.id.textViewExerciseName)
        textViewExerciseNameLoader = view.findViewById(R.id.textViewExerciseNameLoader)
        textViewExerciseNameLoader.resetLoader()
        textViewRepSets = view.findViewById(R.id.textViewRepSets)
        textViewRepSetsLoader = view.findViewById(R.id.textViewRepSetsLoader)
        textViewRepSetsLoader.resetLoader()
        layoutInstructions = view.findViewById(R.id.layoutInstructions)

        player = ExoPlayer.Builder(requireContext()).build()
        playerView = view.findViewById(R.id.playerView)
        val loadControl = DefaultLoadControl.Builder()
            // for retaining previously loaded one hour data in buffer
            .setBackBuffer(/* backBufferDurationMs= */ 1000 * 60 * 60, true)
            .build()
        playerView.player = player
        progressBar2 = view.findViewById(R.id.progressBar2)

        timerTextView = view.findViewById(R.id.timerTextView)
        buttonExerciseStart = view.findViewById(R.id.buttonExerciseStart)
        buttonExerciseCancel = view.findViewById(R.id.buttonExerciseCancel)
        buttonExerciseFinish = view.findViewById(R.id.buttonExerciseFinish)

        return view
    }

    private fun showExitDialog(){
        canShowExitDialog = false

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exit Routine")
            .setMessage("Are you sure you want to exit?")
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                canShowExitDialog = true
            }
            .setPositiveButton("Exit") { dialog, which ->
                canShowExitDialog = true
                parentFragmentManager.popBackStack("RoutineFragment", POP_BACK_STACK_INCLUSIVE)
            }
            .setOnDismissListener {
                canShowExitDialog = true
            }
            .setOnCancelListener {
                canShowExitDialog = true
            }
            .show()
    }

    // Function to start the timer
    fun startTimer() {
        // Start the CountDownTimer
        countDownTimer = createCountDownTimer(millisUntilFinished)
        val timeString = formatMillisecondsToTimeString(millisUntilFinished)
        timerTextView.text = timeString
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
                ::millisUntilFinished.set(millisUntilFinished)
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
        if (::countDownTimer.isInitialized){
            countDownTimer.cancel()
        }

        if (::player.isInitialized){
            player.playWhenReady = false
            player.release()
        }
    }

    override fun onResume() {
        super.onResume()

        if (::countDownTimer.isInitialized){
            if (timerStarted) {
                countDownTimer = createCountDownTimer(millisUntilFinished)
                val timeString = formatMillisecondsToTimeString(millisUntilFinished)
                timerTextView.text = timeString
                countDownTimer.start()
            }
        }

        if (::videoURL.isInitialized){
            player = ExoPlayer.Builder(requireContext()).build()
            playerView.player = player
            setupVideoView(videoURL)
        }


        if (!displayedUI){
            displayedUI = true
            displayData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear the FLAG_KEEP_SCREEN_ON flag when the fragment is destroyed
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun displayData() = runBlocking{
        val db = Firebase.firestore
        val docRef = db.collection("exercises").document(exerciseID)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // exercise record button
                    val name = document.getString("name")?: ""
                    val category = document.getString("category")?: ""
                    val est_time = document.get("est_time").toString()
                    val timeInMills = parseTimeStringToMillis(est_time)

                    countDownTimer = createCountDownTimer(timeInMills)
                    val timeString = formatMillisecondsToTimeString(timeInMills)
                    timerTextView.text = timeString
                    millisUntilFinished = timeInMills

                    buttonExerciseStart.setOnClickListener{
                        buttonExerciseStart.visibility = View.GONE
                        buttonExerciseFinish.visibility = View.GONE
                        buttonExerciseCancel.visibility = View.VISIBLE

                        millisUntilFinished = timeInMills
                        startTimer()
                        timerStarted = true
                    }

                    buttonExerciseCancel.setOnClickListener{
                        buttonExerciseCancel.visibility = View.GONE
                        buttonExerciseFinish.visibility = View.GONE
                        buttonExerciseStart.visibility = View.VISIBLE

                        countDownTimer.cancel()
                        millisUntilFinished = timeInMills
                        timerTextView.text = timeString

                        timerStarted = false
                    }

                    buttonExerciseFinish.setOnClickListener{
                        buttonExerciseFinish.visibility = View.GONE
                        buttonExerciseCancel.visibility = View.GONE
                        buttonExerciseStart.visibility = View.VISIBLE

                        timerTextView.text = timeString
                        recordExercise(name)
                        timerStarted = false
                    }

                    // Exercise Category
                    textViewTitle.text = classifyExercise(category)
                    textViewTitleLoader.visibility = View.INVISIBLE
                    textViewTitle.visibility = View.VISIBLE

                    // Exercise name/title
                    val exerciseName = document.data?.get("name").toString()
                    textViewExerciseName.text = exerciseName
                    textViewExerciseNameLoader.visibility = View.INVISIBLE
                    textViewExerciseName.visibility = View.VISIBLE

                    // Instructional video url
                    videoURL = document.data?.get("video").toString()
                    setupVideoView(videoURL)

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

    fun classifyExercise(input: String): String {
        return when {
            input.contains("upper", ignoreCase = true) -> "Upper Body Exercises"
            input.contains("lower", ignoreCase = true) -> "Lower Body Exercises"
            input.contains("whole", ignoreCase = true) -> "Whole Body Exercises"
            else -> "Exercise"
        }
    }

    private fun setupVideoView(videoUrl: String) {
        val mediaItem = MediaItem.fromUri(ensureMp4Extension(videoUrl))

        if (::player.isInitialized) {
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            progressBar2.visibility = View.GONE

            player.addListener(object: Player.Listener{
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)

                    Toast.makeText(requireContext(), "Error playing video.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    fun ensureMp4Extension(input: String): String {
        val mp4Extension = ".mp4"

        if (!input.endsWith(mp4Extension, ignoreCase = true)) {
            // Append ".mp4" to the input string
            return input + mp4Extension
        }

        return input
    }

//    private fun setupVideoView(videoView: VideoView, videoUrl: String) {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            // Create a MediaController
//            val mediaController = MediaController(requireContext())
//            mediaController.setAnchorView(videoView)
//
//            // Set the MediaController to the VideoView
//            videoView.setMediaController(mediaController)
//
//            mediaController.addOnUnhandledKeyEventListener { v: View?, event: KeyEvent ->
//                //Handle BACK button
//                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
//                    mediaController.hide() //Hide mediaController,according to your needs, you can also called here onBackPressed() or finish()
//                }
//                true
//            }
//        } else {
//            val mediaController = (object : MediaController(requireContext()) {
//                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
//                    if (event.keyCode == KeyEvent.KEYCODE_BACK) {
//                        parentFragmentManager.popBackStack()
//                    }
//                    return super.dispatchKeyEvent(event)
//                }
//            })
//
//            mediaController.setAnchorView(videoView)
//            videoView.setMediaController(mediaController)
//        }
//
//        // Set the video URI and start the video
////        val videoUri = Uri.parse(videoUrl)
////        videoView.setVideoURI(videoUri)
////        videoView.start()
//
//        AsyncTask.execute {
//            val videoUri = Uri.parse(videoUrl)
//
//            // Set the video URI on the main thread
//            videoView.post {
//                videoView.setVideoURI(videoUri)
//                videoView.setOnPreparedListener { mediaPlayer ->
//                    // Video is prepared, start playing and hide loading animation
//                    mediaPlayer.start()
//                    // remove loading animation
//                    val videoViewContainer = view?.findViewById<ConstraintLayout>(R.id.videoViewContainer)
//                    val viewToRemove = view?.findViewById<ProgressBar>(R.id.progressBar2)
//                    if (videoViewContainer != null) {
//                        videoViewContainer.removeView(viewToRemove)
//                    }
//                }
//                videoView.setOnErrorListener { _, _, _ ->
//                    // Handle error, e.g., show an error message
//                    // remove loading animation
//                    val videoViewContainer = view?.findViewById<ConstraintLayout>(R.id.videoViewContainer)
//                    val viewToRemove = view?.findViewById<ProgressBar>(R.id.progressBar2)
//                    if (videoViewContainer != null) {
//                        videoViewContainer.removeView(viewToRemove)
//                    }
//                    true
//                }
//            }
//        }
//
//
//    }

    override fun onPause() {
        super.onPause()
        // Release resources when the fragment is paused
//        videoView.stopPlayback()
//        mediaPlayer.release()
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
                "exerciseName" to name,
                "isPartOfRoutine" to isPartOfRoutine
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

    private fun replaceFragmentWithAnimWithData(fragment: Fragment, exerciseID: String, exerciseIndex: Int, exerciseItemListData: ArrayList<Map<String, Any>>) {
        val bundle = Bundle()
        bundle.putString("exerciseID", exerciseID)
        bundle.putBoolean("isPartOfRoutine", true)
        bundle.putInt("exerciseIndex", exerciseIndex)
        bundle.putSerializable("exerciseItemList", exerciseItemListData)

        val newFragment = fragment
        newFragment.arguments = bundle

        val fragmentManager = getParentFragmentManager()
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            R.anim.slide_in_right, // Enter animation
            R.anim.slide_out_left, // Exit animation
            R.anim.slide_in_left, // Pop enter animation (for back navigation)
            R.anim.slide_out_right // Pop exit animation (for back navigation)
        )
        fragmentTransaction.replace(R.id.fragment_container, newFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun replaceFragmentWithAnimWithDataLeft(fragment: Fragment, exerciseID: String, exerciseIndex: Int, exerciseItemListData: ArrayList<Map<String, Any>>) {
        val bundle = Bundle()
        bundle.putString("exerciseID", exerciseID)
        bundle.putBoolean("isPartOfRoutine", true)
        bundle.putInt("exerciseIndex", exerciseIndex)
        bundle.putSerializable("exerciseItemList", exerciseItemListData)

        val newFragment = fragment
        newFragment.arguments = bundle

        val fragmentManager = getParentFragmentManager()
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            R.anim.slide_in_left, // Enter animation
            R.anim.slide_out_right, // Exit animation
            R.anim.slide_in_left, // Pop enter animation (for back navigation)
            R.anim.slide_out_right // Pop exit animation (for back navigation)
        )
        fragmentTransaction.replace(R.id.fragment_container, newFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

}