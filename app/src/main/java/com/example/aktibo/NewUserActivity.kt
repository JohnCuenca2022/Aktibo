package com.example.aktibo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment

class NewUserActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()

        replaceFragment(NewUser1Fragment()) // show initial fragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_user)

        val helper = MyHelperFunctions()
        val NightModeInt = helper.getPreferenceInt("NightModeInt", this)

        if (NightModeInt == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager

        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container_new_user, fragment)
        // transaction.addToBackStack(null)
        transaction.commit()
    }
}