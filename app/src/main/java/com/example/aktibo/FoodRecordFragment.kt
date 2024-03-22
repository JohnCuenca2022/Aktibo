package com.example.aktibo

import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aktibo.LoginActivity.Companion.TAG
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class FoodRecordFragment : Fragment() {

    private lateinit var button_newFoodRecord: Button
    private lateinit var adapter: FoodRecordItemAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_record, container, false)

        val supportFragmentManager = parentFragmentManager

        button_newFoodRecord = view.findViewById(R.id.button_newFoodRecord)
        button_newFoodRecord.setOnClickListener{
            val customDialog = CustomMaterialDialogFragment()
            customDialog.show(supportFragmentManager, "CustomMaterialDialogFragment")
        }

        recyclerView = view.findViewById(R.id.recyclerView)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.IO).launch{

            val dataset = async {
                getDataSet()
            }.await()

            withContext(Dispatchers.Main) {

                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                adapter = FoodRecordItemAdapter(dataset as MutableList<FoodRecordItem>) { item ->
                    // Handle long click event here
                }
                recyclerView.adapter = adapter

            }

        }
    }

//    private suspend fun getDataSet(): List<Map<String, Any>> {
//
//        val future = CompletableDeferred<List<Map<String, Any>>>()
//
//        // Retrieve data from Firestore
//        val db = Firebase.firestore
//        val collectionRef = db.collection("users")
//
//        collectionRef.get()
//            .addOnSuccessListener { documents ->
//                val dataSet = mutableListOf<Map<String, Any>>()
//
//                for (document in documents) {
//                    val mealRecords = document.get("mealRecords") as? List<Map<String, Any>>
//                    Log.w(TAG, mealRecords.toString())
//                    // Add each meal record to the dataset
//                    mealRecords?.forEach { mealRecord ->
//                        dataSet.add(mealRecord)
//                    }
//                }
//
//                val sortedArray = dataSet.sortedByDescending { it["date"] as Timestamp }
//
//                future.complete(sortedArray)
//            }
//            .addOnFailureListener { exception ->
//                Log.w(TAG, "Error getting documents.", exception)
//                future.completeExceptionally(exception)
//            }
//
//        return future.await()
//    }

    private suspend fun getDataSet(): List<FoodRecordItem> {
        val future = CompletableDeferred<List<FoodRecordItem>>()

        val currentUser = Firebase.auth.currentUser
        val uid = currentUser?.uid

        if (uid != null){
            // Retrieve data from Firestore
            val db = Firebase.firestore
            val collectionRef = db.collection("users").document(uid)

            collectionRef.get()
                .addOnSuccessListener { document ->
                    val dataSet = mutableListOf<FoodRecordItem>()

                    val groupedData = mutableMapOf<Date, MutableList<Map<String, Any>>>()

                    val mealRecords = document.get("mealRecords") as? List<Map<String, Any>>
                    mealRecords?.forEach { mealRecord ->
                        val date = (mealRecord["date"] as? Timestamp)?.toDate()//.formatDate()
                        if (date != null) {
                            val truncatedDate = truncateTime(date)
                            groupedData.getOrPut(truncatedDate) { mutableListOf() }.add(mealRecord)
                        }
                    }

                    // Add DateHeaderItem and FoodItem to dataSet
                    groupedData.entries.sortedByDescending { it.key }.forEach { (date, records) ->
                        println(date)
                        dataSet.add(FoodRecordItem.DateHeaderItem(date.formatDate()))
                        records.reversed().forEach { record ->
                            println("PROTEIN: ${record["protein"].toString().toDouble()}")

                            dataSet.add(FoodRecordItem.FoodItem(
                                record["foodLabel"].toString(),
                                record["calories"].toString().toDouble(),
                                record["carbohydrates"].toString().toDouble(),
                                record["protein"].toString().toDouble(),
                                record["fat"].toString().toDouble(),
                                record["date"] as Timestamp
                            ))
                        }
                    }

                    future.complete(dataSet)
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents.", exception)
                    future.completeExceptionally(exception)
                }


        }
        return future.await()
    }

    fun truncateTime(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    // Extension function to format Date
    private fun Date.formatDate(): String {
        val today = LocalDate.now()
        val date = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        }
    }
}