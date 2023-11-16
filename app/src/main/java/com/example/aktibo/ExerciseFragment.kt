package com.example.aktibo

import android.app.usage.UsageEvents
import java.util.Calendar
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.applandeo.materialcalendarview.CalendarView
import androidx.recyclerview.widget.RecyclerView
import com.applandeo.materialcalendarview.CalendarDay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private lateinit var calendarView: CalendarView

class ExerciseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_exercise, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        val currentDate = Calendar.getInstance()

        val currentDate2 = Calendar.getInstance()
        currentDate2.add(Calendar.DAY_OF_MONTH, -1)

        val list = listOf(
            CalendarDay(currentDate).apply {
                labelColor = R.color.primary
                backgroundResource = R.drawable.exercise_star
            },
            CalendarDay(currentDate2).apply {
                labelColor = R.color.primary
                backgroundResource = R.drawable.exercise_star
            }
        )

        calendarView.setCalendarDayLayout(R.layout.custom_calendar_day_layout)
        calendarView.setCalendarDays(list)

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lightExerciseButton = view.findViewById<ImageButton>(R.id.lightExerciseButton)
        lightExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseRegionsFragment(), "light")
        }

        val moderateExerciseButton = view.findViewById<ImageButton>(R.id.moderateExerciseButton)
        moderateExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseRegionsFragment(), "moderate")
        }

        val vigorousExerciseButton = view.findViewById<ImageButton>(R.id.vigorousExerciseButton)
        vigorousExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseRegionsFragment(), "vigorous")
        }
    }

    private fun replaceFragmentWithAnimWithData(fragment: Fragment, intensity: String) {
        val bundle = Bundle()
        bundle.putString("intensity", intensity)
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