package com.example.aktibo

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.ArrayList

class MealRecipesDatabaseFragment : Fragment() {

    private lateinit var adapter: BookmarkedRecipeItemAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_meal_recipes_database, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val db = Firebase.firestore
        val recipesCollection = db.collection("recipes")

        recipesCollection.get()
            .addOnSuccessListener { documents ->
                val mapArray: MutableList<Map<String, Any>> = mutableListOf()
                for (document in documents) {
                    val data = mapOf(
                        "foodLabel" to document["foodLabel"],
                        "calories" to document["calories"],
                        "carbs" to document["carbs"],
                        "calories" to document["calories"],
                        "protein" to document["protein"],
                        "fat" to document["fat"],
                        "ingredients" to document["ingredients"] as? ArrayList<String>,
                        "instructions" to document["instructions"] as? ArrayList<String>,
                        "quantity" to document["quantity"]
                    )

                    println("---------------------------------")
                    println(data.toString())
                    mapArray.add(data as Map<String, Any>)
                }
                val adapter = BookmarkedRecipeItemAdapter(mapArray, parentFragmentManager)
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->

            }

        return view
    }



}