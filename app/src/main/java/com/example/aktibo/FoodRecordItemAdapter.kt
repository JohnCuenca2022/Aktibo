package com.example.aktibo

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FoodRecordItemAdapter(private val dataSet: MutableList<FoodRecordItem>, private val onItemLongClickListener: (FoodRecordItem.FoodItem) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_FOOD_RECORD = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.food_record_date, parent, false)
                DateHeaderViewHolder(view)
            }
            VIEW_TYPE_FOOD_RECORD -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.food_record_item, parent, false)
                FoodRecordViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataSet[position]
        when (holder.itemViewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val dateHeaderViewHolder = holder as DateHeaderViewHolder
                dateHeaderViewHolder.bind((item as FoodRecordItem.DateHeaderItem).date)
            }
            VIEW_TYPE_FOOD_RECORD -> {
                val foodRecordViewHolder = holder as FoodRecordViewHolder
                val foodItem = item as FoodRecordItem.FoodItem
                foodRecordViewHolder.bind(
                    foodItem.foodLabel,
                    foodItem.calories,
                    foodItem.carbs,
                    foodItem.protein,
                    foodItem.fat,
                    foodItem.date
                )

                holder.itemView.setOnLongClickListener {
                    val foodItem = item as? FoodRecordItem.FoodItem
                    if (foodItem != null) {
                        showDeleteConfirmationDialog(holder.itemView.context) { confirmed ->
                            if (confirmed) {
                                // Call the provided long click listener with the item data
                                onItemLongClickListener(foodItem)

                                // Remove the item from the dataset and notify adapter
                                val positionToRemove = holder.adapterPosition
                                dataSet.removeAt(positionToRemove)
                                notifyItemRemoved(positionToRemove)

                                val user = Firebase.auth.currentUser
                                user?.let {
                                    val uid = it.uid
                                    val db = Firebase.firestore
                                    val docRef = db.collection("users").document(uid)
                                    docRef.get()
                                        .addOnSuccessListener { document ->
                                            if (document != null) {
                                                val mapArray = document.get("mealRecords") as? MutableList<Map<String, Any>>
                                                if (mapArray != null) {
                                                    // Define values to compare for removal
                                                    val nameToRemove = foodItem.foodLabel
                                                    val caloriesToRemove = foodItem.calories
                                                    val carbsToRemove = foodItem.carbs
                                                    val proteinToRemove = foodItem.protein
                                                    val fatToRemove = foodItem.fat
                                                    val dateToRemove = foodItem.date

                                                    println(nameToRemove)
                                                    println(caloriesToRemove)
                                                    println(carbsToRemove)
                                                    println(proteinToRemove)
                                                    println(fatToRemove)
                                                    println(dateToRemove)


                                                    // Remove the maps that match the specified criteria
                                                    val iterator = mapArray.iterator()
                                                    while (iterator.hasNext()) {
                                                        val map = iterator.next()
                                                        val name = map["foodLabel"].toString()
                                                        val calories = map["calories"].toString().toDouble()
                                                        val carbs = map["carbohydrates"].toString().toDouble()
                                                        val protein = map["protein"].toString().toDouble()
                                                        val fat = map["fat"].toString().toDouble()
                                                        val date = map["date"] as Timestamp

                                                        println("------------------------------")
                                                        println(name)
                                                        println(calories)
                                                        println(carbs)
                                                        println(protein)
                                                        println(fat)
                                                        println(date)

                                                        if (name == nameToRemove && calories == caloriesToRemove &&
                                                            carbs == carbsToRemove && protein == proteinToRemove &&
                                                            fat == fatToRemove && date == dateToRemove) {
                                                            iterator.remove() // Remove the map from the list
                                                        }
                                                        // Add additional conditions as needed
                                                    }

                                                    // Update the document with the modified list
                                                    docRef.update("mealRecords", mapArray)
                                                        .addOnSuccessListener {
                                                            // Update successful
                                                            // Handle success as needed
                                                            println("Success")
                                                        }
                                                        .addOnFailureListener { exception ->
                                                            // Handle failure
                                                            println(exception)
                                                        }
                                                }

                                            } else {
                                                Log.d(ContentValues.TAG, "No such document")
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.d(ContentValues.TAG, "get failed with ", exception)
                                        }
                                }
                            }
                        }
                    }
                    true // Return true to indicate the event is consumed
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(context: Context, onConfirmation: (Boolean) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Delete") { dialog, _ ->
                onConfirmation(true)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                onConfirmation(false)
                dialog.dismiss()
            }
            .show()
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (dataSet[position]) {
            is FoodRecordItem.DateHeaderItem -> VIEW_TYPE_DATE_HEADER
            is FoodRecordItem.FoodItem -> VIEW_TYPE_FOOD_RECORD
        }
    }

    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.title)

        fun bind(date: String) {
            dateTextView.text = date
        }
    }

    inner class FoodRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodNameTextView: TextView = itemView.findViewById(R.id.foodName)
        private val caloriesTextView: TextView = itemView.findViewById(R.id.calories)
        private val carbsTextView: TextView = itemView.findViewById(R.id.carbs)
        private val proteinTextView: TextView = itemView.findViewById(R.id.protein)
        private val fatTextView: TextView = itemView.findViewById(R.id.fat)

        fun bind(
            foodName: String,
            calories: Double,
            carbs: Double,
            protein: Double,
            fat: Double,
            date: Timestamp
        ) {
            foodNameTextView.text = foodName
            caloriesTextView.text = calories.toString()
            carbsTextView.text = carbs.toString() + "g"
            proteinTextView.text = protein.toString() + "g"
            fatTextView.text = fat.toString() + "g"
        }
    }
}