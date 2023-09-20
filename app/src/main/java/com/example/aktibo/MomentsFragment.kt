package com.example.aktibo

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView

class MomentsFragment : Fragment() {

    private lateinit var scrollView: ScrollView
    private lateinit var linearLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var imageButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_moments, container, false)


        scrollView = view.findViewById(R.id.scrollView)
        linearLayout = view.findViewById(R.id.scrollContainer)
        progressBar = view.findViewById(R.id.progressBar)

        // first 10 moments
        addNewContent(10)

        // add more content when user has scrolled to bottom
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (isAtBottom(scrollView)) {
                // Show loading ProgressBar
                progressBar.visibility = View.VISIBLE

                // Simulate loading new content
                Handler().postDelayed({
                    // Call your function to add 10 new content items here
                    addNewContent(10)

                    // Hide loading ProgressBar
                    progressBar.visibility = View.GONE
                }, 1000) // Delay for simulating loading (adjust as needed)
            }
        }

        // New Moment Button
        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        imageButton = view.findViewById(R.id.addNewMomentButton)
        imageButton.setOnClickListener {
            // Apply fadeOut animation when pressed
            imageButton.startAnimation(fadeOut)

            val fragmentManager = getParentFragmentManager()
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right, // Enter animation
                R.anim.slide_out_left, // Exit animation
                R.anim.slide_in_right, // Pop enter animation (for back navigation)
                R.anim.slide_out_left // Pop exit animation (for back navigation)
            )
            fragmentTransaction.replace(R.id.fragment_container, NewMomentFragment())
            fragmentTransaction.commit()

            // Apply fadeIn animation when released
            imageButton.startAnimation(fadeIn)
        }

        return view
    }

    // Helper function to check if the ScrollView is at the bottom
    private fun isAtBottom(scrollView: ScrollView): Boolean {
        val scrollY = scrollView.scrollY
        val height = scrollView.height
        val scrollViewChild = scrollView.getChildAt(0)
        return scrollY + height >= scrollViewChild.height
    }

    // Function to add new content items to the LinearLayout
    private fun addNewContent(count: Int) {
        val inflater = layoutInflater

        for (i in 1..count) {
            val itemLayout = inflater.inflate(R.layout.moments_item, null)

            val marginLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            marginLayoutParams.setMargins(
                0,
                0,
                0,
                resources.getDimensionPixelSize(R.dimen.bottom_margin)
            ) // Adjust the margin as needed
            itemLayout.layoutParams = marginLayoutParams

            val imageView = itemLayout.findViewById<ImageView>(R.id.momentImg)
            if (i % 2 == 0) {
                imageView.visibility = ImageView.GONE // Set visibility to "gone"
            }

            linearLayout.addView(itemLayout)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}