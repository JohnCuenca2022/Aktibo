package com.example.aktibo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class FoodRecordFragment : Fragment() {

    private lateinit var button_newFoodRecord: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_record, container, false)

        val supportFragmentManager = parentFragmentManager

        button_newFoodRecord = view.findViewById(R.id.button_newFoodRecord)
        button_newFoodRecord.setOnClickListener{
            val customDialog = CustomMaterialDialogFragment()
            customDialog.show(supportFragmentManager, "CustomMaterialDialogFragment")
        }

        return view
    }

}