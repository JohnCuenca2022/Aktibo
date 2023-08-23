package com.example.aktibo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView

class ExerciseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)

        // Initialize and assign variable
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set Home selected
        bottomNavigationView.selectedItemId = R.id.exercise

        // Perform item selected listener

        // Perform item selected listener
        bottomNavigationView.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.exercise -> return@OnNavigationItemSelectedListener true
                R.id.home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(R.anim.enter_from_left, R.anim.exit_out_right)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.food -> {
                    startActivity(Intent(applicationContext, FoodActivity::class.java))
                    overridePendingTransition(R.anim.enter_from_left, R.anim.exit_out_right)
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })
    }
}