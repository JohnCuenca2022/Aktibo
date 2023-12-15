package com.example.aktibo

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
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
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfWriter
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
    private lateinit var datePickerButton: Button
    private lateinit var workbook: Workbook

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detailed_steps_graph, container, false)

        // Set the minimum date to a specific date (e.g., January 1, 2023)
        val minDate = Calendar.getInstance()
        minDate.set(2023, Calendar.AUGUST, 19)

        // Set the maximum date to today
        val maxDate = Calendar.getInstance()

        // Set/placeholder date
        val firstDate = Calendar.getInstance()
        firstDate.add(Calendar.DAY_OF_MONTH, -6)

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

        datePickerButton = view.findViewById(R.id.datePickerButton)
        datePickerButton.setOnClickListener{
            datePicker.show(parentFragmentManager, datePicker.toString())
        }

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            val start = Date(startDate)
            val end = Date(endDate)

            // show user the chosen dates
            val dateFormat = SimpleDateFormat("MMM d, yyyy")
            val startFormatted  = dateFormat.format(start)
            val endFormatted  = dateFormat.format(end)

            val linearLayout = view.findViewById<LinearLayout>(R.id.linearLayout)
            val textView2 = view.findViewById<TextView>(R.id.textView2)
            textView2.text = startFormatted
            val textView4 = view.findViewById<TextView>(R.id.textView4)
            textView4.text = endFormatted
            linearLayout.visibility = View.VISIBLE

            // get accurate time
            val helper = MyHelperFunctions()

            val startDateCalendar = Calendar.getInstance()
            startDateCalendar.timeInMillis = startDate
            val realStartDate = helper.startOfDay(startDateCalendar)

            val endDateCalendar = Calendar.getInstance()
            endDateCalendar.timeInMillis = endDate
            val realEndDate = helper.endOfDay(endDateCalendar)

            getDailyStepCount(requireContext(), realStartDate.time, realEndDate.time)

            //getDailyStepCount(requireContext(), startDateCalendar.time, endDateCalendar.time)
        }

        // initialize barChart
        barChart = view.findViewById(R.id.barChart)

