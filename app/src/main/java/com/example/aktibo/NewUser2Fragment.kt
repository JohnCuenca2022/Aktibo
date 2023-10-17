package com.example.aktibo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider

class NewUser2Fragment : Fragment() {

    private lateinit var sharedViewModel: NewUserSharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(NewUserSharedViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_new_user2, container, false)

        val button_gain: Button = view.findViewById(R.id.button_gain)
        button_gain.setOnClickListener{
            sharedViewModel.weightGoal = 1

            val fragmentManager = getParentFragmentManager()
            val transaction = fragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.anim.slide_in_right, // Enter animation
                R.anim.slide_out_left, // Exit animation
                R.anim.slide_in_left, // Pop enter animation (for back navigation)
                R.anim.slide_out_right // Pop exit animation (for back navigation)
            )
            transaction.replace(R.id.fragment_container_new_user, NewUser3Fragment())
            transaction.addToBackStack("NewUser3Fragment")
            transaction.commit()
        }

        val button_lose: Button = view.findViewById(R.id.button_lose)
        button_lose.setOnClickListener{
            sharedViewModel.weightGoal = 2

            val fragmentManager = getParentFragmentManager()
            val transaction = fragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.anim.slide_in_right, // Enter animation
                R.anim.slide_out_left, // Exit animation
                R.anim.slide_in_left, // Pop enter animation (for back navigation)
                R.anim.slide_out_right // Pop exit animation (for back navigation)
            )
            transaction.replace(R.id.fragment_container_new_user, NewUser3Fragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        val button_maintain: Button = view.findViewById(R.id.button_maintain)
        button_maintain.setOnClickListener{
            sharedViewModel.weightGoal = 0
            sharedViewModel.targetWeight = sharedViewModel.weight

            val fragmentManager = getParentFragmentManager()
            val transaction = fragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.anim.slide_in_right, // Enter animation
                R.anim.slide_out_left, // Exit animation
                R.anim.slide_in_left, // Pop enter animation (for back navigation)
                R.anim.slide_out_right // Pop exit animation (for back navigation)
            )
            transaction.replace(R.id.fragment_container_new_user, NewUser4Fragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        return view
    }

}