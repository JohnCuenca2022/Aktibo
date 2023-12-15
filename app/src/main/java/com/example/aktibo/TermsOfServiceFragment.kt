package com.example.aktibo

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class TermsOfServiceFragment : Fragment() {

    private val PREFS_NAME = "MyPrefsFile"
    private val PREF_KEY_SHOW_DIALOG = "showDialog"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_terms_of_service, container, false)

        val acceptButton = view.findViewById<Button>(R.id.acceptButton)
        acceptButton.setOnClickListener{
            setShowDialogPreference(false) // hide ToS
            (activity as MainActivity).navigationInterface(4)
        }

        val declineButton = view.findViewById<Button>(R.id.declineButton)
        declineButton.setOnClickListener{
            (activity as MainActivity).navigationInterface(1)
        }

        return view
    }

    private fun setShowDialogPreference(show: Boolean) {
        val prefs: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(PREF_KEY_SHOW_DIALOG, show)
        editor.apply()
    }

}