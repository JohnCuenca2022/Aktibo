package com.example.aktibo

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView

class FoodFragment : Fragment() {

    private lateinit var textViewCalories: TextView
    private lateinit var textViewCarbsCount: TextView
    private lateinit var textViewProteinCount: TextView
    private lateinit var textViewFatCount: TextView

    private lateinit var progressBarCalories: ProgressBar
    private lateinit var progressBarCarbs: ProgressBar
    private lateinit var progressBarProtein: ProgressBar
    private lateinit var progressBarFat: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food, container, false)

        // total calories
        textViewCalories = view.findViewById(R.id.textViewCalories)
        progressBarCalories = view.findViewById(R.id.progressBarCalories)

        // macros
        textViewCarbsCount = view.findViewById(R.id.textViewCarbsCount)
        progressBarCarbs = view.findViewById(R.id.progressBarCarbs)

        textViewProteinCount = view.findViewById(R.id.textViewProteinCount)
        progressBarProtein = view.findViewById(R.id.progressBarProtein)

        textViewFatCount = view.findViewById(R.id.textViewFatCount)
        progressBarFat = view.findViewById(R.id.progressBarFat)

        val foodRecordButton = view.findViewById<ImageButton>(R.id.foodRecordButton)
        val mealRecipesButton = view.findViewById<ImageButton>(R.id.mealRecipesButton)

        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        foodRecordButton.setOnClickListener{
            Log.i(ContentValues.TAG,"Food Record")
            // Apply fadeOut animation when pressed
            foodRecordButton.startAnimation(fadeOut)

            replaceFragmentWithAnim(FoodRecordFragment())

            // Apply fadeIn animation when released
            foodRecordButton.startAnimation(fadeIn)
        }

        mealRecipesButton.setOnClickListener{
            Log.i(ContentValues.TAG,"Meal Recipes")
        }

        return view
    }

    private fun replaceFragmentWithAnim(fragment: Fragment) {
        val fragmentManager = getParentFragmentManager()
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            R.anim.slide_in_right, // Enter animation
            R.anim.slide_out_left, // Exit animation
            R.anim.slide_in_left, // Pop enter animation (for back navigation)
            R.anim.slide_out_right // Pop exit animation (for back navigation)
        )
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AnimationUtil.animateTextViewNumerical(textViewCalories, 0, 100, 1000)
        AnimationUtil.animateProgressBar(progressBarCalories, 0, 100, 1000)

        AnimationUtil.animateTextViewMacros(textViewCarbsCount, 0, 100, 1000)
        AnimationUtil.animateProgressBar(progressBarCarbs, 0, 100, 1000)

        AnimationUtil.animateTextViewMacros(textViewProteinCount, 0, 100, 1000)
        AnimationUtil.animateProgressBar(progressBarProtein, 0, 100, 1000)

        AnimationUtil.animateTextViewMacros(textViewFatCount, 0, 100, 1000)
        AnimationUtil.animateProgressBar(progressBarFat, 0, 100, 1000)
    }

}