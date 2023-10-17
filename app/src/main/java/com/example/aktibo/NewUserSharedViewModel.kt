package com.example.aktibo

import androidx.lifecycle.ViewModel

class NewUserSharedViewModel : ViewModel() {
    var weight: String = ""
    var height: String = ""
    var weightGoal: Int = 0
    var targetWeight: String = ""
    var exerciseGoal: Int = 0
}