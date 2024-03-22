package com.example.aktibo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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


        val upperExerciseButtonFrame = view.findViewById<FrameLayout>(R.id.upperExerciseButtonFrame)
        val lowerExerciseButtonFrame = view.findViewById<FrameLayout>(R.id.lowerExerciseButtonFrame)
        val wholeExerciseButtonFrame = view.findViewById<FrameLayout>(R.id.wholeExerciseButtonFrame)

        val prePostStretchesButtonFrame = view.findViewById<FrameLayout>(R.id.prePostStretchesButtonFrame)
        val preventionStretchesButtonFrame = view.findViewById<FrameLayout>(R.id.preventionStretchesButtonFrame)

        if (intensity == "stretching"){
            val prePostStretchesButton = view.findViewById<ImageButton>(R.id.prePostStretchesButton)
            prePostStretchesButton.setOnClickListener{
                replaceFragmentWithAnimWithData(ExerciseListFragment(), "pre-post")
            }
            val preventionStretchesButton = view.findViewById<ImageButton>(R.id.preventionStretchesButton)
            preventionStretchesButton.setOnClickListener{
                replaceFragmentWithAnimWithData(ExerciseListFragment(), "prevention")
            }
            upperExerciseButtonFrame.visibility = View.GONE
            lowerExerciseButtonFrame.visibility = View.GONE
            wholeExerciseButtonFrame.visibility = View.GONE
            prePostStretchesButtonFrame.visibility = View.VISIBLE
            preventionStretchesButtonFrame.visibility = View.VISIBLE

        } else {
            val upperExerciseButton = view.findViewById<ImageButton>(R.id.upperExerciseButton)
            upperExerciseButton.setOnClickListener{
                replaceFragmentWithAnimWithData(ExerciseListFragment(), "upper")
            }

            val lowerExerciseButton = view.findViewById<ImageButton>(R.id.lowerExerciseButton)
            lowerExerciseButton.setOnClickListener{
                replaceFragmentWithAnimWithData(ExerciseListFragment(), "lower")
            }

            val wholeExerciseButton = view.findViewById<ImageButton>(R.id.wholeExerciseButton)
            wholeExerciseButton.setOnClickListener{
                replaceFragmentWithAnimWithData(ExerciseListFragment(), "whole")
            }
            prePostStretchesButtonFrame.visibility = View.GONE
            preventionStretchesButtonFrame.visibility = View.GONE
            upperExerciseButtonFrame.visibility = View.VISIBLE
            lowerExerciseButtonFrame.visibility = View.VISIBLE
            wholeExerciseButtonFrame.visibility = View.VISIBLE
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