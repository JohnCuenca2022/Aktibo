package com.example.aktibo

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton

class FoodFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_food, container, false)

        val foodRecordButton = view.findViewById<ImageButton>(R.id.foodRecordButton)
        val mealRecipesButton = view.findViewById<ImageButton>(R.id.mealRecipesButton)

        foodRecordButton.setOnClickListener{
            Log.i(ContentValues.TAG,"Food Record")
        }

        mealRecipesButton.setOnClickListener{
            Log.i(ContentValues.TAG,"Meal Recipes")
        }

        return view
    }

}