package com.example.aktibo

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DetailedWeightGraphFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 222
    private lateinit var lineChart: LineChart
    private lateinit var datePickerButton: Button
    private lateinit var workbook: Workbook

    var canShowDatePicker = true
    var canShowGenerateReportDialog = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detailed_weight_graph, container, false)

        // Set the minimum date to a specific date (e.g., January 1, 2023)
        val minDate = Calendar.getInstance()
        minDate.set(2023, Calendar.AUGUST, 19)

        // Set the maximum date to today
        val maxDate = Calendar.getInstance()

        // Set/placeholder date
        val firstDate = Calendar.getInstance()
        firstDate.add(Calendar.DAY_OF_MONTH, -7)

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
            if (canShowDatePicker) {
                canShowDatePicker = false
                datePicker.show(parentFragmentManager, datePicker.toString())
            }
        }

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            val dateFormat = SimpleDateFormat("MMM d, yyyy")
            val start = Date(startDate)
            val startFormatted  = dateFormat.format(start)
            val end = Date(endDate)
            val endFormatted  = dateFormat.format(end)

            val linearLayout = view.findViewById<LinearLayout>(R.id.linearLayout)

            val textView2 = view.findViewById<TextView>(R.id.textView2)
            textView2.text = startFormatted

            val textView4 = view.findViewById<TextView>(R.id.textView4)
            textView4.text = endFormatted

            linearLayout.visibility = View.VISIBLE

            getWeightRecord(Date(startDate), Date(endDate))

            canShowDatePicker = true
        }

        datePicker.addOnDismissListener{
            canShowDatePicker = true
        }
        datePicker.addOnCancelListener{
            canShowDatePicker = true
        }

        // initialize barChart
        lineChart = view.findViewById(R.id.lineChart)

        val generateReportButton = view.findViewById<ImageButton>(R.id.generateReportButton)
        generateReportButton.setOnClickListener{
            if (canShowGenerateReportDialog){
                canShowGenerateReportDialog = false
                saveFile()
            }
        }

        return view
    }

    fun getWeightRecord(startDate: Date, endDate: Date) {

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
                        val weightRecordsList: ArrayList<Map<String, Any>> = ArrayList()

                        val weightRecords = document.get("weightRecords") as? ArrayList<Map<Any, Any>> ?: ArrayList()

                        val currentWeight = document.getDouble("weight") ?: 0.0
                        val numberOfDays = getNumberOfDays(startDate, endDate)
                        val daysDates = getDatesInRange(startDate, endDate)
                        if (weightRecords.isEmpty()){ // no entries
                            for (i in 0 until numberOfDays) { // loop 7 times
                                val date = daysDates[i]

                                val dataPoint = mutableMapOf(
                                    "date" to date,
                                    "weight" to currentWeight
                                )
                                weightRecordsList.add(dataPoint)
                            }
                        } else {
                            for (i in 0 until numberOfDays) { // loop 7 times
                                val day = daysDates[i]
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val dayString = sdf.format(day)

                                var hasDataPoint = false
                                for (map in weightRecords) {
                                    val mapTimestamp = map["date"] as Timestamp
                                    val weight = map["weight"] as Double
                                    val mapDate = sdf.format(mapTimestamp.toDate())
                                    if (mapDate == dayString) {
                                        val dataPoint = mutableMapOf(
                                            "date" to day,
                                            "weight" to weight
                                        )
                                        weightRecordsList.add(dataPoint)
                                        hasDataPoint = true
                                        break
                                    }
                                }

                                if(!hasDataPoint && i == 0){
                                    val dataPoint = mutableMapOf(
                                        "date" to day,
                                        "weight" to currentWeight
                                    )
                                    weightRecordsList.add(dataPoint)
                                } else if (!hasDataPoint){
                                    val previousWeight = weightRecordsList[i-1]["weight"] as Double
                                    val dataPoint = mutableMapOf(
                                        "date" to day,
                                        "weight" to previousWeight
                                    )
                                    weightRecordsList.add(dataPoint)
                                }

                            }
                        }

                        if (!weightRecordsList.isEmpty()){
                            updateChart(weightRecordsList)
                            createExcel(weightRecordsList)
                        }
                    }
                }
        }

    }

    fun getDatesInRange(startDate: Date, endDate: Date): List<Date> {
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        val datesInRange = mutableListOf<Date>()

        while (!calendar.time.after(endDate)) {
            datesInRange.add(calendar.time)
            calendar.add(Calendar.DATE, 1)
        }

        return datesInRange
    }

    fun getNumberOfDays(startDate: Date, endDate: Date): Int {
        // Calculate the difference in milliseconds
        val differenceInMillis = endDate.time - startDate.time

        // Convert milliseconds to days
        val daysDifference = (differenceInMillis / (1000 * 60 * 60 * 24)).toInt()

        // Add 1 to include both the start and end dates
        return daysDifference + 1
    }

    private fun updateChart(weightRecordsList: ArrayList<Map<String, Any>>) {
        val entries = ArrayList<Entry>()

        val weightArray = ArrayList<Double>()
        val dayArray = ArrayList<String>()

        for (weightRecord in weightRecordsList) {
            val date = weightRecord["date"] as Date
            val weight = weightRecord["weight"] as Double

            weightArray.add(weight)

            val dateFormat = SimpleDateFormat("MMM-d", Locale.getDefault())
            val day = dateFormat.format(date).toString()
            dayArray.add(day)
        }

        // Create data entries for the bar chart from the input data
        for ((index, value) in weightArray.withIndex()) {
            entries.add(BarEntry(index.toFloat(), value.toFloat()))
        }

        val set = LineDataSet(entries, "LineDataSet")
        set.color = Color.rgb(99,169,31)
        set.setCircleColor(R.color.green)

        val dataSet = LineData(set)
        //dataSet.barWidth = 0.9f; // set custom bar width
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.primary))

        // disable dragging/zooming
        // barChart.setTouchEnabled(false)

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
                return dayArray.getOrNull(value.toInt()) ?: value.toString()
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

    private fun createExcel(weightRecordsList: ArrayList<Map<String, Any>>){
        // Create a new Excel workbook
        workbook = HSSFWorkbook()

        // Create a sheet in the workbook
        val sheet: Sheet = workbook.createSheet("Sheet1")

        var index = 0

        for (weightRecord in weightRecordsList) {
            val date = weightRecord["date"] as Date
            val weight = weightRecord["weight"] as Double

            val row: Row = sheet.createRow(index)

            val dateFormat = SimpleDateFormat("MM/dd/YYYY", Locale.getDefault())
            val day = dateFormat.format(date).toString()

            // Create a cell in the row and set its value
            val cell: Cell = row.createCell(0)
            cell.setCellValue(day)

            // Create a cell in the row and set its value
            val cell2: Cell = row.createCell(1)
            cell2.setCellValue(weight.toString())

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
        canShowGenerateReportDialog = true
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
                        val fileName = "aktibo-weight_$currentDateAndTime.xls"

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
                        val pdfFileName = "aktibo-weight_$currentDateAndTime.pdf"

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