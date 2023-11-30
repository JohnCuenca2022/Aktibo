package com.example.aktibo

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DetailedStepsGraphFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 123
    private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 222
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY, FitnessOptions.ACCESS_READ)
        .build()
    private lateinit var barChart: BarChart
    private lateinit var textView2: TextView
    private lateinit var workbook: Workbook

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_detailed_steps_graph, container, false)

        // Set the minimum date to a specific date (e.g., January 1, 2023)
        val minDate = Calendar.getInstance()
        minDate.set(2023, Calendar.AUGUST, 19)

        // Set the maximum date to today
        val maxDate = Calendar.getInstance()

        // Set/placeholder date
        val firstDate = Calendar.getInstance()
        firstDate.add(Calendar.DAY_OF_MONTH, -7)

        // Create a Calendar instance for initial and end date selection
        val calendar = Calendar.getInstance()

        // Set up the date picker for range selection
        val builder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select a date range")
            .setSelection(
                androidx.core.util.Pair(firstDate.timeInMillis, maxDate.timeInMillis)
            )

        // Create custom DateValidator to restrict the selection
        val dateValidator = object : CalendarConstraints.DateValidator {
            override fun isValid(date: Long): Boolean {
                return date >= minDate.timeInMillis && date <= maxDate.timeInMillis
            }

            override fun writeToParcel(dest: android.os.Parcel, flags: Int) {
                // Implement writeToParcel as required
            }

            override fun describeContents(): Int {
                // Implement describeContents as required
                return 0
            }
        }

        // Create CalendarConstraints
        val constraintsBuilder = CalendarConstraints.Builder()
        constraintsBuilder.setStart(minDate.timeInMillis)
        constraintsBuilder.setEnd(maxDate.timeInMillis)
        constraintsBuilder.setValidator(dateValidator)

        builder.setCalendarConstraints(constraintsBuilder.build())

        val datePicker = builder.build()

        textView2 = view.findViewById(R.id.textView2)
        textView2.setOnClickListener{
            datePicker.show(parentFragmentManager, datePicker.toString())
        }

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            println(startDate)
            println(endDate)
            getDailySteps(startDate, endDate)

            // Handle selected dates
            // ...
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

        val generateReportButton = view.findViewById<ImageButton>(R.id.generateReportButton)
        generateReportButton.setOnClickListener{
            savePDF()
        }

        return view
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
                Log.i(ContentValues.TAG, "OnSuccess()")
                // getDailySteps()
            }
            .addOnFailureListener { e -> Log.d(ContentValues.TAG, "OnFailure()", e) }
    }

    private fun getDailySteps(startTime: Long, endTime: Long){
        val calendar = android.icu.util.Calendar.getInstance()
        calendar.time = Date()

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

                val dailyStepCounts = mutableMapOf<Long, Int>()

                for (bucket in dataReadResponse.buckets) {
                    val dataSet = bucket.dataSets.firstOrNull { it.dataType == DataType.TYPE_STEP_COUNT_DELTA }
                    if (dataSet != null) {
                        for (dataPoint in dataSet.dataPoints) {

                            val startTimeMillis = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                            val stepCount = dataPoint.getValue(Field.FIELD_STEPS).asInt()

                            // Get the date (in milliseconds since epoch) for this DataPoint
                            val dateMillis = getStartOfDayInMillis(startTimeMillis)

                            // Update the daily step count in the map
                            dailyStepCounts[dateMillis] = (dailyStepCounts[dateMillis] ?: 0) + stepCount

                            Log.i(ContentValues.TAG, Date(startTimeMillis).toString())
                            Log.i(ContentValues.TAG, stepCount.toString())

                        }
                        updateChart(dailyStepCounts)
                        createPDF(dailyStepCounts)
                    }
                }
            }
        }.addOnFailureListener { e ->
            // Handle the error
        }
    }

    // Helper function to get the start of the day in milliseconds
    private fun getStartOfDayInMillis(millis: Long): Long {
        return millis - millis % (24 * 60 * 60 * 1000)
    }

    private fun updateChart(dailyStepCounts: MutableMap<Long, Int>) {
        val entries = ArrayList<BarEntry>()

        val stepArray = ArrayList<Int>()
        val dayArray = ArrayList<String>()

        for ((dateMillis, totalSteps) in dailyStepCounts) {
            stepArray.add(totalSteps)

            val dateFormat = SimpleDateFormat("MMM-d", Locale.getDefault())
            val day = dateFormat.format(Date(dateMillis)).toString()
            dayArray.add(day)

            println("Day: $day, Total Steps: $totalSteps")
        }


        // Create data entries for the bar chart from the input data
        for ((index, value) in stepArray.withIndex()) {
            entries.add(BarEntry(index.toFloat(), value.toFloat()))
        }

        val set = BarDataSet(entries, "BarDataSet")
        set.color = Color.rgb(99,169,31)

        val dataSet = BarData(set)
        dataSet.barWidth = 0.9f; // set custom bar width
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.primary))

        // disable dragging/zooming
        // barChart.setTouchEnabled(false)

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
                return dayArray.getOrNull(value.toInt()) ?: value.toString()
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

    private fun createPDF(dailyStepCounts: MutableMap<Long, Int>){
        // Create a new Excel workbook
        workbook = HSSFWorkbook()

        // Create a sheet in the workbook
        val sheet: Sheet = workbook.createSheet("Sheet1")

        var index = 0
        for ((dateMillis, totalSteps) in dailyStepCounts) {
            // Create a row in the sheet
            val row: Row = sheet.createRow(index)

            val dateFormat = SimpleDateFormat("MM/dd/YYYY", Locale.getDefault())
            val day = dateFormat.format(Date(dateMillis)).toString()

            // Create a cell in the row and set its value
            val cell: Cell = row.createCell(0)
            cell.setCellValue(day)

            // Create a cell in the row and set its value
            val cell2: Cell = row.createCell(1)
            cell2.setCellValue(totalSteps.toString())

            index++
        }

    }

    private fun savePDF(){
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
            )
        } else {
            // Permission is already granted, proceed with Excel file creation
            // Save the workbook to a file
            val folderName = "Aktibo"
            val fileName = "text.xls"
            val externalStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val folder = File(externalStorageDir, folderName)
            if (!folder.exists()) {
                folder.mkdirs()
            }

            // Create the Excel file inside the folder
            val excelFile = File(folder, fileName)

            try {
                val fileOutputStream = FileOutputStream(excelFile)
                workbook.write(fileOutputStream)
                fileOutputStream.close()
                println("Success")
                Toast.makeText(context, "File Successfully Created", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                println("failed")
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with Excel file creation
                    savePDF()
                } else {
                    // Permission denied, handle accordingly (e.g., show a message to the user)
                }
            }
        }
    }
}