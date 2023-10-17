package com.example.aktibo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton

class ExerciseRegionsFragment : Fragment() {

    lateinit var intensity: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = arguments
        if (bundle != null) {
            val data = bundle.getString("intensity")
            if (data != null) {
                intensity = data
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_exercise_regions, container, false)

        val lightExerciseButton = view.findViewById<ImageButton>(R.id.upperExerciseButton)
        lightExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseListFragment(), "upper")
        }

        val moderateExerciseButton = view.findViewById<ImageButton>(R.id.lowerExerciseButton)
        moderateExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseListFragment(), "lower")
        }

        val vigorousExerciseButton = view.findViewById<ImageButton>(R.id.wholeExerciseButton)
        vigorousExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseListFragment(), "whole")
        }

        return view
    }

    private fun replaceFragmentWithAnimWithData(fragment: Fragment, region: String) {
        val bundle = Bundle()
        bundle.putString("intensity", intensity)
        bundle.putString("region", region)
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
}