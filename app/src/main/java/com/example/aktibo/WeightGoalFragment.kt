package com.example.aktibo

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeightGoalFragment : Fragment() {

    private lateinit var tableLayout: TableLayout
    private lateinit var createNewRecordButton: Button
    private lateinit var textViewNoRecords: TextView
    private lateinit var weightGoalButton: ImageButton

    private lateinit var records: ArrayList<Map<Any, Any>>

    var canShowCreateNewRecordDialog = true
    var canShowChangeWeightGoalDialog = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_weight_goal, container, false)

        // create new weight record
        createNewRecordButton = view.findViewById(R.id.createNewRecordButton)
        createNewRecordButton.setOnClickListener{
            if (canShowCreateNewRecordDialog){
                canShowCreateNewRecordDialog = false
                showNewRecordDialog()
            }

        }

        // load press animations
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        weightGoalButton = view.findViewById(R.id.weightGoalButton)
        weightGoalButton.setOnClickListener {
            // Apply fadeOut animation when pressed
            weightGoalButton.startAnimation(fadeOut)

            if (canShowChangeWeightGoalDialog){
                canShowChangeWeightGoalDialog = false
                showChangeWeightGoalDialog()
            }

            // Apply fadeIn animation when released
            weightGoalButton.startAnimation(fadeIn)
        }

        tableLayout = view.findViewById(R.id.tableLayout)
        textViewNoRecords = view.findViewById(R.id.textViewNoRecords)

        showWeightRecords()

        return view
    }

    private fun showWeightRecords() {
        val recordInflater = LayoutInflater.from(requireContext())

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
                        // weight record table
                        val weightRecords = document.get("weightRecords") as? ArrayList<Map<Any, Any>> ?: ArrayList()

                        records = weightRecords

                        weightRecords.sortWith(Comparator { map1, map2 ->
                            // Assuming the field in the map is named "timestamp"
                            val timestamp1 = map1["date"] as? Timestamp
                            val timestamp2 = map2["date"] as? Timestamp

                            // Add null checks to handle potential null values
                            if (timestamp1 == null || timestamp2 == null) {
                                return@Comparator 0 // Handle as needed
                            }

                            // Compare the Firebase Timestamps
                            timestamp1.compareTo(timestamp2)
                        })

                        weightRecords.reverse()

                        if (!weightRecords.isEmpty()){
                            tableLayout.removeAllViews()
                            textViewNoRecords.visibility = View.GONE
                        }

                        for (record in weightRecords){
                            val date = record["date"] as Timestamp
                            val weightInKg = record["weight"].toString()

                            val weightInLbs = convertKgToLbs(weightInKg.toDouble())

                            val dateString = formatDateToString(date.toDate())
                            val weightString = "${formatDouble(weightInKg.toDouble())}kg / ${formatDouble(weightInLbs)}lbs"

                            val tableRow = recordInflater.inflate(R.layout.weight_record_tablerow_layout, null)

                            val weightRecordDate: TextView = tableRow.findViewById(R.id.weightRecordDate)
                            val weightRecordWeight: TextView = tableRow.findViewById(R.id.weightRecordWeight)

                            weightRecordDate.text = dateString
                            weightRecordWeight.text = weightString

                            tableLayout.addView(tableRow)
                        }
                    }
                }
        }
    }

    private fun showNewRecordDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.create_new_record_dialog, null)

        var date = Calendar.getInstance()
        val textViewDate = view.findViewById<TextView>(R.id.textViewDate)
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formattedDate = sdf.format(date.time)
        textViewDate.text = formattedDate

        val datePickerLayout = view.findViewById<ConstraintLayout>(R.id.datePicker)
        datePickerLayout.setOnClickListener{
            val builder = MaterialDatePicker.Builder.datePicker()
            builder.setTitleText("Select a Date")

            // Set the current date as the maximum date
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build()
            builder.setCalendarConstraints(constraints)

            val datePicker = builder.build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection

                date = calendar
                val newFormattedDate = sdf.format(date.time)
                textViewDate.text = newFormattedDate
            }

            datePicker.show(parentFragmentManager, datePicker.toString())
        }

        val weightSpinner: Spinner = view.findViewById(R.id.weightSpinner)
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.weight_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        weightSpinner.adapter = adapter

        // Set up the dialog builder
        val builder = MaterialAlertDialogBuilder(requireContext(),R.style.MaterialAlertDialog_App)
            .setTitle("Create a New Weight Record")
            .setView(view)
            .setPositiveButton("Confirm", null)
            .setNegativeButton("Cancel") { dialog, which ->
                // Handle negative button click
                dialog.dismiss()
                canShowCreateNewRecordDialog = true
            }
            .setOnCancelListener{
                canShowCreateNewRecordDialog = true
            }
            .setOnDismissListener{
                canShowCreateNewRecordDialog = true
            }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                canShowCreateNewRecordDialog = true
                val weightEditText = view.findViewById<EditText>(R.id.weightEditText)

                if (weightEditText.text.toString().trim() == ""){
                    weightEditText.error = "Weight cannot be empty"
                    return@setOnClickListener
                } else if (!isNumeric(weightEditText.text.toString().trim())){
                    weightEditText.error = "Please enter a valid number"
                    return@setOnClickListener
                } else if (hasSameDate(records,Timestamp(date.time))) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Warning")
                        .setMessage("A record for this date already exists. Would you like to overwrite the record?")
                        .setNegativeButton("Cancel") { dialog2, which ->
                            dialog2.dismiss()
                        }
                        .setPositiveButton("Continue") { dialog2, which ->
                            var weightInKg = weightEditText.text.toString().trim().toDouble()

                            val currentSelectedValue = weightSpinner.selectedItem.toString()
                            if (currentSelectedValue != "kg"){ // convert to kg
                                weightInKg = weightInKg * 0.453592
                            }

                            val recordToBeReplaced = records[indexOfSameDate(records,Timestamp(date.time))]
                            val dataToBeReplaced = mapOf(
                                "date" to recordToBeReplaced["date"],
                                "weight" to recordToBeReplaced["weight"]
                            )

                            val data = mapOf(
                                "date" to Timestamp(date.time),
                                "weight" to weightInKg
                            )

                            val db = Firebase.firestore
                            // get user
                            val user = Firebase.auth.currentUser
                            val userID = user?.uid
                            val userRef = userID?.let { it1 -> db.collection("users").document(it1) }
                            if (userRef != null) {

                                if (areDatesEqual(date.time, Calendar.getInstance().time)) { // current Date
                                    userRef.update(
                                        "weightRecords", FieldValue.arrayUnion(data),
                                        "weightRecords", FieldValue.arrayRemove(dataToBeReplaced),
                                        "weight", weightInKg
                                    )
                                        .addOnSuccessListener {
                                            Toast.makeText(requireContext(), "New Record Created", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            showWeightRecords()
                                        }.addOnFailureListener{
                                            Toast.makeText(requireContext(), "Error creating new record.", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                        }
                                } else {
                                    userRef.update(
                                        "weightRecords", FieldValue.arrayUnion(data),
                                        "weightRecords", FieldValue.arrayRemove(dataToBeReplaced)
                                    )
                                        .addOnSuccessListener {
                                            Toast.makeText(requireContext(), "New Record Created", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            showWeightRecords()
                                        }.addOnFailureListener{
                                            Toast.makeText(requireContext(), "Error creating new record.", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                        }
                                }


                            }
                        }
                        .show()
                } else {
                    var weightInKg = weightEditText.text.toString().trim().toDouble()

                    val currentSelectedValue = weightSpinner.selectedItem.toString()
                    if (currentSelectedValue != "kg"){ // convert to kg
                        weightInKg = weightInKg * 0.453592
                    }

                    val data = mapOf(
                        "date" to Timestamp(date.time),
                        "weight" to weightInKg
                    )

                    val db = Firebase.firestore
                    // get user
                    val user = Firebase.auth.currentUser
                    val userID = user?.uid
                    val userRef = userID?.let { it1 -> db.collection("users").document(it1) }
                    if (userRef != null) {

                        if (areDatesEqual(date.time, Calendar.getInstance().time)) { // current Date
                            userRef.update(
                                "weightRecords", FieldValue.arrayUnion(data),
                                "weight", weightInKg,
                            )
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "New Record Created", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                    showWeightRecords()
                                }.addOnFailureListener{
                                    Toast.makeText(requireContext(), "Error creating new record.", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                }
                        } else {
                            userRef.update(
                                "weightRecords", FieldValue.arrayUnion(data)
                            )
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "New Record Created", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                    showWeightRecords()
                                }.addOnFailureListener{
                                    Toast.makeText(requireContext(), "Error creating new record.", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                }
                        }


                    }
                }
            }
        }
        dialog.show()
    }

    private fun showChangeWeightGoalDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.change_weight_goal_dialog, null)

        val db = Firebase.firestore
        val user = Firebase.auth.currentUser
        val userID = user?.uid
        val docRef = userID?.let { db.collection("users").document(it) }
        if (docRef != null) {
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val weightGoalID = document.getDouble("weightGoal")?.toInt() ?: 0
                        val targetWeight = document.get("targetWeight") as? Double ?: 0.0
                        val weight = document.getDouble("weight") ?: 0.0

                        val weightSpinner: Spinner = view.findViewById(R.id.weightSpinner)
                        val adapter = ArrayAdapter.createFromResource(
                            requireContext(),
                            R.array.weight_array,
                            android.R.layout.simple_spinner_item
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        weightSpinner.adapter = adapter

                        val weightGoalSpinner: Spinner = view.findViewById(R.id.weightGoalSpinner)
                        val adapter2 = ArrayAdapter.createFromResource(
                            requireContext(),
                            R.array.weight_goal_array,
                            android.R.layout.simple_spinner_item
                        )
                        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        weightGoalSpinner.adapter = adapter2

                        val targetWeightEditText = view.findViewById<EditText>(R.id.targetWeightEditText)
                        val targetWeightLayout = view.findViewById<ConstraintLayout>(R.id.targetWeightLayout)

                        weightGoalSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                                val selectedItem = parent?.getItemAtPosition(position).toString()
                                if (selectedItem == "Maintain Weight") {
                                    targetWeightLayout.alpha = 0.5f
                                    targetWeightEditText.isEnabled = false
                                    weightSpinner.isEnabled = false
                                } else {
                                    targetWeightLayout.alpha = 1.0f
                                    targetWeightEditText.isEnabled = true
                                    weightSpinner.isEnabled = true
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                // Handle the case where nothing is selected if needed
                            }
                        })
                        weightGoalSpinner.setSelection(weightGoalID)


                        targetWeightEditText.setText(formatDouble(targetWeight))

                        // Set up the dialog builder
                        val builder = MaterialAlertDialogBuilder(requireContext(),R.style.MaterialAlertDialog_App)
                            .setTitle("Change Weight Goal")
                            .setView(view)
                            .setPositiveButton("Confirm", null)
                            .setNegativeButton("Cancel") { dialog, which ->
                                // Handle negative button click
                                dialog.dismiss()
                                canShowChangeWeightGoalDialog = true
                            }
                            .setOnCancelListener {
                                canShowChangeWeightGoalDialog = true
                            }
                            .setOnDismissListener {
                                canShowChangeWeightGoalDialog = true
                            }

                        val dialog = builder.create()

                        dialog.setOnShowListener {
                            val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                            positiveButton.setOnClickListener {
                                canShowChangeWeightGoalDialog = true
                                val newTargetWeight = targetWeightEditText.text.toString().trim()
                                if (newTargetWeight == ""){
                                    targetWeightEditText.error = "Weight cannot be empty"
                                    return@setOnClickListener
                                } else if (!isNumeric(newTargetWeight)) {
                                    targetWeightEditText.error = "Please enter a valid number"
                                    return@setOnClickListener
                                } else {
                                    val newWeightGoal = weightGoalSpinner.selectedItemPosition

                                    var newTargetWeightValue = newTargetWeight.toDouble()
                                    if (weightSpinner.selectedItem == "lbs"){
                                        newTargetWeightValue *= 0.453592
                                    }

                                    if (newWeightGoal == 0){
                                        newTargetWeightValue = weight
                                    }

                                    if (newWeightGoal == 1 && newTargetWeightValue < weight){ // gain
                                        targetWeightEditText.error = "Weight goal must be greater than current weight"
                                        return@setOnClickListener
                                    }

                                    if (newWeightGoal == 2 && newTargetWeightValue > weight){ // lose
                                        targetWeightEditText.error = "Weight goal must be less than current weight"
                                        return@setOnClickListener
                                    }

                                    val userRef = userID.let { it1 -> db.collection("users").document(it1) }
                                    userRef.update(
                                        "weightGoal", newWeightGoal,
                                        "targetWeight", newTargetWeightValue)
                                        .addOnSuccessListener {
                                            Toast.makeText(requireContext(), "Weight Goal Updated", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                        }.addOnFailureListener{
                                            Toast.makeText(requireContext(), "Error updating weight goal.", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                        }
                                }
                            }
                        }
                        dialog.show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Could not load user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun convertKgToLbs(kilograms: Double): Double {
        // Conversion factor: 1 kg = 2.20462 lbs
        val pounds = kilograms * 2.20462

        // Round to 2 decimal places
        return "%.2f".format(pounds).toDouble()
    }

    fun formatDouble(num: Double): String {
        return "%.2f".format(num)
    }

    fun formatDateToString(date: Date): String {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }

    fun hasSameDate(list: ArrayList<Map<Any, Any>>, timestamp: Timestamp): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val checkDate = sdf.format(timestamp.toDate())

        for (map in list) {
            val mapTimestamp = map["date"] as? Timestamp
            if (mapTimestamp != null) {
                val mapDate = sdf.format(mapTimestamp.toDate())
                if (mapDate == checkDate) {
                    return true
                }
            }
        }

        return false
    }

    fun indexOfSameDate(list: ArrayList<Map<Any, Any>>, timestamp: Timestamp): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val checkDate = sdf.format(timestamp.toDate())

        var index = 0
        for (map in list) {
            val mapTimestamp = map["date"] as? Timestamp
            if (mapTimestamp != null) {
                val mapDate = sdf.format(mapTimestamp.toDate())
                if (mapDate == checkDate) {
                    return index
                }
            }
            index++
        }

        return 0
    }

    fun isNumeric(str: String): Boolean {
        return str.toDoubleOrNull() != null
    }

    fun areDatesEqual(date1: Date, date2: Date): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Format the dates as strings
        val dateString1 = sdf.format(date1)
        val dateString2 = sdf.format(date2)

        // Compare the formatted date strings for equality
        return dateString1 == dateString2
    }
}