//        val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)
//
//        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
//            GoogleSignIn.requestPermissions(
//                this, // your activity
//                819, // e.g. 1
//                account,
//                fitnessOptions)
//        } else {
//            accessGoogleFit()
//        }

        val generateReportButton = view.findViewById<ImageButton>(R.id.generateReportButton)
        generateReportButton.setOnClickListener{
            saveFile()
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

    fun getDailyStepCount(context: Context, startDate: Date, endDate: Date) {

        val googleSignInAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startDate.time, endDate.time, TimeUnit.MILLISECONDS)
            .build()

        val stepCountsMap = ArrayList<MutableMap<String, Any>>()

        val dataReadResult = Fitness.getHistoryClient(context, googleSignInAccount).readData(readRequest)
        dataReadResult.addOnSuccessListener { dataReadResponse ->
            if (dataReadResponse.buckets.isNotEmpty()) {
                for (bucket in dataReadResponse.buckets) {
                    for (dataset in bucket.dataSets) {
                        for (dataPoint in dataset.dataPoints) {
                            val date = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                            val stepCount = dataPoint.getValue(Field.FIELD_STEPS).asInt()
                            val realDate = Date(date)

                            println("*********************************")
                            println(stepCount)
                            println(realDate.toString())

                            var hasRecord = false
                            for (stepsRecord in stepCountsMap){
                                val date = stepsRecord["date"] as Date

                                if (isSameDate(realDate, date)){
                                    val currentSteps = stepsRecord["steps"] as Int
                                    stepsRecord["steps"] = currentSteps + stepCount
                                    hasRecord = true
                                }
                            }

                            if (hasRecord){
                                continue
                            }

                            val stepsRecord = mutableMapOf<String, Any>(
                                "date" to realDate,
                                "steps" to stepCount
                            )
                            stepCountsMap.add(stepsRecord)
                        }
                    }
                }
            }

            val firstDay = Calendar.getInstance()

            firstDay.time = startDate

            while (firstDay.time <= endDate) {
                val calendarDate = Date(firstDay.timeInMillis)

                var hasRecord = false
                for (stepsRecord in stepCountsMap){
                    val date = stepsRecord["date"] as Date

                    if (isSameDate(calendarDate, date)){
                        hasRecord = true
                    }
                }
                if (hasRecord){
                    firstDay.add(Calendar.DAY_OF_MONTH, 1)
                    continue
                }

                val newRecord = mutableMapOf<String, Any>(
                    "date" to calendarDate,
                    "steps" to 0
                )

                stepCountsMap.add(newRecord)

                firstDay.add(Calendar.DAY_OF_MONTH, 1)
            }

            // sort by date
            stepCountsMap.sortWith(compareBy { it["date"] as Date })

            updateChart(stepCountsMap)
            createExcel(stepCountsMap)

        }
    }

    fun isSameDate(firstDate: Date, secondDate: Date): Boolean {
        val firstDateCalendar = Calendar.getInstance()
        firstDateCalendar.time = firstDate

        val secondDateCalendar = Calendar.getInstance()
        secondDateCalendar.time = secondDate

        return (firstDateCalendar.get(Calendar.YEAR) == secondDateCalendar.get(
            Calendar.YEAR) &&
                firstDateCalendar.get(Calendar.MONTH) == secondDateCalendar.get(
            Calendar.MONTH) &&
                firstDateCalendar.get(Calendar.DAY_OF_MONTH) == secondDateCalendar.get(
            Calendar.DAY_OF_MONTH))
    }

    private fun updateChart(dailyStepCounts: ArrayList<MutableMap<String, Any>>) {
        val entries = ArrayList<BarEntry>()

        val stepArray = ArrayList<Int>()
        val dayArray = ArrayList<String>()

        for (stepsRecord in dailyStepCounts) {
            val date = stepsRecord["date"] as Date
            val steps = stepsRecord["steps"] as Int

            stepArray.add(steps)

            val dateFormat = SimpleDateFormat("MMM-d", Locale.getDefault())
            val day = dateFormat.format(date).toString()
            dayArray.add(day)
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

    private fun createExcel(dailyStepCounts: ArrayList<MutableMap<String, Any>>){
        // Create a new Excel workbook
        workbook = HSSFWorkbook()

        // Create a sheet in the workbook
        val sheet: Sheet = workbook.createSheet("Sheet1")

        var index = 0

        for (stepsRecord in dailyStepCounts) {
            val date = stepsRecord["date"] as Date
            val steps = stepsRecord["steps"] as Int

            val row: Row = sheet.createRow(index)

            val dateFormat = SimpleDateFormat("MM/dd/YYYY", Locale.getDefault())
            val day = dateFormat.format(date).toString()

            // Create a cell in the row and set its value
            val cell: Cell = row.createCell(0)
            cell.setCellValue(day)

            // Create a cell in the row and set its value
            val cell2: Cell = row.createCell(1)
            cell2.setCellValue(steps.toString())

            index++

        }

    }

    fun createPDFFromWorkbook(context: Context, workbook: Workbook, pdfFileName: String) {
        val externalStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        val pdfFilePath = File(externalStorageDir, pdfFileName).toString()

        val document = Document(PageSize.LETTER)
        val pdfWriter = PdfWriter.getInstance(document, FileOutputStream(pdfFilePath))
        document.open()

        val baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED)
        val contentByte: PdfContentByte = pdfWriter.directContent

        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)

            document.newPage()
            contentByte.setFontAndSize(baseFont, 12f)

            for (rowIndex in 0 until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(rowIndex)

                for (cellIndex in 0 until row.physicalNumberOfCells) {
                    val cell = row.getCell(cellIndex)

                    // Adjust x, y coordinates and other parameters as needed
                    contentByte.setTextMatrix(cellIndex.toFloat() * 100, document.top() - rowIndex * 20)
                    contentByte.showText(cell.toString())
                }
            }
        }

        document.close()

        // Use MediaStore to insert the PDF file into the database
        val contentValues = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, pdfFileName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val pdfUri = context.contentResolver.insert(contentUri, contentValues)

        pdfUri?.let {
            try {
                val outputStream = context.contentResolver.openOutputStream(pdfUri)
                if (outputStream != null) {
                    val inputStream = File(pdfFilePath).inputStream()
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveFile(){
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
            )
        } else {
            if (!::workbook.isInitialized){
                Toast.makeText(context, "Please select a date range", Toast.LENGTH_SHORT).show()
                return
            }

            val options = arrayOf("Save as Excel", "Save as PDF")
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Select File Type")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val currentDateAndTime = dateFormat.format(Date())
                        val fileName = "aktibo-steps_$currentDateAndTime.xls"

                        val contentValues = ContentValues().apply {
                            put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/vnd.ms-excel")
                        }

                        val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        val uri = requireContext().contentResolver.insert(contentUri, contentValues)

                        uri?.let {
                            try {
                                val outputStream = requireContext().contentResolver.openOutputStream(uri)
                                outputStream?.use { fileOutputStream ->
                                    workbook.write(fileOutputStream)
                                }

                                println("Success")
                                Toast.makeText(context, "File Successfully Created in Documents", Toast.LENGTH_SHORT).show()
                            } catch (e: IOException) {
                                e.printStackTrace()
                                Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    1 -> {
                        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val currentDateAndTime = dateFormat.format(Date())
                        val pdfFileName = "aktibo-steps_$currentDateAndTime.pdf"

                        try {
                            createPDFFromWorkbook(requireContext(), workbook, pdfFileName)
                            Toast.makeText(context, "File Successfully Created in Documents", Toast.LENGTH_SHORT).show()
                        } catch  (e: IOException) {
                            Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
                        }

                    }
                }
            }
            builder.show()
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
                    saveFile()
                } else {
                    // Permission denied, handle accordingly (e.g., show a message to the user)
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}