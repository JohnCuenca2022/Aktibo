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
import android.widget.Toast
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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

    private lateinit var button_cancel: Button
    private lateinit var foodDescription: String

    // Edamam Food Database
    val appId = "3aa8e209"
    val appKey = "e27dfa3bd620b88df56e8416fa7de697"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_food_record, container, false)

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

                val foodItems = foodNutrients[0]

                println(foodItems.toString())

//                val foodItems = foodNutrients[0] // food
//                for (food in foodItems) {
//                    var label = parsedItem.food.label
//                    val ENERC_KCAL = parsedItem.food.nutrients.ENERC_KCAL
//                    val CHOCDF = parsedItem.food.nutrients.CHOCDF
//                    val PROCNT = parsedItem.food.nutrients.PROCNT
//                    val FAT = parsedItem.food.nutrients.FAT
//
//                    Log.e("Edamam API", label)
//                    Log.e("Edamam API ENERC_KCAL", ENERC_KCAL.toString())
//                    Log.e("Edamam API CHOCDF", CHOCDF.toString())
//                    Log.e("Edamam API PROCNT", PROCNT.toString())
//                    Log.e("Edamam API FAT", FAT.toString())
//
//                }
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
                        } else {
                            foodNutrients.complete(edamamResponse.parsed)
                        }

                    } else {
                        Log.e("Edamam API", "Empty response body")
                    }
                } else {
                    Log.e("Edamam API", "failed reponse")
                }
            }

            override fun onFailure(call: Call<EdamamResponse>, t: Throwable) {
                Log.e("Edamam API", "error: ${t.message}")
            }
        })
        return foodNutrients.await()
    }

}