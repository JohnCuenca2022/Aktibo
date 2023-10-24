package com.example.aktibo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ExerciseListFragment : Fragment() {
    private lateinit var intensity: String
    private lateinit var region: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = arguments
        if (bundle != null) {
            intensity = bundle.getString("intensity").toString()
            region = bundle.getString("region").toString()

            // println("$intensity, $region")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_exercise_list, container, false)

        var query_region = ""
        var query_intensity = ""

        val textViewTitle = view.findViewById<TextView>(R.id.textViewTitle)
        when (region) {
            "upper" -> {
                textViewTitle.text = "Upper Body Exercises"
                query_region = "Upper Body"
            }
            "lower" -> {
                textViewTitle.text = "Lower Body Exercises"
                query_region = "Lower Body"
            }
            "whole" ->{
                textViewTitle.text = "Whole Body Exercises"
                query_region = "Whole Body"
            }
        }

        when (intensity) {
            "light" -> {
                query_intensity = "Easy"
            }
            "moderate" -> {
                query_intensity = "Intermediate"
            }
            "vigorous" ->{
                query_intensity = "Advanced"
            }
        }

        val linearLayout = view.findViewById<LinearLayout>(R.id.scrollContainer)

        val db = Firebase.firestore
        db.collection("exercises")
            .whereEqualTo("category", query_region)
            .whereEqualTo("intensity", query_intensity)
            .get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                for (data in queryDocumentSnapshots){
                    val id = data.id
                    val name = data.getString("name").toString()
                    val reps_duration = data["reps_duration"].toString()
                    val sets = data.getDouble("sets")?.toInt().toString()
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

                    val itemLayout = inflater.inflate(R.layout.exercise_item, null)

                    val marginLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.exer_item_height)
                    )
                    marginLayoutParams.setMargins(
                        0,
                        0,
                        0,
                        resources.getDimensionPixelSize(R.dimen.bottom_margin)
                    ) // Adjust the margin as needed
                    itemLayout.layoutParams = marginLayoutParams

                    val nameView = itemLayout.findViewById<TextView>(R.id.exerciseName)
                    nameView.text = name

                    val exerciseInfo = itemLayout.findViewById<TextView>(R.id.exerciseInfo)
                    if (containsOnlyDigits(reps_duration)){
                        exerciseInfo.text = "${reps_duration} reps - ${sets} sets\nEst.: ${est_time}"
                    } else {
                        exerciseInfo.text = "${reps_duration}\n${sets} sets\nEst.: ${est_time}"
                    }

                    val exerciseTags = itemLayout.findViewById<TextView>(R.id.exerciseTags)
                    exerciseTags.text = tagsString

                    val layout = itemLayout.findViewById<LinearLayout>(R.id.layout)
                    layout.setOnClickListener{
                        replaceFragmentWithAnimWithData(ExerciseItemFragment(), id)
                    }

                    linearLayout.addView(itemLayout)
                }
            }

        return view
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