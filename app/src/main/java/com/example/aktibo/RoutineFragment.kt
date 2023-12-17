package com.example.aktibo

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class RoutineFragment : Fragment() {

    private lateinit var name: String
    private lateinit var routineList: ArrayList<Map<String, ArrayList<String>>>

    private lateinit var exercisesContainer: LinearLayout
    private lateinit var deleteRoutineImageButton: ImageButton

    private lateinit var parentLayout: ConstraintLayout

    var db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_routine, container, false)

        val bundle = arguments
        if (bundle != null) {
            name = bundle.getString("name").toString()
            routineList = bundle.getSerializable("routineList") as ArrayList<Map<String, ArrayList<String>>>
        }

        parentLayout = view.findViewById(R.id.parentLayout)


        val textViewRoutineName = view.findViewById<TextView>(R.id.textViewRoutineName)
        val routineNameEditText = view.findViewById<EditText>(R.id.routineNameEditText)

        textViewRoutineName.text = name
        textViewRoutineName.setOnClickListener {
            textViewRoutineName.visibility = View.GONE
            routineNameEditText.visibility = View.VISIBLE
            routineNameEditText.requestFocus()
        }

        routineNameEditText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                // EditText has gained focus
                // Add your code here for the onFocus event
            } else {
                textViewRoutineName.visibility = View.VISIBLE
                routineNameEditText.visibility = View.GONE
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
                    showDeleteRoutineDialog()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showExercises(){
        if (routineList.isEmpty()){
            exercisesContainer.removeAllViews()
            addTextViewToLinearLayout(exercisesContainer, "There are no exercises")
        } else {
            exercisesContainer.removeAllViews()
            for (routine in routineList) {
                val exerciseID = routine["exerciseID"] as? String ?: ""

                val db = Firebase.firestore
                val docRef = db.collection("exercises").document(exerciseID)
                docRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val id = document.id
                            val name = document.getString("name").toString()
                            val reps_duration = document["reps_duration"].toString()
                            val sets = document.getDouble("sets")?.toInt().toString()
                            val est_time = document["est_time"].toString()

                            val tags: ArrayList<String> = document.data?.get("tags") as ArrayList<String>

                            var tagsString = ""
                            for ((index, tag) in tags.withIndex()) {
                                if (index == tags.size - 1) {
                                    tagsString += "${tag}"
                                } else {
                                    tagsString += "${tag}, "
                                }
                            }

                            val inflater = LayoutInflater.from(requireContext())
                            val itemLayout = inflater.inflate(R.layout.exercise_item, null)

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

                            val addToRoutineButton = itemLayout.findViewById<ImageButton>(R.id.addToRoutineButton)
                            val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                            val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

                            addToRoutineButton.setOnClickListener{
                                addToRoutineButton.startAnimation(fadeOut)

                                // showAddToRoutineDialog(name, id)

                                addToRoutineButton.startAnimation(fadeIn)
                            }

                            val layout = itemLayout.findViewById<LinearLayout>(R.id.layout)
                            layout.setOnClickListener{
                                replaceFragmentWithAnimWithData(ExerciseItemFragment(), id)
                            }

                            exercisesContainer.addView(itemLayout)
                        }
                    }
            }
        }
    }


    private fun showDeleteRoutineDialog(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Are you sure you want to remove ${name}?")
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setPositiveButton("Confirm") { dialog, which ->
                deleteRoutine()
                dialog.dismiss()
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