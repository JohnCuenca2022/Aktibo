package com.example.aktibo

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates


class RoutineFragment : Fragment() {

    private lateinit var name: String
    private var index: Int = 0
    private lateinit var routineList: ArrayList<Map<String, Any>>

    private lateinit var textViewRoutineName: TextView

    private lateinit var exercisesContainer: LinearLayout
    private lateinit var deleteRoutineImageButton: ImageButton
    private lateinit var editRoutineImageButton: ImageButton
    private lateinit var saveRoutineImageButton: ImageButton

    private lateinit var parentLayout: ConstraintLayout

    private lateinit var userID: String

    var canShowDeleteRoutineDialog = true

    var db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_routine, container, false)

        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid
            userID = uid
        }

        val bundle = arguments
        if (bundle != null) {
            name = bundle.getString("name").toString()
            index = bundle.getInt("index")
            routineList = bundle.getSerializable("routineList") as ArrayList<Map<String, Any>>
        }

        parentLayout = view.findViewById(R.id.parentLayout)


        textViewRoutineName = view.findViewById(R.id.textViewRoutineName)
        val routineNameEditText = view.findViewById<EditText>(R.id.routineNameEditText)

        // textViewRoutineName.text = name

        editRoutineImageButton = view.findViewById(R.id.editRoutineImageButton)
        editRoutineImageButton.setOnClickListener{
            editRoutineImageButton.visibility = View.GONE
            saveRoutineImageButton.visibility = View.VISIBLE

            textViewRoutineName.visibility = View.GONE
            routineNameEditText.setText(textViewRoutineName.text)
            routineNameEditText.visibility = View.VISIBLE
            // routineNameEditText.requestFocus()

            for (i in 0 until exercisesContainer.childCount) {
                val childView: View = exercisesContainer.getChildAt(i)

                if (childView is TextView){
                    continue
                }
                if (childView is ProgressBar){
                    continue
                }

                val removeButton = childView.findViewById<ImageButton>(R.id.removeFromRoutineButton)
                if (removeButton != null){
                    removeButton.visibility = View.VISIBLE
                }
            }
        }

        saveRoutineImageButton = view.findViewById(R.id.saveRoutineImageButton)
        saveRoutineImageButton.setOnClickListener{
            saveRoutineImageButton.visibility = View.GONE
            editRoutineImageButton.visibility = View.VISIBLE

            textViewRoutineName.text = routineNameEditText.text.toString().trim()

            textViewRoutineName.visibility = View.VISIBLE
            routineNameEditText.visibility = View.GONE

            updateRoutineName(routineNameEditText.text.toString().trim())

            for (i in 0 until exercisesContainer.childCount) {
                val childView: View = exercisesContainer.getChildAt(i)

                if (childView is TextView){
                    continue
                }
                if (childView is ProgressBar){
                    continue
                }

                val removeButton = childView.findViewById<ImageButton>(R.id.removeFromRoutineButton)
                if (removeButton != null){
                    removeButton.visibility = View.GONE
                }

            }
        }


        deleteRoutineImageButton = view.findViewById(R.id.deleteRoutineImageButton)
        deleteRoutineImageButton.setOnClickListener{
            showPopupMenu(deleteRoutineImageButton)
        }

        exercisesContainer = view.findViewById(R.id.exercisesContainer)

        showExercises()

        return view
    }

    private fun showPopupMenu(view: ImageButton) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.routine_menu) // Create a menu resource file (e.g., res/menu/popup_menu.xml)
        0

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_option1 -> {
                    if (canShowDeleteRoutineDialog){
                        showDeleteRoutineDialog()
                    }

                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showExercises(){
        val docRef = db.collection("users").document(userID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val routines = document.get("routines") as ArrayList<Map<String, Any>>

                    val routine = routines[index] // selected routine

                    val routineName = routine["name"] as? String ?: ""
                    val routineList = routine["routineList"] as? ArrayList<Map<String, Any>> ?: ArrayList()

                    textViewRoutineName.text = routineName
                    name = routineName

                    ::routineList.set(routineList)

                    if (routineList.isEmpty()){
                        exercisesContainer.removeAllViews()
                        addTextViewToLinearLayout(exercisesContainer, "There are no exercises")
                    } else {
                        exercisesContainer.removeAllViews()
                        val documentIds: MutableList<String> = mutableListOf()
                        for ((index, routine) in routineList.withIndex()) {
                            val exerciseID = routine["exerciseID"] as? String ?: ""

                            documentIds.add(exerciseID)
                        }

                        documentIds.add("asdf")
                        try {
                            CoroutineScope(Dispatchers.IO).launch {
                                val results = async {
                                    getDocumentsInOrder(documentIds)
                                }.await()
                                for ((index, result) in results.withIndex()) {
                                    println("Document ${documentIds[index]} data: $result")

                                    val id = result.id
                                    val name = result.getString("name").toString()
                                    val reps_duration = result["reps_duration"].toString()
                                    val sets = result.getDouble("sets")?.toInt().toString()
                                    val est_time = result["est_time"].toString()

                                    val tags: ArrayList<String> = result.get("tags") as ArrayList<String>

                                    var tagsString = ""
                                    for ((index, tag) in tags.withIndex()) {
                                        if (index == tags.size - 1) {
                                            tagsString += "${tag}"
                                        } else {
                                            tagsString += "${tag}, "
                                        }
                                    }

                                    val inflater = LayoutInflater.from(requireContext())
                                    val itemLayout = inflater.inflate(R.layout.exercise_item_routine, null)

                                    val marginLayoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        resources.getDimensionPixelSize(R.dimen.exer_item_height)
                                    )
                                    marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))
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

                                    val removeFromRoutineButton = itemLayout.findViewById<ImageButton>(R.id.removeFromRoutineButton)
                                    val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                                    val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

                                    removeFromRoutineButton.setOnClickListener{
                                        removeFromRoutineButton.startAnimation(fadeOut)

                                        removeFromRoutine(name, id)
                                        exercisesContainer.removeView(itemLayout)

                                        removeFromRoutineButton.startAnimation(fadeIn)
                                    }

                                    val layout = itemLayout.findViewById<LinearLayout>(R.id.layout)
                                    layout.setOnClickListener{
                                        replaceFragmentWithAnimWithData(ExerciseItemFragment(), id, ::routineList.get())
                                    }

                                    withContext(Dispatchers.Main) {
                                        exercisesContainer.addView(itemLayout)
                                    }

                                }
                            }

                        } catch (e: Exception) {
                            // Handle exceptions
                            println("Error fetching documents: ${e.message}")
                            Toast.makeText(context, "Error loading exercises", Toast.LENGTH_SHORT).show()
                        }


                        }
                    }
                }
    }

    suspend fun getDocumentsInOrder(documentIds: List<String>): List<DocumentSnapshot> {
        return coroutineScope {
            val db = FirebaseFirestore.getInstance()

            val results = mutableListOf<DocumentSnapshot>()

            for (documentId in documentIds) {
                val docRef = db.collection("exercises").document(documentId)
                val documentSnapshot = docRef.get().await()

                if (documentSnapshot.exists()) {
                    results.add(documentSnapshot)
                } else {
                    // Handle the case where the document doesn't exist
                    // results.add(documentSnapshot)
                }
            }

            results
        }
    }

    private fun updateRoutineName(newRoutineName: String){
        if (newRoutineName.trim() == ""){
            Toast.makeText(context, "Routine name cannot be empty", Toast.LENGTH_SHORT).show()
            textViewRoutineName.text = name
            return
        }

        val docRef = db.collection("users").document(userID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val routines = document.get("routines") as ArrayList<Map<String, Any>>

                    val routine = routines[index] // selected routine
                    val routineName = routine["name"] as? String ?: ""
                    val routineList = routine["routineList"] as? ArrayList<Map<String, Any>>

                    val updatedRoutine = mapOf(
                        "name" to newRoutineName,
                        "routineList" to routineList
                    )

                    // replace old routine data with updated data
                    routines[index] = updatedRoutine as Map<String, Any>

                    val userRef = db.collection("users").document(userID)
                    userRef.update(
                        "routines", routines
                    ).addOnSuccessListener {
                        // Toast.makeText(context, "Removed $name", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to update name", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
    }

    private fun removeFromRoutine(name: String, id: String) {
        val docRef = db.collection("users").document(userID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val routines = document.get("routines") as ArrayList<Map<String, Any>>

                    val routine = routines[index] // selected routine
                    val routineName = routine["name"] as? String ?: ""
                    val routineList = routine["routineList"] as? ArrayList<Map<String, Any>>

                    val exerciseData = mapOf(
                        "exerciseName" to name,
                        "exerciseID" to id
                    )

                    routineList?.remove(exerciseData)

                    //update global var
                    if (routineList != null) {
                        ::routineList.set(routineList)
                    }

                    val updatedRoutine = mapOf(
                        "name" to routineName,
                        "routineList" to routineList
                    )

                    // replace old routine data with updated data
                    routines[index] = updatedRoutine as Map<String, Any>

                    val userRef = db.collection("users").document(userID)
                    userRef.update(
                        "routines", routines
                    ).addOnSuccessListener {
                        Toast.makeText(context, "Removed $name", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to remove exercise", Toast.LENGTH_SHORT)
                            .show()
                    }

                }
            }
    }

    private fun showDeleteRoutineDialog(){
        canShowDeleteRoutineDialog = false
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Are you sure you want to remove ${name}?")
            .setNegativeButton("Cancel") { dialog, which ->
                canShowDeleteRoutineDialog = true
                dialog.dismiss()
            }
            .setPositiveButton("Confirm") { dialog, which ->
                canShowDeleteRoutineDialog = true
                deleteRoutine()
                dialog.dismiss()
            }.setOnDismissListener{
                canShowDeleteRoutineDialog = true
            }.setOnCancelListener{
                canShowDeleteRoutineDialog = true
            }
            .show()
    }

    private fun deleteRoutine(){
        val routineData = mapOf(
            "name" to name,
            "routineList" to routineList
        )

        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid
            val userRef = db.collection("users").document(uid)
            userRef.update(
                "routines", FieldValue.arrayRemove(routineData)
            ).addOnSuccessListener {
                Toast.makeText(context, "Successfully removed $name", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }.addOnFailureListener{
                Toast.makeText(context, "Failed to remove $name", Toast.LENGTH_SHORT).show()
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

    private fun replaceFragmentWithAnimWithData(fragment: Fragment, exerciseID: String, exerciseItemListData: ArrayList<Map<String, Any>>) {

        val exerciseIndex = exerciseItemListData.indexOfFirst { it["exerciseID"] == exerciseID }

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
        fragmentTransaction.addToBackStack("RoutineFragment")
        fragmentTransaction.commit()
    }

    fun replaceTimeUnits(input: String): String {
        var result = input
        result = result.replace("minute", "min", ignoreCase = true)
        result = result.replace("seconds", "sec", ignoreCase = true)
        return result
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