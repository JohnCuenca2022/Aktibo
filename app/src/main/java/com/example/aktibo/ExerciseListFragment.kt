package com.example.aktibo

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ExerciseListFragment : Fragment() {
    private lateinit var intensity: String
    private lateinit var region: String

    private lateinit var query_region: String
    private lateinit var query_intensity: String

    private lateinit var userID: String

    var displayedUI = false

    var db = Firebase.firestore

    var canShowAddToRoutineDialog = true

    private lateinit var inflater: LayoutInflater
    private lateinit var marginLayoutParams: LinearLayout.LayoutParams
    var marginDim = 0
    private lateinit var fadeIn: Animation
    private lateinit var fadeOut: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ::inflater.set(LayoutInflater.from(requireContext()))

        marginLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.exer_item_height)
        )
        marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))

        marginDim = resources.getDimensionPixelSize(R.dimen.exer_item_height)

        fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        val bundle = arguments
        if (bundle != null) {
            intensity = bundle.getString("intensity").toString()
            region = bundle.getString("region").toString()

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_exercise_list, container, false)

        query_region = ""
        query_intensity = ""

        val textViewTitle = view.findViewById<TextView>(R.id.textViewTitle)
        val textViewHeader2 = view.findViewById<TextView>(R.id.textViewHeader2)
        when (region) {
            "upper" -> {
                textViewTitle.text = "Upper Body Exercises"
                textViewHeader2.text = "Upper Body Exercises"
                query_region = "Upper Body"
            }
            "lower" -> {
                textViewTitle.text = "Lower Body Exercises"
                textViewHeader2.text = "Lower Body Exercises"
                query_region = "Lower Body"
            }
            "whole" ->{
                textViewTitle.text = "Whole Body Exercises"
                textViewHeader2.text = "Whole Body Exercises"
                query_region = "Whole Body"
            }
            "pre-post" ->{
                textViewTitle.text = "Pre-Post Stretches"
                textViewHeader2.text = "Pre-Post Stretches"
                query_region = "Pre-Post"
            }
            "prevention" ->{
                textViewTitle.text = "Prevention Stretches"
                textViewHeader2.text = "Prevention Stretches"
                query_region = "Prevention"
            }
            else -> {
                textViewHeader2.text = "Exercises"
            }
        }

        when (intensity) {
            "stretching" -> {
                query_intensity = "Stretching"
            }
            "light" -> {
                query_intensity = "Light"
            }
            "moderate" -> {
                query_intensity = "Moderate"
            }
            "vigorous" ->{
                query_intensity = "Advanced"
            }
        }

        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid
            userID = uid
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val docRef = db.collection("users").document(userID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    showUserRoutines(document, userID)
                    showExercises()
                }
            }
    }

    private fun showUserRoutines(document: DocumentSnapshot, uid: String) {
        val routines = document.get("routines") as? ArrayList<Map<String, Any>> ?: ArrayList()
        if (routines.isEmpty()){ // user has no routines yet, create empty routine

            val emptyStringArray: ArrayList<String> = ArrayList()
            val routineData = mapOf(
                "name" to "My Routine",
                "routineList" to emptyStringArray
            )

            // add routine to user's routine list
            val userRef = db.collection("users").document(uid)
            userRef.update(
                "routines", FieldValue.arrayUnion(routineData)
            ).addOnSuccessListener {
                routines.add(routineData)

                // display routines to the user
                val linearLayout = view?.findViewById<LinearLayout>(R.id.yourRoutinesContainer)
                linearLayout?.removeAllViews()
                for (routine in routines){
                    val name = routine["name"] as String
                    val routineList = routine["routineList"] as ArrayList<Map<String, ArrayList<String>>>

                    // val inflater = LayoutInflater.from(requireContext())
                    val itemLayout = inflater.inflate(R.layout.exercise_routine_item, null)

                    val marginLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.exer_item_height)
                    )
                    marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))
                    itemLayout.layoutParams = marginLayoutParams

                    val exerciseName = itemLayout.findViewById<TextView>(R.id.exerciseName)
                    exerciseName.text = name

                    val exerciseInfo = itemLayout.findViewById<TextView>(R.id.exerciseInfo)
                    exerciseInfo.text = ""

                    itemLayout.setOnClickListener{
                        replaceFragmentToRoutine(RoutineFragment(), name, routineList, 0)
                    }

                    linearLayout?.addView(itemLayout)
                }
            }

        } else {

            val linearLayout = view?.findViewById<LinearLayout>(R.id.yourRoutinesContainer)
            linearLayout?.removeAllViews()
            for ((indexRoutine, routine) in routines.withIndex()){
                val name = routine["name"] as String
                val routineList = routine["routineList"] as ArrayList<Map<String, ArrayList<String>>>

                // val inflater = LayoutInflater.from(requireContext())
                val itemLayout = inflater.inflate(R.layout.exercise_routine_item, null)

//                val marginLayoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    resources.getDimensionPixelSize(R.dimen.exer_item_height)
//                )
//                marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))
                itemLayout.layoutParams = marginLayoutParams

                val exerciseName = itemLayout.findViewById<TextView>(R.id.exerciseName)
                exerciseName.text = name

                // display exercises
                val exer: ArrayList<String> = ArrayList()

                for (exercise in routineList) {
                    val exerciseNameString = exercise["exerciseName"] as? String ?: ""
                    exer.add(exerciseNameString)
                }

                var exerciseInfoString = ""
                var index = 0
                var wentOver = 0
                for (ex in exer){
                    if (index < 4){
                        if (index == 0){
                            exerciseInfoString += "${ex}"
                        } else {
                            exerciseInfoString += "\n${ex}"
                        }
                    } else {
                        wentOver++
                    }
                    index++
                }
                if (wentOver > 0){
                    exerciseInfoString += "...${wentOver} more"
                }

                val exerciseInfo = itemLayout.findViewById<TextView>(R.id.exerciseInfo)
                exerciseInfo.text = exerciseInfoString

                itemLayout.setOnClickListener{
                    replaceFragmentToRoutine(RoutineFragment(), name, routineList, indexRoutine)
                }

                linearLayout?.addView(itemLayout)
            }

        }
    }

    private fun replaceFragmentToRoutine(fragment: Fragment, name: String, routineList: ArrayList<Map<String, ArrayList<String>>>, index: Int) {
        val bundle = Bundle()
        bundle.putString("name", name)
        bundle.putInt("index", index)
        bundle.putSerializable("routineList", routineList)
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

    private fun showExercises(){
        val linearLayout = view?.findViewById<LinearLayout>(R.id.exercisesContainer)

        if (query_region == ""){
            db.collection("exercises")
                .whereEqualTo("intensity", query_intensity)
                .get()
                .addOnSuccessListener { queryDocumentSnapshots ->
                    val viewToRemove = view?.findViewById<ProgressBar>(R.id.progressBar2)
                    linearLayout?.removeView(viewToRemove)

                    if (queryDocumentSnapshots.isEmpty){
                        if (linearLayout != null) {
                            linearLayout.removeAllViews()
                            addTextViewToLinearLayout(linearLayout, "There are no exercises")
                        }
                    }

                    for (data in queryDocumentSnapshots){
                        val id = data.id
                        val name = data.getString("name").toString()
                        val reps_duration = data["reps_duration"].toString()
                        val sets: String =
                            try {
                                data.getDouble("sets")?.toInt()
                            } catch (e: Exception){
                                data.get("sets")?.toString()
                            }.toString()

                        val est_time = data["est_time"].toString()

                        val tags: ArrayList<String> = data.data.get("tags") as ArrayList<String>

                        var tagsString = ""
                        for ((index, tag) in tags.withIndex()) {
                            if (index == tags.size - 1) {
                                tagsString += "${tag}"
                            } else {
                                tagsString += "${tag}, "
                            }
                        }

                        // val inflater = LayoutInflater.from(requireContext())
                        val itemLayout = inflater.inflate(R.layout.exercise_item, null)

//                        val marginLayoutParams = LinearLayout.LayoutParams(
//                            LinearLayout.LayoutParams.MATCH_PARENT,
//                            resources.getDimensionPixelSize(R.dimen.exer_item_height)
//                        )
//                        marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))
                        itemLayout.layoutParams = marginLayoutParams

                        val nameView = itemLayout.findViewById<TextView>(R.id.exerciseName)
                        nameView.text = name

                        val exerciseInfo = itemLayout.findViewById<TextView>(R.id.exerciseInfo)
                        if (containsOnlyDigits(reps_duration)){
                            exerciseInfo.text = "${reps_duration} reps - ${sets} set(s)\nEst.: ${replaceTimeUnits(est_time)}"
                        } else {
                            exerciseInfo.text = "${replaceTimeUnits(reps_duration)} ${sets} set(s)\nEst.: ${replaceTimeUnits(est_time)}"
                        }

                        val exerciseTags = itemLayout.findViewById<TextView>(R.id.exerciseTags)
                        exerciseTags.text = tagsString

                        val addToRoutineButton = itemLayout.findViewById<ImageButton>(R.id.addToRoutineButton)
//                        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
//                        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

                        addToRoutineButton.setOnClickListener{
                            addToRoutineButton.startAnimation(fadeOut)

                            if (canShowAddToRoutineDialog){
                                showAddToRoutineDialog(name, id)
                            }


                            addToRoutineButton.startAnimation(fadeIn)
                        }

                        val layout = itemLayout.findViewById<LinearLayout>(R.id.layout)
                        layout.setOnClickListener{
                            replaceFragmentWithAnimWithData(ExerciseItemFragment(), id)
                        }

                        linearLayout?.addView(itemLayout)
                    }
                }
        } else {
            db.collection("exercises")
                .whereEqualTo("category", query_region)
                .whereEqualTo("intensity", query_intensity)
                .get()
                .addOnSuccessListener { queryDocumentSnapshots ->
                    val viewToRemove = view?.findViewById<ProgressBar>(R.id.progressBar2)
                    linearLayout?.removeView(viewToRemove)

                    if (queryDocumentSnapshots.isEmpty){
                        if (linearLayout != null) {
                            linearLayout.removeAllViews()
                            addTextViewToLinearLayout(linearLayout, "There are no exercises")
                        }
                    }

                    for (data in queryDocumentSnapshots){
                        val id = data.id
                        val name = data.getString("name").toString()
                        val reps_duration = data["reps_duration"].toString()
                        val sets: String =
                            try {
                                data.getDouble("sets")?.toInt()
                            } catch (e: Exception){
                                data.get("sets")?.toString()
                            }.toString()

                        val est_time = data["est_time"].toString()

                        val tags: ArrayList<String> = data.data.get("tags") as ArrayList<String>

                        var tagsString = ""
                        for ((index, tag) in tags.withIndex()) {
                            if (index == tags.size - 1) {
                                tagsString += "${tag}"
                            } else {
                                tagsString += "${tag}, "
                            }
                        }

                        // val inflater = LayoutInflater.from(requireContext())
                        val itemLayout = inflater.inflate(R.layout.exercise_item, null)

//                        val marginLayoutParams = LinearLayout.LayoutParams(
//                            LinearLayout.LayoutParams.MATCH_PARENT,
//                            resources.getDimensionPixelSize(R.dimen.exer_item_height)
//                        )
//                        marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))
                        itemLayout.layoutParams = marginLayoutParams

                        val nameView = itemLayout.findViewById<TextView>(R.id.exerciseName)
                        nameView.text = name

                        val exerciseInfo = itemLayout.findViewById<TextView>(R.id.exerciseInfo)
                        if (containsOnlyDigits(reps_duration)){
                            exerciseInfo.text = "${reps_duration} reps - ${sets} set(s)\nEst.: ${replaceTimeUnits(est_time)}"
                        } else {
                            exerciseInfo.text = "${replaceTimeUnits(reps_duration)} ${sets} set(s)\nEst.: ${replaceTimeUnits(est_time)}"
                        }

                        val exerciseTags = itemLayout.findViewById<TextView>(R.id.exerciseTags)
                        exerciseTags.text = tagsString

                        val addToRoutineButton = itemLayout.findViewById<ImageButton>(R.id.addToRoutineButton)

                        addToRoutineButton.setOnClickListener{
                            addToRoutineButton.startAnimation(fadeOut)

                            if (canShowAddToRoutineDialog){
                                showAddToRoutineDialog(name, id)
                            }


                            addToRoutineButton.startAnimation(fadeIn)
                        }

                        val layout = itemLayout.findViewById<LinearLayout>(R.id.layout)
                        layout.setOnClickListener{
                            replaceFragmentWithAnimWithData(ExerciseItemFragment(), id)
                        }

                        linearLayout?.addView(itemLayout)
                    }
                }
        }
    }

    private fun addTextViewToLinearLayout(linearLayout: LinearLayout, text: String) {
        val textView = TextView(linearLayout.context)
        textView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        textView.text = text
        textView.textSize = 16f
        textView.setTextColor(ContextCompat.getColor(linearLayout.context, android.R.color.white))

        val typeface = ResourcesCompat.getFont(linearLayout.context, R.font.roboto_bold)
        textView.typeface = typeface

        textView.gravity = Gravity.CENTER

        linearLayout.addView(textView)
    }

    fun showAddToRoutineDialog(exerciseName: String, exerciseID: String){
        canShowAddToRoutineDialog = false
        val docRef = db.collection("users").document(userID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val routines = document.get("routines") as ArrayList<Map<String, Any>>

                    val routinesNames: ArrayList<String> = ArrayList()
                    for (routine in routines){
                        val name = routine["name"] as String
                        routinesNames.add(name)
                    }
                    routinesNames.add("New Routine")

                    val view = inflater.inflate(R.layout.exercise_add_to_routine_dialog, null)

                    val routineSpinner: Spinner = view.findViewById(R.id.routineSpinner)
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        routinesNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    routineSpinner.adapter = adapter

                    val routineNameLayout = view.findViewById<ConstraintLayout>(R.id.routineNameLayout)

                    routineSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {

                            if (position == routinesNames.size-1) {
                                routineNameLayout.visibility = View.VISIBLE
                            } else {
                                routineNameLayout.visibility = View.GONE
                            }

                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Handle the case when nothing is selected (optional)
                        }
                    }

                    // Set up the dialog builder
                    val builder = MaterialAlertDialogBuilder(requireContext(),R.style.MaterialAlertDialog_App)
                        .setTitle("Add to a routine")
                        .setView(view)
                        .setPositiveButton("Confirm", null)
                        .setNegativeButton("Cancel") { dialog, which ->
                            // Handle negative button click
                            canShowAddToRoutineDialog = true
                            dialog.dismiss()
                        }
                        .setOnCancelListener{
                            canShowAddToRoutineDialog = true
                        }
                        .setOnDismissListener{
                            canShowAddToRoutineDialog = true
                        }

                    val dialog = builder.create()

                    val routineNameEditText = view.findViewById<EditText>(R.id.routineNameEditText)
                    dialog.setOnShowListener {
                        val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                        positiveButton.setOnClickListener {
                            canShowAddToRoutineDialog = true
                            val selected = routineSpinner.selectedItemId

                            if (selected.toInt() == routinesNames.size - 1){
                                val newRoutineName = routineNameEditText.text.toString().trim()

                                if (newRoutineName == ""){
                                    routineNameEditText.error = "Name cannot be empty"
                                    return@setOnClickListener
                                }

                                val exerciseData = mapOf(
                                    "exerciseName" to exerciseName,
                                    "exerciseID" to exerciseID
                                )

                                val routineList = ArrayList<Map<String, String>>()
                                routineList.add(exerciseData)

                                val data = mapOf(
                                    "name" to newRoutineName,
                                    "routineList" to routineList
                                )

                                val user = Firebase.auth.currentUser
                                user?.let {
                                    val uid = it.uid
                                    val db = Firebase.firestore
                                    val docRef = db.collection("users").document(uid)
                                    docRef.update(
                                        "routines", FieldValue.arrayUnion(data)
                                    ).addOnSuccessListener {
                                        Toast.makeText(context, "Created new routine", Toast.LENGTH_SHORT).show()
                                        showUpdatedData()
                                        dialog.dismiss()
                                    }.addOnFailureListener {
                                        Toast.makeText(context, "Failed to create new routine", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            } else {
                                val routine = routines[selected.toInt()] // selected routine
                                val name = routine["name"] as? String ?: ""
                                val routineList = routine["routineList"] as? ArrayList<Map<String, Any>>

                                val exerciseData = mapOf(
                                    "exerciseName" to exerciseName,
                                    "exerciseID" to exerciseID
                                )

                                routineList?.add(exerciseData)

                                val updatedRoutine = mapOf(
                                    "name" to name,
                                    "routineList" to routineList
                                )

                                // replace old routine data with updated data
                                routines[selected.toInt()] = updatedRoutine as Map<String, Any>

                                val userRef = db.collection("users").document(userID)
                                userRef.update(
                                    "routines", routines
                                ).addOnSuccessListener {
                                    Toast.makeText(context, "Added to $name", Toast.LENGTH_SHORT).show()
                                    showUpdatedData()
                                    dialog.dismiss()
                                }.addOnFailureListener {
                                    Toast.makeText(context, "Failed to add exercise", Toast.LENGTH_SHORT)
                                        .show()
                                }

                            }

                        }
                    }
                    dialog.show()
                }
            }
    }

    private fun showUpdatedData(){
        val docRef = db.collection("users").document(userID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    showUserRoutines(document, userID)
                }
            }
    }

    fun replaceTimeUnits(input: String): String {
        var result = input
        result = result.replace("minute", "min", ignoreCase = true)
        result = result.replace("seconds", "sec", ignoreCase = true)
        return result
    }

    private fun replaceFragmentWithAnimWithData(fragment: Fragment, exerciseID: String) {
        val bundle = Bundle()
        bundle.putString("exerciseID", exerciseID)

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

    fun containsOnlyDigits(input: String): Boolean {
        for (char in input) {
            if (!char.isDigit()) {
                return false
            }
        }
        return true
    }

}