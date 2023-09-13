package com.example.aktibo

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
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
import java.util.concurrent.TimeUnit


interface Backable {
    fun onBackPressed(): Boolean
}
class HomeFragment : Fragment(), Backable {

    private lateinit var auth: FirebaseAuth
    private val MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 123
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .build()
    private lateinit var barChart: BarChart

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val button = view.findViewById<ImageButton>(R.id.imageButton)
        button.setOnClickListener {
            // Get the fragment manager.
            val fragmentManager = getParentFragmentManager()

            // Create a fragment transaction.
            val fragmentTransaction = fragmentManager.beginTransaction()

            // Replace the current fragment with the new fragment.
            fragmentTransaction.replace(R.id.fragment_container, AccountFragment())

            // Commit the fragment transaction.
            fragmentTransaction.commit()
        }

        barChart = view.findViewById(R.id.barChart)


        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                var index = 0
                for (bucket in dataReadResponse.buckets) {
                    val dataSet = bucket.dataSets.firstOrNull { it.dataType == DataType.TYPE_STEP_COUNT_DELTA }
                    if (dataSet != null) {

                        for (dataPoint in dataSet.dataPoints) {
                            val timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                            val stepCount = dataPoint.getValue(Field.FIELD_STEPS).asInt()

                            intArray[index] = stepCount
                            index += 1
                            Log.i(TAG,Date(timestamp).toString())
                            Log.i(TAG,stepCount.toString())

                        }
                        updateChart(intArray)
                        // showCaloriesBurned()
                    }
                }
            }
        }.addOnFailureListener { e ->
            // Handle the error
        }
    }

    private fun updateChart(data: IntArray) {
        val entries = ArrayList<BarEntry>()

        // Create data entries for the bar chart from the input data
        for ((index, value) in data.withIndex()) {
            entries.add(BarEntry(index.toFloat() + 1, value.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Bar Chart Example")
        dataSet.color = Color.BLUE
        // Set a custom bar width (default is 0.85f)
        val leftYAxis = barChart.axisLeft
        leftYAxis.granularity = 100f
        leftYAxis.labelCount = 5
        leftYAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        // Get the X-axis and Y-axis of the chart
        val xAxis = barChart.xAxis
        val yAxisLeft = barChart.axisLeft

// Disable gridlines for both X and Y axes
        xAxis.setDrawGridLines(false)
        yAxisLeft.setDrawGridLines(false)

        xAxis.textColor = Color.RED




        yAxisLeft.textColor = Color.BLUE
        dataSet.valueTextColor = Color.GREEN
        val barData = BarData(dataSet)



        val description = Description()
        description.text = "My Bar Chart"
        description.textColor = Color.MAGENTA
        barChart.description = description

        barChart.data = barData
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

    override fun onBackPressed(): Boolean {
        // Replace the current fragment with another fragment.
        val fragmentManager = getParentFragmentManager()
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, HomeFragment())
        fragmentTransaction.commit()

        return true
    }

    private fun checkAndRequestActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                requireActivity(),
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