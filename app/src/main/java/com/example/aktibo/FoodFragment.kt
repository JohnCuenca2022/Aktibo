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
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

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
            // Apply fadeOut animation when pressed
            mealRecipesButton.startAnimation(fadeOut)

            replaceFragmentWithAnim(MealRecipesFragment())

            // Apply fadeIn animation when released
            mealRecipesButton.startAnimation(fadeIn)
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

        CoroutineScope(Dispatchers.IO).launch{
            val mealRecords = async {
                getMealRecords()
            }.await()

            var totalCals = 0.0
            var totalCarbs = 0.0
            var totalProtein = 0.0
            var totalFat = 0.0

            val todaySubset = mealRecords.filter { isToday((it["date"] as Timestamp).toDate()) }

            todaySubset.forEach {
                val name = it.get("foodLabel")
                val cals = it.get("calories").toString().toDouble()
                val carbs = it.get("carbohydrates").toString().toDouble()
                val protein = it.get("protein").toString().toDouble()
                val fat = it.get("fat").toString().toDouble()

                totalCals += cals
                totalCarbs += carbs
                totalProtein += protein
                totalFat += fat

                Log.e("name", name.toString())
            }

            withContext(Dispatchers.Main) {
                AnimationUtil.animateTextViewNumerical(textViewCalories, 0, totalCals.toInt(), 1000)
                AnimationUtil.animateProgressBar(progressBarCalories, 0, totalCals.toInt(), 1000)

                AnimationUtil.animateTextViewMacros(textViewCarbsCount, 0, totalCarbs.toInt(), 1000)
                AnimationUtil.animateProgressBar(progressBarCarbs, 0, totalCarbs.toInt(), 1000)

                AnimationUtil.animateTextViewMacros(textViewProteinCount, 0, totalProtein.toInt(), 1000)
                AnimationUtil.animateProgressBar(progressBarProtein, 0, totalProtein.toInt(), 1000)

                AnimationUtil.animateTextViewMacros(textViewFatCount, 0, totalFat.toInt(), 1000)
                AnimationUtil.animateProgressBar(progressBarFat, 0, totalFat.toInt(), 1000)
            }

        }
    }

    private suspend fun getMealRecords(): ArrayList<Map<String, Any>>{
        val records = CompletableDeferred<ArrayList<Map<String, Any>>>()

        val db = Firebase.firestore
        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid
            val userRef = db.collection("users").document(uid)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        records.complete(
                            document.get("mealRecords") as? ArrayList<Map<String, Any>> ?: ArrayList()
                        )
                    }
                }
        }

        return records.await()
    }

    fun isToday(date: Date): Boolean {
        val todayCalendar = Calendar.getInstance()
        val givenCalendar = Calendar.getInstance()
        givenCalendar.time = date
        return todayCalendar.get(Calendar.YEAR) == givenCalendar.get(Calendar.YEAR) &&
                todayCalendar.get(Calendar.MONTH) == givenCalendar.get(Calendar.MONTH) &&
                todayCalendar.get(Calendar.DAY_OF_MONTH) == givenCalendar.get(Calendar.DAY_OF_MONTH)
    }

}