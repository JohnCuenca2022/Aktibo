package com.example.aktibo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NewUser1Fragment : Fragment() {

    private lateinit var sharedViewModel: NewUserSharedViewModel
    private lateinit var heightImperialTextView: TextView

    private var current_selected_feet = 0
    private var current_selected_inches = 0
    private var heightInInches = 48

    private var canShowHeightDialog = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(NewUserSharedViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_user1, container, false)

        heightImperialTextView = view.findViewById(R.id.heightImperialTextView)

        heightImperialTextView.setOnClickListener {
            showHeightSelectionDialog()
        }

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

        val heightSpinner: Spinner = view.findViewById(R.id.heightSpinner)
        context?.let {
            ArrayAdapter.createFromResource(
                it,
                R.array.height_array,
                R.layout.custom_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                heightSpinner.adapter = adapter
            }
        }

        val heightEditText = view.findViewById<EditText>(R.id.heightEditText)
        val heightImperialLayout = view.findViewById<LinearLayout>(R.id.heightImperialLayout)

        heightSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                val selectedItem = parent?.getItemAtPosition(position).toString()
                if (selectedItem == "in") {
                    heightEditText.visibility = View.GONE
                    heightImperialLayout.visibility = View.VISIBLE
                } else if (selectedItem == "cm") {
                    heightImperialLayout.visibility = View.GONE
                    heightEditText.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case where nothing is selected if needed
            }
        })

//        val heightSpinnerFeet: Spinner = view.findViewById(R.id.heightSpinnerFeet)
//        context?.let {
//            ArrayAdapter.createFromResource(
//                it,
//                R.array.feet_spinner_options,
//                R.layout.custom_spinner_item
//            ).also { adapter ->
//                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//                heightSpinnerFeet.adapter = adapter
//            }
//        }
//
//        val heightSpinnerInches: Spinner = view.findViewById(R.id.heightSpinnerInches)
//        context?.let {
//            ArrayAdapter.createFromResource(
//                it,
//                R.array.inches_spinner_options,
//                R.layout.custom_spinner_item
//            ).also { adapter ->
//                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//                heightSpinnerInches.adapter = adapter
//            }
//        }

        val button_continue: Button = view.findViewById(R.id.button_continue)
        button_continue.setOnClickListener{
            var weightEditText: String = view.findViewById<EditText>(R.id.weightEditText).text.toString()
            var heightEditText: String = view.findViewById<EditText>(R.id.heightEditText).text.toString()

            val weightSpinnerSelected: String = weightSpinner.selectedItem.toString() // kg or lbs
            val heightSpinnerSelected: String = heightSpinner.selectedItem.toString() // cm or in

            if (weightEditText == ""){
                Toast.makeText(context, "Please input your weight", Toast.LENGTH_SHORT).show()
            }
            else if (weightEditText.toFloat() <= 0){
                Toast.makeText(context, "Weight must be greater than 0", Toast.LENGTH_SHORT).show()
            }
            else if (heightSpinnerSelected == "cm" && heightEditText == "") {
                Toast.makeText(context, "Please input your height", Toast.LENGTH_SHORT).show()
            }
            else if (heightSpinnerSelected == "cm" && heightEditText.toFloat() <= 100) {
                Toast.makeText(context, "Height must be greater than 100 cm", Toast.LENGTH_SHORT).show()
            }
            else {
                if (weightSpinnerSelected == "lbs") {
                    var weight = weightEditText.toDouble()
                    weight /= 2.205 // convert lbs to kg
                    weightEditText = weight.toString()
                }
                sharedViewModel.weight = weightEditText

                if (heightSpinnerSelected == "in") {
                    var height = heightInInches.toDouble()
                    height *= 2.54 // convert in to cm
                    heightEditText = height.toString()
                }
                sharedViewModel.height = heightEditText

                val fragmentManager = getParentFragmentManager()
                val transaction = fragmentManager.beginTransaction()
                transaction.setCustomAnimations(
                    R.anim.slide_in_right, // Enter animation
                    R.anim.slide_out_left, // Exit animation
                    R.anim.slide_in_left, // Pop enter animation (for back navigation)
                    R.anim.slide_out_right // Pop exit animation (for back navigation)
                )
                transaction.replace(R.id.fragment_container_new_user, NewUser2Fragment())
                transaction.addToBackStack(null)
                transaction.commit()
            }

        }

        return view
    }

    private fun showHeightSelectionDialog(){
        if (!canShowHeightDialog){
            return
        }

        canShowHeightDialog = false

        val customLayout = layoutInflater.inflate(R.layout.height_imperial_dialog_layout, null)
        val number_picker_feet = customLayout.findViewById<NumberPicker>(R.id.number_picker_feet)
        val feet_array = arrayOf("4'", "5'", "6'", "7'", "8'")
        number_picker_feet.minValue = 0
        number_picker_feet.maxValue = feet_array.size - 1
        number_picker_feet.displayedValues = feet_array
        number_picker_feet.value = current_selected_feet
        number_picker_feet.wrapSelectorWheel = false

        val number_picker_inches = customLayout.findViewById<NumberPicker>(R.id.number_picker_inches)
        val inches_array = arrayOf("0\"", "1\"", "2\"", "3\"", "4\"", "5\"", "6\"", "7\"", "8\"", "9\"", "10\"", "11\"")
        number_picker_inches.minValue = 0
        number_picker_inches.maxValue = inches_array.size - 1
        number_picker_inches.displayedValues = inches_array
        number_picker_inches.value = current_selected_inches
        number_picker_inches.wrapSelectorWheel = false

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(customLayout)
            .setTitle("Select a Height")
            .setPositiveButton("OK") { _, _ ->
                val selectedFeet = feet_array[number_picker_feet.value]
                current_selected_feet = number_picker_feet.value
                val selectedInches = inches_array[number_picker_inches.value]
                current_selected_inches = number_picker_inches.value
                val selectedHeightText = "${selectedFeet}${selectedInches}"
                heightImperialTextView.text = selectedHeightText
                heightInInches = (extractIntFromString(selectedFeet)?.times(12) ?: 0) + (extractIntFromString(selectedInches)?: 0)

                canShowHeightDialog = true
            }
            .setNegativeButton("Cancel") { _, _ ->
                canShowHeightDialog = true
            }
            .setOnCancelListener{
                canShowHeightDialog = true
            }
            .setOnDismissListener{
                canShowHeightDialog = true
            }
        builder.show()
    }

    fun extractIntFromString(input: String): Int? {
        val regex = Regex("\\d+")
        val matchResult = regex.find(input)

        return matchResult?.value?.toIntOrNull()
    }

}