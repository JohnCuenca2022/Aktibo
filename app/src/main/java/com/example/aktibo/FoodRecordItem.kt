package com.example.aktibo

import com.google.firebase.Timestamp

sealed class FoodRecordItem {
    data class DateHeaderItem(val date: String) : FoodRecordItem()
    data class FoodItem(
        val foodLabel: String,
        val calories: Double,
        val carbs: Double,
        val protein: Double,
        val fat: Double,
        val date: Timestamp
    ) : FoodRecordItem()
}