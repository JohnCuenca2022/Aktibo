package com.example.aktibo

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 123
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .build()
    private lateinit var barChart: BarChart
    private lateinit var stepsMore: ImageButton

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // load press animations
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        // transition to account fragment
        val imageButtonAccount = view.findViewById<ImageButton>(R.id.imageButtonAccount)
        imageButtonAccount.setOnClickListener {
            // Apply fadeOut animation when pressed
            imageButtonAccount.startAnimation(fadeOut)

            replaceFragmentWithAnim(AccountFragment())

            // Apply fadeIn animation when released
            imageButtonAccount.startAnimation(fadeIn)
        }

        // transition to more detailed steps graph
        val stepsMore = view.findViewById<ImageButton>(R.id.stepsMore)
        stepsMore.setOnClickListener {
            // Apply fadeOut animation when pressed
            stepsMore.startAnimation(fadeOut)

            replaceFragmentWithAnim(DetailedStepsGraphFragment())

            // Apply fadeIn animation when released
            stepsMore.startAnimation(fadeIn)
        }

        // initialize barChart
        barChart = view.findViewById(R.id.barChart)

        val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                819, // e.g. 1
                account,
                fitnessOptions)
        } else {
            accessGoogleFit()
        }

        return view
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        // Example data (replace this with your own data)
        val inputData = intArrayOf(25, 123, 400, 87)

        // Create a function to update the chart with the provided data
