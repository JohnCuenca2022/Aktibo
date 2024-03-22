package com.example.aktibo

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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
import androidx.core.content.ContextCompat
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

class ExerciseGoalFragment : Fragment() {

    private lateinit var tableLayout: TableLayout
    private lateinit var textViewNoRecords: TextView
    private lateinit var exerciseGoalButton: ImageButton

    var canShowChangeExerciseGoalDialog = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_exercise_goal, container, false)

        // load press animations
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        val textViewExerciseRecordsInfo = view.findViewById<TextView>(R.id.textViewExerciseRecordsInfo)
        val fullText = "Perform exercise routines to create new records. Exercises marked with green are a part of a routine."
        val wordToColor = "green"
        val spannableString = SpannableString(fullText)

        val startIndex = fullText.indexOf(wordToColor)
        val endIndex = startIndex + wordToColor.length

        val colorResourceId = R.color.green
        val color = context?.let { ContextCompat.getColor(it, colorResourceId) }
        spannableString.setSpan(color?.let { ForegroundColorSpan(it) }, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        textViewExerciseRecordsInfo.text = spannableString

        exerciseGoalButton = view.findViewById(R.id.exerciseGoalButton)
        exerciseGoalButton.setOnClickListener {
            // Apply fadeOut animation when pressed
            exerciseGoalButton.startAnimation(fadeOut)

            if (canShowChangeExerciseGoalDialog){
                canShowChangeExerciseGoalDialog = false
                showChangeExerciseGoalDialog()
            }

            // Apply fadeIn animation when released
            exerciseGoalButton.startAnimation(fadeIn)
        }

        tableLayout = view.findViewById(R.id.tableLayout)
        textViewNoRecords = view.findViewById(R.id.textViewNoRecords)

        showExerciseRecords()

        return view
    }

    private fun showExerciseRecords() {
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
                        // exercise record table
                        val exerciseRecords = document.get("exerciseRecords") as? ArrayList<Map<Any, Any>> ?: ArrayList()

                        exerciseRecords.sortWith(Comparator { map1, map2 ->
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

                        exerciseRecords.reverse()

                        if (!exerciseRecords.isEmpty()){
                            tableLayout.removeAllViews()
                            textViewNoRecords.visibility = View.GONE
                        }

                        for (record in exerciseRecords){
                            val date = record["date"] as Timestamp
                            val exerciseID = record["exerciseID"] as String
                            val exerciseName = record["exerciseName"] as? String ?: ""
                            val isPartOfRoutine = record["isPartOfRoutine"] as? Boolean ?: false

                            val dateString = formatDateToString(date.toDate())

                            val tableRow = recordInflater.inflate(R.layout.exercise_record_tablerow_layout, null)

                            val exerciseRecordDate: TextView = tableRow.findViewById(R.id.exerciseRecordDate)
                            val exerciseRecordName: TextView = tableRow.findViewById(R.id.exerciseRecordName)

                            exerciseRecordDate.text = dateString

                            if (isPartOfRoutine) {
                                val spannableString = SpannableString(exerciseName)
                                val colorResourceId = R.color.green
                                val color = context?.let { ContextCompat.getColor(it, colorResourceId) }
                                spannableString.setSpan(color?.let { ForegroundColorSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                exerciseRecordName.text = spannableString
                            } else {
                                exerciseRecordName.text = exerciseName
                            }

                            tableLayout.addView(tableRow)
                        }
                    }
                }
        }
    }

    private fun showChangeExerciseGoalDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.change_exercise_goal_dialog, null)

        val db = Firebase.firestore
        val user = Firebase.auth.currentUser
        val userID = user?.uid
        val docRef = userID?.let { db.collection("users").document(it) }
        if (docRef != null) {
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val exerciseGoalID = document.getDouble("exerciseGoal")?.toInt() ?: 0

                        val exerciseGoalSpinner: Spinner = view.findViewById(R.id.exerciseGoalSpinner)
                        val adapter = ArrayAdapter.createFromResource(
                            requireContext(),
                            R.array.exercise_goal_array,
                            android.R.layout.simple_spinner_item
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        exerciseGoalSpinner.adapter = adapter

                        exerciseGoalSpinner.setSelection(exerciseGoalID)

                        // Set up the dialog builder
                        val builder = MaterialAlertDialogBuilder(requireContext(),R.style.MaterialAlertDialog_App)
                            .setTitle("Change Exercise Goal")
                            .setView(view)
                            .setPositiveButton("Confirm", null)
                            .setNegativeButton("Cancel") { dialog, which ->
                                // Handle negative button click
                                dialog.dismiss()
                                canShowChangeExerciseGoalDialog = true
                            }
                            .setOnCancelListener {
                                canShowChangeExerciseGoalDialog = true
                            }
                            .setOnDismissListener {
                                canShowChangeExerciseGoalDialog = true
                            }

                        val dialog = builder.create()

                        dialog.setOnShowListener {
                            val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                            positiveButton.setOnClickListener {
                                canShowChangeExerciseGoalDialog = true

                                val newExerciseGoal = exerciseGoalSpinner.selectedItemPosition

                                val userRef = userID.let { it1 -> db.collection("users").document(it1) }
                                userRef.update("exerciseGoal", newExerciseGoal)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Exercise Goal Updated", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                    }.addOnFailureListener{
                                        Toast.makeText(requireContext(), "Error updating exercise goal.", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
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

    fun formatDateToString(date: Date): String {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }
}