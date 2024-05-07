package com.example.aktibo

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class DataPrivacyFragment : Fragment() {

    private val PREFS_NAME = "MyPrefsFile"
    private val PREF_KEY_SHOW_DATA_PRIVACY_DIALOG = "showDataPrivacyDialog"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_data_privacy, container, false)

        val acceptButton = view.findViewById<Button>(R.id.acceptButton)
        acceptButton.setOnClickListener{
            setShowDialogPreference(false) // hide Data Privacy
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun setShowDialogPreference(show: Boolean) {
        val prefs: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(PREF_KEY_SHOW_DATA_PRIVACY_DIALOG, show)
        editor.apply()
    }

}