//        updateChart(inputData)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> when (requestCode) {
                819 -> accessGoogleFit()
                else -> {
                    // Result wasn't from Google Fit
                }
            }
            else -> {
                // Permission not granted
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun accessGoogleFit() {
        val end = LocalDateTime.now()
        val start = end.minusYears(1)
        val endSeconds = end.atZone(ZoneId.systemDefault()).toEpochSecond()
        val startSeconds = start.atZone(ZoneId.systemDefault()).toEpochSecond()

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
            .bucketByTime(1, TimeUnit.DAYS)
            .build()
        val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)
        Fitness.getHistoryClient(requireContext(), account)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                // Use response data here
                Log.i(TAG, "OnSuccess()")
                checkAndRequestActivityRecognitionPermission()
                readSteps()
                getDailySteps()
            }
            .addOnFailureListener { e -> Log.d(TAG, "OnFailure()", e) }
    }

    private fun recordSteps() {
        Fitness.getRecordingClient(requireActivity(), GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions))
            .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addOnSuccessListener {
                Log.i(TAG,"Subscription was successful!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing ", e)
            }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readSteps() {
        val startTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())

        val datasource = DataSource.Builder()
            .setAppPackageName("com.google.android.gms")
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED)
            .setStreamName("estimated_steps")
            .build()

        val request = DataReadRequest.Builder()
            .aggregate(datasource)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
            .build()

        Fitness.getHistoryClient(requireActivity(), GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions))
            .readData(request)
            .addOnSuccessListener { response ->
                val totalSteps = response.buckets
                    .flatMap { it.dataSets }
                    .flatMap { it.dataPoints }
                    .sumBy { it.getValue(Field.FIELD_STEPS).asInt() }
                Log.i(TAG, "Total steps: $totalSteps")

            }
    }

    private fun getDailySteps(){
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        val endTime = calendar.timeInMillis // End time is the current date/time
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Go back 7 days
        val startTime = calendar.timeInMillis

        val googleSignInAccount = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        val fitnessDataClient = Fitness.getHistoryClient(requireActivity(), googleSignInAccount)

        val dataReadResultTask = fitnessDataClient.readData(readRequest)
        dataReadResultTask.addOnSuccessListener { dataReadResponse ->
            if (dataReadResponse.buckets.isNotEmpty()) {
                val intArray = IntArray(7)
                val dayArray = arrayOf("","","","","","","")
                var index = 0
                for (bucket in dataReadResponse.buckets) {
                    val dataSet = bucket.dataSets.firstOrNull { it.dataType == DataType.TYPE_STEP_COUNT_DELTA }
                    if (dataSet != null) {
                        for (dataPoint in dataSet.dataPoints) {
                            val timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                            val stepCount = dataPoint.getValue(Field.FIELD_STEPS).asInt()
                            val dateFormat = SimpleDateFormat("E", Locale.getDefault())
                            val day = dateFormat.format(Date(timestamp)).toString()

                            intArray[index] = stepCount
                            dayArray[index] = day
                            index += 1
                            Log.i(TAG,Date(timestamp).toString())
                            Log.i(TAG,stepCount.toString())

                        }

                        updateChart(intArray, dayArray)

                    }
                }
            }
        }.addOnFailureListener { e ->
            // Handle the error
        }
    }

    private fun updateChart(data: IntArray, days: Array<String>) {
        val entries = ArrayList<BarEntry>()

        // Create data entries for the bar chart from the input data
        for ((index, value) in data.withIndex()) {
            entries.add(BarEntry(index.toFloat(), value.toFloat()))
        }

        val set = BarDataSet(entries, "BarDataSet")
        set.color = Color.rgb(99,169,31)

        val dataSet = BarData(set)
        dataSet.barWidth = 0.9f; // set custom bar width
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.primary))

        // disable dragging/zooming
        barChart.setTouchEnabled(false)

        //axis styling
        val xAxis = barChart.xAxis
        val yAxisLeft = barChart.axisLeft
        val yAxisRight = barChart.axisRight

        //disable gridlines
        xAxis.setDrawGridLines(false)
        // yAxisLeft.setDrawGridLines(false)
        yAxisRight.setDrawGridLines(false)

        //text color
        yAxisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.primary)
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.primary)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        yAxisRight.setDrawLabels(false)

        // x-axis values
        val formatter: ValueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return days.getOrNull(value.toInt()) ?: value.toString()
            }
        }
        xAxis.granularity = 1f
        xAxis.valueFormatter = formatter

        //Disable Legend
        val legend = barChart.getLegend()
        legend.isEnabled = false

        //Disable Description
        val description = barChart.getDescription()
        description.isEnabled = false

        //Animation
        barChart.animateY(500, Easing.EaseInSine);
        barChart.animateX(500, Easing.EaseInSine);

        // setting the data
        barChart.data = dataSet
        barChart.setFitBars(true)

        barChart.invalidate()

        // circular progress bar
        val totalSteps = data[0]
        val textViewSteps = view?.findViewById<TextView>(R.id.textViewSteps)
        val progressBarSteps = view?.findViewById<CircularProgressIndicator>(R.id.progressBarSteps)
        if (progressBarSteps != null) {
            // progressBarSteps.setProgress(totalSteps)

            // Create a ValueAnimator
            val animator = ValueAnimator.ofInt(0, totalSteps)
            animator.duration = 1000 // Animation duration in milliseconds

            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                progressBarSteps.progress = animatedValue
            }

            animator.start()
        }
        if (textViewSteps != null) {
            textViewSteps.setText("$totalSteps")

            // Create a ValueAnimator
            val animator = ValueAnimator.ofInt(0, totalSteps)
            animator.duration = 1000 // Animation duration in milliseconds

            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                textViewSteps.text = animatedValue.toString()
            }

            animator.start()
        }
    }

    private fun showCaloriesBurnedFromSteps(stepCount: Int){
        // Constants for the conversion
        val STEPS_PER_100_CALORIES = 2000
        val CALORIES_BURNED_PER_STEP = 100f / STEPS_PER_100_CALORIES

        // Estimate calories burned
        val caloriesBurned = stepCount * CALORIES_BURNED_PER_STEP

    }

    private fun checkAndRequestActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            requestPermissions(
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION
            )
        } else {
            recordSteps()
            // Permission is already granted, proceed with your Google Fit API calls
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        println(requestCode)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with your Google Fit API calls
                    recordSteps()
                } else {
                    // Permission denied, handle it gracefully (e.g., show a message to the user)
                }
            }
            // Handle other permission requests if needed
        }
    }

}