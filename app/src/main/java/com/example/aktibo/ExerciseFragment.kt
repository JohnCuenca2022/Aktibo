package com.example.aktibo

import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import androidx.fragment.app.Fragment


class ExerciseFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_exercise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calendarView = view.findViewById(R.id.calendarView) as CalendarView // Replace with your CalendarView reference

        val calendar = Calendar.getInstance() // Get the current date
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Set to the first day of the month
        val minDate = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1) // Move to the next month
        calendar.add(Calendar.DAY_OF_MONTH, -1) // Set to the last day of the current month
        val maxDate = calendar.timeInMillis

        calendarView.minDate = minDate
        calendarView.maxDate = maxDate

    }

}