package com.example.aktibo

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ExerciseRoutinesFragment : Fragment() {

    private lateinit var userID: String
    private lateinit var textViewHeader2: TextView
    private lateinit var aktiboRoutinesContainer: LinearLayout

    var db = Firebase.firestore

    private lateinit var inflater: LayoutInflater
    private lateinit var marginLayoutParams: LinearLayout.LayoutParams
    var marginDim = 0
    private lateinit var fadeIn: Animation
    private lateinit var fadeOut: Animation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_exercise_routines, container, false)

        ::inflater.set(LayoutInflater.from(requireContext()))

        marginLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.exer_item_height)
        )
        marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))

        marginDim = resources.getDimensionPixelSize(R.dimen.exer_item_height)

        fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        textViewHeader2 = view.findViewById(R.id.textViewHeader2)
        aktiboRoutinesContainer = view.findViewById(R.id.aktiboRoutinesContainer)

        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid
            userID = uid
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler().postDelayed({ // adding artificial delay to make UI more smooth

            // User routines
            if (::userID.isInitialized){
                val docRef = db.collection("users").document(userID)
                docRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            showUserRoutines(document, userID)
                        }
                    }
            }

            // Default routines
            val aktiboRef = db.collection("aktibo").document("aktibo_exercises")
            aktiboRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        showAktiboRoutines(document)
                    }
                }

        }, 500) //

    }

    private fun showAktiboRoutines(document: DocumentSnapshot) {
        val routines = document.get("routines") as? ArrayList<Map<String, Any>> ?: ArrayList()
        println(document)
        println(routines)
        if (!routines.isEmpty()){ // aktibo routines not empty, show to user
            textViewHeader2.visibility = View.VISIBLE
            aktiboRoutinesContainer.visibility = View.VISIBLE

            aktiboRoutinesContainer.removeAllViews()

            for ((indexRoutine, routine) in routines.withIndex()){
                val name = routine["name"] as String
                val routineList = routine["routineList"] as ArrayList<Map<String, ArrayList<String>>>

                // val inflater = LayoutInflater.from(requireContext())
                val itemLayout = inflater.inflate(R.layout.exercise_routine_item, null)

//                val marginLayoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    resources.getDimensionPixelSize(R.dimen.exer_item_height)
//                )
//                marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))
                itemLayout.layoutParams = marginLayoutParams

                val exerciseName = itemLayout.findViewById<TextView>(R.id.exerciseName)
                exerciseName.text = name

                // display exercises
                val exer: ArrayList<String> = ArrayList()

                for (exercise in routineList) {
                    val exerciseNameString = exercise["exerciseName"] as? String ?: ""
                    exer.add(exerciseNameString)
                }

                var exerciseInfoString = ""
                var index = 0
                var wentOver = 0
                for (ex in exer){
                    if (index < 4){
                        if (index == 0){
                            exerciseInfoString += "${ex}"
                        } else {
                            exerciseInfoString += "\n${ex}"
                        }
                    } else {
                        wentOver++
                    }
                    index++
                }
                if (wentOver > 0){
                    exerciseInfoString += "...${wentOver} more"
                }

                val exerciseInfo = itemLayout.findViewById<TextView>(R.id.exerciseInfo)
                exerciseInfo.text = exerciseInfoString

                itemLayout.setOnClickListener{
                    replaceFragmentToRoutine(RoutineFragment(), name, routineList, indexRoutine, true)
                }

                aktiboRoutinesContainer.addView(itemLayout)
            }
        }
    }

    private fun showUserRoutines(document: DocumentSnapshot, uid: String) {
        val routines = document.get("routines") as? ArrayList<Map<String, Any>> ?: ArrayList()
        if (routines.isEmpty()){ // user has no routines yet, create empty routine

            val emptyStringArray: ArrayList<String> = ArrayList()
            val routineData = mapOf(
                "name" to "My Routine",
                "routineList" to emptyStringArray
            )

            // add routine to user's routine list
            val userRef = db.collection("users").document(uid)
            userRef.update(
                "routines", FieldValue.arrayUnion(routineData)
            ).addOnSuccessListener {
                routines.add(routineData)

                // display routines to the user
                val linearLayout = view?.findViewById<LinearLayout>(R.id.yourRoutinesContainer)
                linearLayout?.removeAllViews()
                for (routine in routines){
                    val name = routine["name"] as String
                    val routineList = routine["routineList"] as ArrayList<Map<String, ArrayList<String>>>

                    val inflater = LayoutInflater.from(requireContext())
                    val itemLayout = inflater.inflate(R.layout.exercise_routine_item, null)

                    val marginLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.exer_item_height)
                    )
                    marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))
                    itemLayout.layoutParams = marginLayoutParams

                    val exerciseName = itemLayout.findViewById<TextView>(R.id.exerciseName)
                    exerciseName.text = name

                    val exerciseInfo = itemLayout.findViewById<TextView>(R.id.exerciseInfo)
                    exerciseInfo.text = ""

                    itemLayout.setOnClickListener{
                        replaceFragmentToRoutine(RoutineFragment(), name, routineList, 0)
                    }

                    linearLayout?.addView(itemLayout)
                }
            }

        } else {

            val linearLayout = view?.findViewById<LinearLayout>(R.id.yourRoutinesContainer)
            linearLayout?.removeAllViews()
            for ((indexRoutine, routine) in routines.withIndex()){
                val name = routine["name"] as String
                val routineList = routine["routineList"] as ArrayList<Map<String, ArrayList<String>>>

                // val inflater = LayoutInflater.from(requireContext())
                val itemLayout = inflater.inflate(R.layout.exercise_routine_item, null)

//                val marginLayoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    resources.getDimensionPixelSize(R.dimen.exer_item_height)
//                )
//                marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin))
                itemLayout.layoutParams = marginLayoutParams

                val exerciseName = itemLayout.findViewById<TextView>(R.id.exerciseName)
                exerciseName.text = name

                // display exercises
                val exer: ArrayList<String> = ArrayList()

                for (exercise in routineList) {
                    val exerciseNameString = exercise["exerciseName"] as? String ?: ""
                    exer.add(exerciseNameString)
                }

                var exerciseInfoString = ""
                var index = 0
                var wentOver = 0
                for (ex in exer){
                    if (index < 4){
                        if (index == 0){
                            exerciseInfoString += "${ex}"
                        } else {
                            exerciseInfoString += "\n${ex}"
                        }
                    } else {
                        wentOver++
                    }
                    index++
                }
                if (wentOver > 0){
                    exerciseInfoString += "...${wentOver} more"
                }

                val exerciseInfo = itemLayout.findViewById<TextView>(R.id.exerciseInfo)
                exerciseInfo.text = exerciseInfoString

                itemLayout.setOnClickListener{
                    replaceFragmentToRoutine(RoutineFragment(), name, routineList, indexRoutine)
                }

                linearLayout?.addView(itemLayout)
            }

        }
    }

    private fun replaceFragmentToRoutine(
        fragment: Fragment, name: String,
        routineList: ArrayList<Map<String, ArrayList<String>>>,
        index: Int,
        isAktibo: Boolean = false)
    {
        val bundle = Bundle()
        bundle.putString("name", name)
        bundle.putInt("index", index)
        bundle.putSerializable("routineList", routineList)
        bundle.putBoolean("isAktibo", isAktibo)
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