package com.example.aktibo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment

class NewUserActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()

        replaceFragment(NewUser1Fragment()) // show initial fragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_user)
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager

        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container_new_user, fragment)
        // transaction.addToBackStack(null)
        transaction.commit()
    }
}