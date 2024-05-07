package com.example.aktibo

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MomentsFragment : Fragment() {

    private lateinit var imageButton: ImageButton
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val PREFS_NAME = "MyPrefsFile"
    private val PREF_KEY_SHOW_DIALOG = "showDialog"

    //var canLoadMoreMoments = true
    //var canShowEndOfMomentsMessage = true

//    val db = Firebase.firestore
//    val momentsRef = db.collection("moments")
//
//    val query = momentsRef
//        // don't show moments(posts) with more than 5 reports
//        .whereLessThan("reportsCount", 5)
//        .orderBy("reportsCount", Query.Direction.ASCENDING)
//        .orderBy("datePosted", Query.Direction.DESCENDING)

//    private lateinit var lastVisible: DocumentSnapshot

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_moments, container, false)

        // New Moment Button
        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        imageButton = view.findViewById(R.id.addNewMomentButton)
        imageButton.setOnClickListener {
            // Apply fadeOut animation when pressed
            imageButton.startAnimation(fadeOut)

            replaceFragmentWithAnim(NewMomentFragment())

            // Apply fadeIn animation when released
            imageButton.startAnimation(fadeIn)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        val adapter = ViewPagerAdapterMoments(childFragmentManager, lifecycle)
        viewPager.adapter = adapter

        // Create tabs and connect them with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Set tab text based on position
            tab.text = when (position) {
                0 -> "Moments"
                1 -> "My Posts"
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }.attach()

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

    // Terms of Service
    private fun shouldShowDialog(): Boolean {
        val prefs: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_SHOW_DIALOG, true)
    }

    private fun setShowDialogPreference(show: Boolean) {
        val prefs: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(PREF_KEY_SHOW_DIALOG, show)
        editor.apply()
    }

    private fun resetPreferences() {
        val prefs: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.clear() // This will remove all preferences
        editor.apply()
    }


}