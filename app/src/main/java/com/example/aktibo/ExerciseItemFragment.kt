package com.example.aktibo

import android.os.Bundle
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class ExerciseItemFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_exercise_item, container, false)


        // Text Instruction
        val items = arrayOf(
            "Start on your hands and toes, with your hands slightly wider than shoulder-width apart.",
            "Keep your body straight from head to toes, like a plank.",
            "Bend your arms and lower your body until your chest touches the ground or goes as low as you can comfortably.",
            "Push through your hands and straighten your arms to raise your body back up.",
            "Keep your body straight throughout the movement.",
            "Repeat for the desired number of repetitions, maintaining good form.",
            "Repeat for the desired number of repetitions, maintaining good form.",
            "Repeat for the desired number of repetitions, maintaining good form.",
            "Repeat for the desired number of repetitions, maintaining good form.",
            "Repeat for the desired number of repetitions, maintaining good form.")

        val layoutInstructions: LinearLayout = view.findViewById(R.id.layoutInstructions)

        for ((index, item) in items.withIndex()) {
            val customItem = inflater.inflate(R.layout.ordered_list_item, null) as LinearLayout

            val textViewNumber = customItem.findViewById<TextView>(R.id.textViewNumber)
            val listNum = index+1
            val textViewNumberText = "$listNum."
            textViewNumber.text = textViewNumberText

            val textViewStep = customItem.findViewById<TextView>(R.id.textViewStep)
            textViewStep.text = item

            layoutInstructions.addView(customItem)
        }


        return view
    }

}