package com.example.aktibo

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class CreditsFragment : Fragment() {

    private lateinit var textViewTesters: TextView
    private lateinit var textViewOthers: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_credits, container, false)

        textViewTesters = view.findViewById(R.id.textViewTesters)
        textViewOthers = view.findViewById(R.id.textViewOthers)

        setBoldText(textViewTesters, textViewTesters.text.toString(), "testers")
        setBoldText(textViewOthers, textViewOthers.text.toString(), "family, friends, and acquaintances")

        return view
    }

    private fun setBoldText(textView: TextView, fullText: String, boldText: String) {
        val spannableString = SpannableString(fullText)

        val startIndex = fullText.indexOf(boldText)
        val endIndex = startIndex + boldText.length

        spannableString.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
    }

}