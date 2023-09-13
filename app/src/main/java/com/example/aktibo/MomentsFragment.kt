package com.example.aktibo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout

class MomentsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_moments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val linearLayout = view.findViewById<LinearLayout>(R.id.scrollContainer)
        val inflater = layoutInflater


        for (i in 0 until 5) {
            val itemLayout = inflater.inflate(R.layout.moments_item, null)

            // Find and update the UI elements in the item layout
//            val textView = itemLayout.findViewById<TextView>(R.id.textView)
//            textView.text = "Item $i"

            val marginLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            marginLayoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_margin)) // Adjust the margin as needed
            itemLayout.layoutParams = marginLayoutParams

            val imageView = itemLayout.findViewById<ImageView>(R.id.momentImg)
            if (i%2 == 0) {
                imageView.visibility = ImageView.GONE // Set visibility to "gone"
            }

            // Add the item layout to the LinearLayout
            linearLayout.addView(itemLayout)
        }
    }
}