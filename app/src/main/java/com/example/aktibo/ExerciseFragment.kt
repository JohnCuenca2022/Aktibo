package com.example.aktibo

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.CalendarView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExerciseFragment : Fragment() {

    private lateinit var monthTextView: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var exerciseGoalMessage: TextView
    private lateinit var exerciseGoalButton: ImageButton
    private lateinit var exerciseRoutinesButton: ImageButton

    class DayViewContainer(view: View) : ViewContainer(view) {
        val textView = view.findViewById<TextView>(R.id.calendarDayText)
        val starImageView = view.findViewById<ImageView>(R.id.starImageView)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_exercise, container, false)

        monthTextView = view.findViewById(R.id.monthTextView)
        calendarView = view.findViewById(R.id.calendarView)

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                // val day = data.date.dayOfMonth
                val dayLocalDate = data.date

                // in and out days
                val isAnInOrOutDay = data.position != DayPosition.MonthDate


                CoroutineScope(IO).launch{
                    val exerciseRecords = async {
                        getExerciseRecords()
                    }.await()

                    var showStar = false

                    for (record in exerciseRecords){
                        val date = record["date"] as? Timestamp
                        if (date != null){
                            val localDate = date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                            if (dayLocalDate == localDate){
//                                println(dayLocalDate.toString())
//                                println(localDate.toString())
                                showStar = true
                            }
                        }

                    }

                    withContext(Dispatchers.Main) {
                        // star days
                        if (showStar){
                            container.textView.text = data.date.dayOfMonth.toString()
                            container.textView.setTextColor(resources.getColor(R.color.white))
                            container.starImageView.visibility = View.VISIBLE
                            if (isAnInOrOutDay){
                                container.textView.setTextColor(resources.getColor(R.color.fainted_primary))
                                container.starImageView.alpha = 0.6F
                            }
                        } else {
                            container.textView.text = data.date.dayOfMonth.toString()
                            if (isAnInOrOutDay){
                                container.textView.setTextColor(resources.getColor(R.color.fainted_primary))
                            }
                        }
                    }

                }
            }
        }

        calendarView.monthScrollListener = { updateTitle() }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(3)  // Adjust as needed
        val endMonth = currentMonth.plusMonths(0)  // Adjust as needed
        val firstDayOfWeek = firstDayOfWeekFromLocale() // Available from the library
        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        val daysOfWeek = daysOfWeek()

        val titlesContainer = view.findViewById<ViewGroup>(R.id.titlesContainer)
        titlesContainer.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                val dayOfWeek = daysOfWeek[index]
                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                textView.text = title
            }

        exerciseGoalMessage = view.findViewById(R.id.exerciseGoalMessage)

        showExerciseRecordMessage()

        // load press animations
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        exerciseGoalButton = view.findViewById(R.id.exerciseGoalButton)
        exerciseGoalButton.setOnClickListener{
            // Apply fadeOut animation when pressed
            exerciseGoalButton.startAnimation(fadeOut)

            replaceFragmentWithAnim(ExerciseGoalFragment())

            // Apply fadeIn animation when released
            exerciseGoalButton.startAnimation(fadeIn)
        }

        exerciseRoutinesButton = view.findViewById(R.id.exerciseRoutinesButton)
        exerciseRoutinesButton.setOnClickListener{
            // Apply fadeOut animation when pressed
            exerciseRoutinesButton.startAnimation(fadeOut)

            replaceFragmentWithAnim(ExerciseRoutinesFragment())

            // Apply fadeIn animation when released
            exerciseRoutinesButton.startAnimation(fadeIn)
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

    private fun updateTitle() {
        val month = calendarView.findFirstVisibleMonth()?.yearMonth ?: return
        val stringText = "${month.month.toString().toLowerCase().capitalize()} ${month.year}"
        monthTextView.text = stringText
    }

    private suspend fun getExerciseRecords(): ArrayList<Map<String, Any>>{
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
                            document.get("exerciseRecords") as? ArrayList<Map<String, Any>> ?: ArrayList()
                        )
                    }
                }
        }

        return records.await()
    }

    private fun showExerciseRecordMessage() {

        val db = Firebase.firestore
        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid
            val userRef = db.collection("users").document(uid)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {

                        val exerciseRecords = document.get("exerciseRecords") as? ArrayList<Map<String, Any>> ?: ArrayList()
                        val exerciseGoal = (document.getDouble("exerciseGoal"))?.toInt()

                        val datesList = ArrayList<Date>()
                        for (record in exerciseRecords) {
                            val date = (record["date"] as? Timestamp)?.toDate()
                            if (date != null){
                                datesList.add(date)
                            }
                        }

                        val uniqueDates = datesList.distinctBy { getCalendarDate(it) }

                        var threshold = 0

                        when (exerciseGoal) {
                            0 -> { // 1-2 days
                                threshold = 1
                            }

                            1 -> { // 3-4
                                threshold = 3
                            }

                            2 -> { // 5+
                                threshold = 5
                            }
                        }

                        val isConsistent = countDatesPerWeekInPast(uniqueDates, 2, threshold)

                        var rangeValue = ""

                        when (exerciseGoal) {
                            0 -> { // 1-2 days
                                rangeValue = "1-2 exercises"
                            }

                            1 -> { // 3-4
                                rangeValue = "3-4 exercises"
                            }

                            2 -> { // 5+
                                rangeValue = "5+ exercises"
                            }
                        }

                        var fullText = ""

                        if (isConsistent){
                            fullText = "You have been consistent on your exercise goal of $rangeValue per week"
                        } else {
                            fullText = "Let's get you back on track on your exercise goal of $rangeValue per week"
                        }

                        val wordToColor = "$rangeValue"
                        val spannableString = SpannableString(fullText)

                        val startIndex = fullText.indexOf(wordToColor)
                        val endIndex = startIndex + wordToColor.length

                        val colorResourceId = R.color.green
                        val color = context?.let { ContextCompat.getColor(it, colorResourceId) }
                        spannableString.setSpan(color?.let { ForegroundColorSpan(it) }, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        exerciseGoalMessage.text = spannableString

                    }
                }
        }
    }

    fun getCalendarDate(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Set time components to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.time
    }

    fun countDatesPerWeekInPast(dateList: List<Date>, weeksBack: Int, threshold: Int): Boolean {

        if (dateList.isEmpty()){
            return false
        }

        // Filter dates in the past set weeks back
        var datesInLastWeeks = dateList.filter { date ->

            // Get the current date and time
            val currentDate = Calendar.getInstance()

            // Set the time to 12:00 AM
            currentDate.set(Calendar.HOUR_OF_DAY, 0)
            currentDate.set(Calendar.MINUTE, 0)
            currentDate.set(Calendar.SECOND, 0)
            currentDate.set(Calendar.MILLISECOND, 0)

            // Set the day of the week to the first day of the week
            currentDate.set(Calendar.DAY_OF_WEEK, currentDate.firstDayOfWeek)

            val endDate = currentDate.timeInMillis
            val startDate = endDate - 7 * weeksBack * 24 * 60 * 60 * 1000


            date.time in startDate until endDate
        }

        if (datesInLastWeeks.isEmpty()){
            return false
        }

        datesInLastWeeks = datesInLastWeeks.sorted()

        val groupedByWeek = datesInLastWeeks
            .groupBy { getWeekOfYear(it) }
            .mapValues { (_, dates) -> dates.size }

        println("Original List: $dateList")
        println("Grouped by Week: $groupedByWeek")

        val allWeeksMeetThreshold = groupedByWeek.all { (_, count) -> count >= threshold }

        println("All weeks meet the threshold: $allWeeksMeetThreshold")

        return allWeeksMeetThreshold
    }

    fun getWeekOfYear(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar[Calendar.WEEK_OF_YEAR]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stretchesExerciseButton = view.findViewById<ImageButton>(R.id.stretchesExerciseButton)
        stretchesExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseRegionsFragment(), "stretching")
        }

        val lightExerciseButton = view.findViewById<ImageButton>(R.id.lightExerciseButton)
        lightExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseListFragment(), "light")
        }

        val moderateExerciseButton = view.findViewById<ImageButton>(R.id.moderateExerciseButton)
        moderateExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseRegionsFragment(), "moderate")
        }

        val vigorousExerciseButton = view.findViewById<ImageButton>(R.id.vigorousExerciseButton)
        vigorousExerciseButton.setOnClickListener{
            replaceFragmentWithAnimWithData(ExerciseRegionsFragment(), "vigorous")
        }

        //showExerciseRecords() // stars
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