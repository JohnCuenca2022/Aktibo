package com.example.aktibo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class BookmarkedRecipeItemAdapter(
    private val mapList: MutableList<Map<String, Any>>,
    private val fragmentManager: FragmentManager
    ) :
    RecyclerView.Adapter<BookmarkedRecipeItemAdapter.MapViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.food_record_item, parent, false)
        return MapViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MapViewHolder, position: Int) {
        val currentItem = mapList[position]
        holder.foodNameTextView.text = currentItem["foodLabel"].toString()
        holder.caloriesTextView.text = currentItem["calories"].toString()
        holder.carbsTextView.text = currentItem["carbs"].toString()
        holder.proteinTextView.text = currentItem["protein"].toString()
        holder.fatTextView.text = currentItem["fat"].toString()

        holder.itemView.setOnClickListener {
            // Get the clicked item's data
            val itemData = mapList[position]

            // Create a new instance of the destination fragment
            val fragment = MealRecipeFragmentViewing()

            // Pass data to the destination fragment using arguments
            val bundle = Bundle().apply {
                // Pass the data from the clicked item to the new fragment
                putString("foodLabel", itemData["foodLabel"].toString())
                putString("calories", itemData["calories"].toString())
                putString("carbs", itemData["carbs"].toString())
                putString("protein", itemData["protein"].toString())
                putString("fat", itemData["fat"].toString())
                putStringArrayList("ingredients", itemData["ingredients"] as? ArrayList<String>)
                putStringArrayList("instructions", itemData["instructions"] as? ArrayList<String>)
                putString("quantity", itemData["quantity"].toString())
                putBoolean("bookmerked", true)
                // Add more data as needed
            }
            fragment.arguments = bundle

            // Start FragmentTransaction to navigate to the destination fragment
            val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right, // Enter animation
                R.anim.slide_out_left, // Exit animation
                R.anim.slide_in_left, // Pop enter animation (for back navigation)
                R.anim.slide_out_right // Pop exit animation (for back navigation)
            )
            fragmentTransaction.replace(R.id.fragment_container, fragment)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }

    }

    override fun getItemCount() = mapList.size

    class MapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodName)
        val caloriesTextView: TextView = itemView.findViewById(R.id.calories)
        val carbsTextView: TextView = itemView.findViewById(R.id.carbs)
        val proteinTextView: TextView = itemView.findViewById(R.id.protein)
        val fatTextView: TextView = itemView.findViewById(R.id.fat)

    }
}