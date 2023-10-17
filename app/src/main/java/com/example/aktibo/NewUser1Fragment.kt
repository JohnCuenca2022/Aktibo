package com.example.aktibo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider


class NewUser1Fragment : Fragment() {

    private lateinit var sharedViewModel: NewUserSharedViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(NewUserSharedViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_user1, container, false)

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

        val button_continue: Button = view.findViewById(R.id.button_continue)
        button_continue.setOnClickListener{
            var weightEditText: String = view.findViewById<EditText>(R.id.weightEditText).text.toString()
            var heightEditText: String = view.findViewById<EditText>(R.id.heightEditText).text.toString()

            val weightSpinnerSelected: String = weightSpinner.selectedItem.toString() // kg or lbs
            val heightSpinnerSelected: String = heightSpinner.selectedItem.toString() // cm or in

            if (weightEditText == ""){
                Toast.makeText(context, "Please input your weight", Toast.LENGTH_SHORT).show()
            }
            else if (heightEditText == "") {
                Toast.makeText(context, "Please input your height", Toast.LENGTH_SHORT).show()
            }
            else {
                if (weightSpinnerSelected == "lbs") {
                    var weight = weightEditText.toDouble()
                    weight /= 2.205 // convert lbs to kg
                    weightEditText = weight.toString()
                }
                sharedViewModel.weight = weightEditText

                if (heightSpinnerSelected == "in") {
                    var height = heightEditText.toDouble()
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


}