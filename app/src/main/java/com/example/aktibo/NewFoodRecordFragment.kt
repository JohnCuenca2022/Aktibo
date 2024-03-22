package com.example.aktibo

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream

class NewFoodRecordFragment : Fragment() {

    private lateinit var linearlayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var button_cancel: Button
    private lateinit var foodDescription: String
    private lateinit var inflater2: LayoutInflater
    private lateinit var marginLayoutParams: LinearLayout.LayoutParams

    // Edamam Food Database
    val appId = "3aa8e209"
    val appKey = "e27dfa3bd620b88df56e8416fa7de697"
    var canSaveFoodRecord = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_food_record, container, false)

        inflater2 = LayoutInflater.from(requireContext())

        marginLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.exer_item_height)
        )
        marginLayoutParams.setMargins(
            0,
            0,
            0,
            resources.getDimensionPixelSize(R.dimen.bottom_margin)
        ) // Adjust the margin as needed

        linearlayout = view.findViewById(R.id.foodContainerLayout)
        progressBar = view.findViewById(R.id.progressBar4)

        button_cancel = view.findViewById(R.id.button_cancel)
        button_cancel.setOnClickListener{
            val fragmentManager = parentFragmentManager
            fragmentManager.popBackStack()
        }

        val args = arguments
        if (args != null) {
            foodDescription = args.getString("foodDescription").toString()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (::foodDescription.isInitialized){

            CoroutineScope(Dispatchers.IO).launch{
                val foodNutrients = async {
                    getFoodNutrients(foodDescription, appId, appKey)
                }.await()

                for (parsed in foodNutrients){
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE

                        val foodItem = parsed.food
                        val quantity = parsed.quantity ?: 0

                        var label = foodItem.label
                        val ENERC_KCAL = foodItem.nutrients.ENERC_KCAL ?: 0
                        val CHOCDF = foodItem.nutrients.CHOCDF ?: 0
                        val PROCNT = foodItem.nutrients.PROCNT ?: 0
                        val FAT = foodItem.nutrients.FAT ?: 0

                        // create view item

                        val itemLayout = inflater2.inflate(R.layout.food_record_item, null)

                        itemLayout.layoutParams = marginLayoutParams

                        // show values

                        val foodName = itemLayout.findViewById<TextView>(R.id.foodName)
                        var foodNameString = ""
                        if(quantity.toDouble() > 0.0){
                            foodNameString = "${label} (${formatFloat(quantity.toDouble())})"
                        } else {
                            foodNameString = label
                        }
                        foodName.text = foodNameString

                        val calories = itemLayout.findViewById<TextView>(R.id.calories)
                        val valoriesString = formatFloat(ENERC_KCAL.toDouble()) // round off
                        calories.text = valoriesString

                        val carbs = itemLayout.findViewById<TextView>(R.id.carbs)
                        val carbsString = formatFloat(CHOCDF.toDouble()) + "g"
                        carbs.text = carbsString

                        val protein = itemLayout.findViewById<TextView>(R.id.protein)
                        val proteinString = formatFloat(PROCNT.toDouble()) + "g"
                        protein.text = proteinString

                        val fat = itemLayout.findViewById<TextView>(R.id.fat)
                        val fatString = formatFloat(FAT.toDouble()) + "g"
                        fat.text = fatString

                        itemLayout.setOnClickListener{
                            saveFoodRecord(label, formatFloat(quantity.toDouble()).toDouble(), ENERC_KCAL.toDouble(), CHOCDF.toDouble(), PROCNT.toDouble(), FAT.toDouble())
                        }

                        // add to layout
                        linearlayout.addView(itemLayout)

                        Log.e("Edamam API", label)
                        Log.e("Edamam API", quantity.toString())
                        Log.e("Edamam API ENERC_KCAL", formatFloat(ENERC_KCAL.toDouble()))
                        Log.e("Edamam API CHOCDF", CHOCDF.toString())
                        Log.e("Edamam API PROCNT", PROCNT.toString())
                        Log.e("Edamam API FAT", FAT.toString())
                    }
                }
            }

        }
    }

    suspend fun getFoodNutrients(foodDescription: String, appID: String, appKey: String): List<ParsedItem> {
        val foodNutrients = CompletableDeferred<List<ParsedItem>>()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.edamam.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(EdamamService::class.java)

        val call = service.findFood(appID, appKey, foodDescription, "logging")
        call.enqueue(object : Callback<EdamamResponse> {
            override fun onResponse(call: Call<EdamamResponse>, response: Response<EdamamResponse>) {
                if (response.isSuccessful) {
                    val edamamResponse = response.body()
                    if (edamamResponse != null) {
                        if (edamamResponse.parsed.isEmpty()){
                            Log.e("Edamam API", "No record found")
                            Toast.makeText(context, "No record found", Toast.LENGTH_SHORT).show()
                            val fragmentManager = parentFragmentManager
                            fragmentManager.popBackStack()
                        } else {
                            foodNutrients.complete(edamamResponse.parsed)
                        }

                    } else {
                        Log.e("Edamam API", "Empty response body")
                        Toast.makeText(context, "No record found", Toast.LENGTH_SHORT).show()
                        val fragmentManager = parentFragmentManager
                        fragmentManager.popBackStack()
                    }
                } else {
                    Log.e("Edamam API", "failed reponse")
                    Toast.makeText(context, "failed to connect to food database", Toast.LENGTH_SHORT).show()
                    val fragmentManager = parentFragmentManager
                    fragmentManager.popBackStack()
                }
            }

            override fun onFailure(call: Call<EdamamResponse>, t: Throwable) {
                Log.e("Edamam API", "error: ${t.message}")
            }
        })
        return foodNutrients.await()
    }

    private fun formatFloat(number: Double?): String {
        val roundedNumber = String.format("%.2f", number).toDouble()
        return if (roundedNumber % 1.0 == 0.0) {
            roundedNumber.toInt().toString()
        } else {
            roundedNumber.toString()
        }
    }

    private fun saveFoodRecord(foodLabel: String, quantity: Double, calories: Double, carbohydrates: Double, protein: Double, fat: Double){
        if (!canSaveFoodRecord){
            return
        }

        canSaveFoodRecord = false

        Log.e("Edamam API", foodLabel)
        Log.e("Edamam API", quantity.toString())
        Log.e("Edamam API ENERC_KCAL", calories.toString())
        Log.e("Edamam API CHOCDF", carbohydrates.toString())
        Log.e("Edamam API PROCNT", protein.toString())
        Log.e("Edamam API FAT", fat.toString())

        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("You selected ${foodLabel}(${quantity.toInt()})")
            .setPositiveButton("Confirm") { dialog, _ ->
                // User confirmed, execute the action
                val user = Firebase.auth.currentUser
                user?.let {
                    val uid = it.uid
                    val db = Firebase.firestore

                    val mealRecord = mapOf(
                        "date" to Timestamp.now(),
                        "foodLabel" to foodLabel,
                        "quantity" to quantity,
                        "calories" to roundToTwoDecimalPlaces(calories),
                        "carbohydrates" to roundToTwoDecimalPlaces(carbohydrates),
                        "protein" to roundToTwoDecimalPlaces(protein),
                        "fat" to roundToTwoDecimalPlaces(fat)
                    )

                    val userRef = db.collection("users").document(uid)
                    userRef
                        .update("mealRecords", FieldValue.arrayUnion(mealRecord))
                        .addOnSuccessListener {
                            Toast.makeText(context, "Meal Recorded", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to record meal", Toast.LENGTH_SHORT).show()
                        }
                }
                dialog.dismiss()
                canSaveFoodRecord = true
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                canSaveFoodRecord = true
            }
            .setOnDismissListener {
                canSaveFoodRecord = true
            }
            .setOnCancelListener{
                canSaveFoodRecord = true
            }
        val alert = builder.create()
        alert.show()

    }

    fun roundToTwoDecimalPlaces(number: Double): Double {
        return "%.${2}f".format(number).toDouble()
    }

}