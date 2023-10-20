package com.example.aktibo

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class NewUser3Fragment : Fragment() {

    private lateinit var sharedViewModel: NewUserSharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(NewUserSharedViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_new_user3, container, false)

        val weightSpinner: Spinner = view.findViewById(R.id.weightSpinner)
        context?.let {
            ArrayAdapter.createFromResource(
                it,
                R.array.weight_array,
                R.layout.custom_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                weightSpinner.adapter = adapter
            }
        }

        val height = sharedViewModel.height.toFloat()
        println(height)

        val dbw = (height - 100) - (height - 100)*0.1 // height is in cm
        val formattedValue = String.format("%.2f", dbw) // round off to 2 decimal places
        val desiredWeight = formattedValue.toDouble()

        val dbwLbs = dbw * 2.205 // 1kg = 2.205 lbs
        val formattedValueLbs = String.format("%.2f", dbwLbs)
        val desiredWeightLbs = formattedValueLbs.toDouble()

        val textView = view.findViewById<TextView>(R.id.textView2)
        val fullText = "According to your height,\n$desiredWeight kg / $desiredWeightLbs lbs is your ideal body weight"
        val wordToColor = "$desiredWeight kg / $desiredWeightLbs lbs"
        val spannableString = SpannableString(fullText)

        val startIndex = fullText.indexOf(wordToColor)
        val endIndex = startIndex + wordToColor.length

        val colorResourceId  = R.color.green
        val color = context?.let { ContextCompat.getColor(it, colorResourceId) }
        spannableString.setSpan(color?.let { ForegroundColorSpan(it) }, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString


        val button_continue = view.findViewById<Button>(R.id.button_continue)
        button_continue.setOnClickListener{
            val weightEditText = view.findViewById<EditText>(R.id.weightEditText).text.toString() // target weight

            if (weightEditText == ""){
                Toast.makeText(context, "Please input a target weight", Toast.LENGTH_SHORT).show()
            }
            else if (weightEditText.toDouble() <= 0){
                Toast.makeText(context, "Weight must be greater than 0", Toast.LENGTH_SHORT).show()
            }
            else {
                var targetWeight = weightEditText.toDouble()
                val weightSpinnerSelected: String = weightSpinner.selectedItem.toString() // kg or lbs

                if (weightSpinnerSelected == "lbs"){
                    targetWeight /= 2.205 // convert lbs to kg
                }

                if (sharedViewModel.weightGoal == 1){ // gain weight
                    if (targetWeight <= sharedViewModel.weight.toDouble()) {
                        Toast.makeText(context, "Target weight must be greater than current weight to achieve your weight goal", Toast.LENGTH_SHORT).show()
                    } else {
                        sharedViewModel.targetWeight = targetWeight.toString()
                        println(targetWeight.toString())
                        val fragmentManager = getParentFragmentManager()
                        val transaction = fragmentManager.beginTransaction()
                        transaction.setCustomAnimations(
                            R.anim.slide_in_right, // Enter animation
                            R.anim.slide_out_left, // Exit animation
                            R.anim.slide_in_left, // Pop enter animation (for back navigation)
                            R.anim.slide_out_right // Pop exit animation (for back navigation)
                        )
                        transaction.replace(R.id.fragment_container_new_user, NewUser4Fragment())
                        transaction.addToBackStack(null)
                        transaction.commit()
                    }
                }
                else if (sharedViewModel.weightGoal == 2) { // lose weight
                    if (targetWeight >= sharedViewModel.weight.toDouble()) {
                        Toast.makeText(context, "Target weight must be less than current weight to achieve your weight goal", Toast.LENGTH_SHORT).show()
                    } else {
                        sharedViewModel.targetWeight = targetWeight.toString()
                        println(targetWeight.toString())
                        val fragmentManager = getParentFragmentManager()
                        val transaction = fragmentManager.beginTransaction()
                        transaction.setCustomAnimations(
                            R.anim.slide_in_right, // Enter animation
                            R.anim.slide_out_left, // Exit animation
                            R.anim.slide_in_left, // Pop enter animation (for back navigation)
                            R.anim.slide_out_right // Pop exit animation (for back navigation)
                        )
                        transaction.replace(R.id.fragment_container_new_user, NewUser4Fragment())
                        transaction.addToBackStack(null)
                        transaction.commit()
                    }
                }
            }



        }

        return view
    }

}