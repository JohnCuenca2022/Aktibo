package com.example.aktibo

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
        .build()
    private lateinit var barChart: BarChart
    private lateinit var stepsMore: ImageButton

    private lateinit var lineChart: LineChart
    private lateinit var weightMore: ImageButton

    private lateinit var siblingView: View
    private lateinit var targetView: View
    private lateinit var textView: TextView

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

        // initialize barChart
        barChart = view.findViewById(R.id.barChart)

        // transition to more detailed steps graph
        stepsMore = view.findViewById(R.id.stepsMore)
        stepsMore.setOnClickListener {
            // Apply fadeOut animation when pressed
            stepsMore.startAnimation(fadeOut)

            replaceFragmentWithAnim(DetailedStepsGraphFragment())

            // Apply fadeIn animation when released
            stepsMore.startAnimation(fadeIn)
        }

        // initialize lineChart
        lineChart = view.findViewById(R.id.lineChart)

        weightMore = view.findViewById(R.id.weightMore)
        weightMore.setOnClickListener{
            // Apply fadeOut animation when pressed
            stepsMore.startAnimation(fadeOut)

            replaceFragmentWithAnim(DetailedStepsGraphFragment())

            // Apply fadeIn animation when released
            stepsMore.startAnimation(fadeIn)
        }

        // bmi indicator
        siblingView = view.findViewById<View>(R.id.bmi_slider)
        targetView = view.findViewById<View>(R.id.bmi_indicator)
        textView = view.findViewById<TextView>(R.id.textView2)


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

        // get user
        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid // user ID

            // get user data
            val db = Firebase.firestore
            val docRef = db.collection("users").document(uid)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {

                        // Weight Chart
                        updateWeightChart(document)

                        // BMI Indicator
                        val weight = document.getDouble("weight")?: 1.0
                        val height = document.getDouble("height")?: 1.0
                        showBMI(weight, height)

                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }


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
                val textViewSteps = view?.findViewById<TextView>(R.id.textViewSteps)
                val progressBarSteps = view?.findViewById<CircularProgressIndicator>(R.id.progressBarSteps)
                if (progressBarSteps != null) {
                    progressBarSteps.setProgress(totalSteps)
                }
                if (textViewSteps != null) {
                    textViewSteps.setText("$totalSteps")
                }
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
                        updateStepsChart(intArray, dayArray)
                    }
                }
            }
        }.addOnFailureListener { e ->
            // Handle the error
        }
    }

    private fun updateStepsChart(data: IntArray, days: Array<String>) {
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
    }

    private fun updateWeightChart(document: DocumentSnapshot){

        val weightRecord = document.get("weightRecord") as? ArrayList<Map<Any, Any>> ?: ArrayList()
        val dataPoints = DoubleArray(7)

        val currentWeight = document.getDouble("weight")
        if (weightRecord.isEmpty()){ // no entries
            for (i in 0 until 7) { // loop 7 times
                dataPoints[i] = currentWeight?: 0.0
            }
        }

        val days = getPreviousDaysList(7)
        days.reverse()

        // Create data entries for the bar chart from the input data
        val entries = ArrayList<Entry>()
        for ((index, value) in dataPoints.withIndex()) {
            entries.add(Entry(index.toFloat(), value.toFloat()))
        }

        val set = LineDataSet(entries, "LineDataSet")
        set.color = Color.rgb(99,169,31)
        set.setCircleColor(R.color.green)

        val dataSet = LineData(set)
        //dataSet.barWidth = 0.9f; // set custom bar width
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.primary))

        // disable dragging/zooming
        lineChart.setTouchEnabled(false)

        //axis styling
        val xAxis = lineChart.xAxis
        val yAxisLeft = lineChart.axisLeft
        val yAxisRight = lineChart.axisRight

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
        val legend = lineChart.getLegend()
        legend.isEnabled = false

        //Disable Description
        val description = lineChart.getDescription()
        description.isEnabled = false

        //Animation
        lineChart.animateY(500, Easing.EaseInSine);
        lineChart.animateX(500, Easing.EaseInSine);

        // setting the data
        lineChart.data = dataSet
        //lineChart.setFitBars(true)

        lineChart.invalidate()
    }

    private fun getPreviousDaysList(numDays: Int): Array<String> {
        val calendar = Calendar.getInstance()
        val daysList = mutableListOf<String>()

        for (i in numDays - 1 downTo 0) {
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            val dayName = sdf.format(calendar.time)
            daysList.add(dayName)

            // Move to the previous day
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        return daysList.toTypedArray()
    }

    private fun getWeightRecord(weightRecord: ArrayList<Map<String, Any>>,): DoubleArray{
        val weightRecord = sortByFirebaseTimestampField(weightRecord,"timestamp") //
        val dataPoints = DoubleArray(7)

        val currentDate = Calendar.getInstance()
//        for (i in 6 downTo 0) {
//            if(isSameDate()){
//
//            }
//            println("Day $i: $dayName")
//
//            // Move to the previous day
//            currentDate.add(Calendar.DAY_OF_MONTH, -1)
//        }

        return dataPoints
    }

    fun sortByFirebaseTimestampField(listOfMaps: ArrayList<Map<String, Any>>, fieldName: String): ArrayList<Map<String, Any>> {
        return listOfMaps.sortedBy { (it[fieldName] as Timestamp).toDate() } as ArrayList<Map<String, Any>>
    }

    fun isSameDate(timestamp: Timestamp, calendar: Calendar): Boolean {
        val firebaseDate = Calendar.getInstance()
        firebaseDate.time = timestamp.toDate()

        return (calendar.get(Calendar.YEAR) == firebaseDate.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == firebaseDate.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == firebaseDate.get(Calendar.DAY_OF_MONTH))
    }

    private fun setStartMarginAsPercentage(view: View, sibling: View, percentage: Int) {
        val clampedValue = when {
            percentage < 1 -> 1
            percentage > 96 -> 96
            else -> percentage
        }
        val siblingWidth = sibling.width
        val margin = (siblingWidth * clampedValue / 100)

        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = margin

        view.layoutParams = layoutParams
    }

    private fun showBMI(weight: Double, height: Double) {
        var bmi = weight / (Math.pow((height/100), 2.0)) // weight (kg) / [height (m)]^2
        bmi = "%.2f".format(bmi).toDouble()

        val percentageMargin = ((bmi / 50)*100).toInt() // Set the desired percentage
        setStartMarginAsPercentage(targetView, siblingView, percentageMargin)

        val rangeValue = when {
            bmi < 18.6 -> "underweight"
            bmi < 25 -> "healthy weight"
            bmi < 30 -> "overweight"
            bmi > 30 -> "obese"
            else -> "healthy weight"
        }
        val fullText = "Your current BMI is $bmi. You are within the $rangeValue range"
        val wordToColor = "$rangeValue"
        val spannableString = SpannableString(fullText)

        val startIndex = fullText.indexOf(wordToColor)
        val endIndex = startIndex + wordToColor.length

        val colorResourceId = when {
            bmi < 18.6 -> R.color.blue
            bmi < 25 -> R.color.green
            bmi < 30 -> R.color.orange
            bmi > 30 -> R.color.red
            else -> R.color.green
        }
        val color = context?.let { ContextCompat.getColor(it, colorResourceId) }
        spannableString.setSpan(color?.let { ForegroundColorSpan(it) }, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
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