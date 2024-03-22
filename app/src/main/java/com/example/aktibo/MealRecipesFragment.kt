package com.example.aktibo

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MealRecipesFragment : Fragment() {

    private lateinit var button_newMealRecipe: Button
    private lateinit var adapter: BookmarkedRecipeItemAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var button_browse: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_meal_recipes, container, false)

        val supportFragmentManager = parentFragmentManager

        button_newMealRecipe = view.findViewById(R.id.button_newMealRecipe)
        button_newMealRecipe.setOnClickListener{
            val customDialog = CustomMaterialDialogMealRecipeFragment()
            customDialog.show(supportFragmentManager, "CustomMaterialDialogMealRecipeFragment")
        }

        button_browse = view.findViewById(R.id.button_browse)
        button_browse.setOnClickListener{
            replaceFragmentWithAnim(MealRecipesDatabaseFragment())
        }

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid
            val db = Firebase.firestore
            val docRef = db.collection("users").document(uid)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val mapArray = document.get("bookmarkedRecipes") as? List<Map<String, Any>>
                        if (mapArray != null) {
                            val adapter = BookmarkedRecipeItemAdapter(mapArray as MutableList<Map<String, Any>>, parentFragmentManager)
                            recyclerView.adapter = adapter
                        }

                    } else {
                        Log.d(ContentValues.TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(ContentValues.TAG, "get failed with ", exception)
                }
        }



        return view
    }

    private fun replaceFragmentWithAnim(fragment: Fragment) {
        val fragmentManager = parentFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
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