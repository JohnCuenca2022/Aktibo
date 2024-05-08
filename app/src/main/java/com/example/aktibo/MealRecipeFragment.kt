package com.example.aktibo

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.assistant.AssistantId
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.core.Status
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.message.MessageContent
import com.aallam.openai.api.message.MessageRequest
import com.aallam.openai.api.run.RunRequest
import com.aallam.openai.api.run.ThreadRunRequest
import com.aallam.openai.api.thread.ThreadMessage
import com.aallam.openai.api.thread.ThreadRequest
import com.aallam.openai.client.OpenAI
import com.elyeproj.loaderviewlibrary.LoaderTextView
import com.google.api.Distribution.BucketOptions.Linear
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Date
import kotlin.time.Duration.Companion.seconds

class MealRecipeFragment : Fragment() {

    // jakewumbo acc
    private var openAI_API_KEY = "sk-proj-er5ZvDChun1D8sxW2ibIT3BlbkFJMLiMvh3hNQDr31fNX9JT"
    private var openAI_Assistant_ID = "asst_hoMTFS1Qdw4ZLhEZLKYTQRfR"

    private lateinit var textViewCalories: TextView
    private lateinit var textViewCarbs: TextView
    private lateinit var textViewProtein: TextView
    private lateinit var textViewFat: TextView
    private lateinit var servingSize: TextView
    private lateinit var textViewExerciseName: TextView
    private lateinit var textViewExerciseNameLoader: LoaderTextView
    private lateinit var layoutIngredients: LinearLayout
    private lateinit var layoutInstructions: LinearLayout

    private lateinit var inflater2: LayoutInflater

    private lateinit var moreButton: ImageButton

    private lateinit var ingredients: String

    private lateinit var workbook: Workbook

    var canBookmarkRecipe = true
    var bookmerked = false

    val averageMaxCals = 2500
    val averageMaxCarbs = 250
    val averageMaxProtein = 100
    val averageMaxFat = 70
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_meal_recipe, container, false)

        textViewCalories = view.findViewById(R.id.textViewCalories)
        textViewCarbs = view.findViewById(R.id.textViewCarbs)
        textViewProtein = view.findViewById(R.id.textViewProtein)
        textViewFat = view.findViewById(R.id.textViewFat)
        servingSize = view.findViewById(R.id.servingSize)
        textViewExerciseName = view.findViewById(R.id.textViewExerciseName)
        textViewExerciseNameLoader = view.findViewById(R.id.textViewExerciseNameLoader)
        layoutIngredients = view.findViewById(R.id.layoutIngredients)
        layoutInstructions = view.findViewById(R.id.layoutInstructions)

        inflater2 = LayoutInflater.from(requireContext())

        moreButton = view.findViewById(R.id.moreButton)

        val args = arguments
        if (args != null) {
            ingredients = args.getString("ingredients").toString()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.IO).launch{
            val mealRecords = async {
                getMealRecords()
            }.await()

            var totalCals = 0.0
            var totalCarbs = 0.0
            var totalProtein = 0.0
            var totalFat = 0.0

            val todaySubset = mealRecords.filter { isToday((it["date"] as Timestamp).toDate()) }

            todaySubset.forEach {
                val name = it.get("foodLabel")
                val cals = it.get("calories").toString().toDouble()
                val carbs = it.get("carbohydrates").toString().toDouble()
                val protein = it.get("protein").toString().toDouble()
                val fat = it.get("fat").toString().toDouble()

                totalCals += cals
                totalCarbs += carbs
                totalProtein += protein
                totalFat += fat

                Log.e("name", name.toString())
            }

            openAPI(
                ingredients,
                averageMaxCals-totalCals.toInt(),
                averageMaxCarbs-totalCarbs.toInt(),
                averageMaxProtein-totalProtein.toInt(),
                averageMaxFat-totalFat.toInt())

        }
    }

    private suspend fun getMealRecords(): ArrayList<Map<String, Any>>{
        val records = CompletableDeferred<ArrayList<Map<String, Any>>>()

        val db = Firebase.firestore
        val user = Firebase.auth.currentUser
        user?.let {
            val uid = it.uid
            val userRef = db.collection("users").document(uid)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        records.complete(
                            document.get("mealRecords") as? ArrayList<Map<String, Any>> ?: ArrayList()
                        )
                    }
                }
        }

        return records.await()
    }

    fun isToday(date: Date): Boolean {
        val todayCalendar = Calendar.getInstance()
        val givenCalendar = Calendar.getInstance()
        givenCalendar.time = date
        return todayCalendar.get(Calendar.YEAR) == givenCalendar.get(Calendar.YEAR) &&
                todayCalendar.get(Calendar.MONTH) == givenCalendar.get(Calendar.MONTH) &&
                todayCalendar.get(Calendar.DAY_OF_MONTH) == givenCalendar.get(Calendar.DAY_OF_MONTH)
    }

    @OptIn(BetaOpenAI::class)
    suspend fun openAPI(ingredients: String, maxCals: Int, maxCarbs: Int, maxProtein: Int, maxFat: Int) {

        // 1. Setup client
        // val token = System.getenv(openAI_API_KEY)
        val openAI = OpenAI(openAI_API_KEY)

        // 2. Create an Assistant
        val assistant = openAI.assistant(id = AssistantId(openAI_Assistant_ID))

        // 3. Create a thread
        val thread = openAI.thread()

        // 4. Add a message to the thread
        openAI.message(
            threadId = thread.id,
            request = MessageRequest(
                role = Role.User,
                content = "{\n" +
                        "  user_prompt: \"${ingredients}\",\n" +
                        "  constraint_calories: ${maxCals},\n" +
                        "  constraint_carbohydrates: ${maxCarbs},\n" +
                        "  constraint_protein: ${maxProtein},\n" +
                        "  constraint_fat: ${maxFat}\n" +
                        "}"
            )
        )
        val messages = openAI.messages(thread.id)
        //println("List of messages in the thread:")
        for (message in messages) {
            val textContent = message.content.first() as? MessageContent.Text ?: error("Expected MessageContent.Text")
            //println(textContent.text.value)
        }

        // 5. Run the assistant
        val run = openAI.createRun(
            thread.id,
            request = RunRequest(
                assistantId = AssistantId(openAI_Assistant_ID),
                //instructions = "Please address the user as Jane Doe. The user has a premium account.",
            )
        )

        // 6. Check the run status
        do {
            delay(1500)
            val retrievedRun = openAI.getRun(threadId = thread.id, runId = run.id)
        } while (retrievedRun.status != Status.Completed)

        // 6. Display the assistant's response
        val assistantMessages = openAI.messages(thread.id)
        //println("\nThe assistant's response:")

        var index = 0
        for (message in assistantMessages) {
            //println("---------------------------------------")
            val textContent = message.content.first() as? MessageContent.Text ?: error("Expected MessageContent.Text")
            //println(textContent.text.value)
            //println("---------------------------------------")

            val gson = Gson()
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(textContent.text.value, mapType)

            if (index != 0){
                continue
            }

            println("asdfasdfsdf")
            println(map.toString())
            println(map["name"])

            val name = map["name"] as? String ?: ""
            val ingredients = map["ingredients"] as? List<String>
            val instructions = map["instructions"] as? List<String>
            val number_of_servings = map["number_of_servings"] as? Double ?: 0
            val total_calories_per_serving = map["total_calories_per_serving"] as? Double ?: 0
            val total_carbohydrates_per_serving = map["total_carbohydrates_per_serving"] as? Double ?: 0
            val total_protein_per_serving = map["total_protein_per_serving"] as? Double ?: 0
            val total_fat_per_serving = map["total_fat_per_serving"] as? Double ?: 0

            withContext(Dispatchers.Main) {
                textViewExerciseName.text = name

                textViewExerciseNameLoader.visibility = View.INVISIBLE
                textViewExerciseName.visibility = View.VISIBLE

                AnimationUtil.animateRecipeTextViewMacros(textViewCalories, "g\nCalories", 0, total_calories_per_serving.toInt(), 1000)
                AnimationUtil.animateRecipeTextViewMacros(textViewCarbs, "g\nCarbs", 0, total_carbohydrates_per_serving.toInt(), 1000)
                AnimationUtil.animateRecipeTextViewMacros(textViewProtein, "g\nProtein", 0, total_protein_per_serving.toInt(), 1000)
                AnimationUtil.animateRecipeTextViewMacros(textViewFat, "g\nFat", 0, total_fat_per_serving.toInt(), 1000)

                servingSize.text = "per serving (makes ${number_of_servings} serving/s)"

                layoutIngredients.removeAllViews()
                if (ingredients != null) {
                    for ((index, item) in ingredients.withIndex()) {

                        if (item == ""){
                            continue
                        }

                        val customItem = inflater2.inflate(R.layout.ordered_list_item, null) as LinearLayout

                        val textViewNumber = customItem.findViewById<TextView>(R.id.textViewNumber)
                        val listNum = index+1
                        val textViewNumberText = "$listNum."
                        textViewNumber.text = textViewNumberText

                        val textViewStep = customItem.findViewById<TextView>(R.id.textViewStep)
                        textViewStep.text = item

                        layoutIngredients.addView(customItem)
                    }
                }

                layoutInstructions.removeAllViews()
                if (instructions != null) {
                    for ((index, item) in instructions.withIndex()) {

                        if (item == ""){
                            continue
                        }

                        val customItem = inflater2.inflate(R.layout.ordered_list_item, null) as LinearLayout

                        val textViewNumber = customItem.findViewById<TextView>(R.id.textViewNumber)
                        val listNum = index+1
                        val textViewNumberText = "$listNum."
                        textViewNumber.text = textViewNumberText

                        val textViewStep = customItem.findViewById<TextView>(R.id.textViewStep)
                        textViewStep.text = item

                        layoutInstructions.addView(customItem)
                    }
                }

                moreButton.setOnClickListener{
                    val popupMenu = PopupMenu(requireContext(), moreButton)
                    popupMenu.inflate(R.menu.recipe_popup_menu) // Create a menu resource file (e.g., res/menu/popup_menu.xml)
                    0

                    popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                        when (item.itemId) {
                            R.id.menu_option1 -> {
                                if (canBookmarkRecipe){
                                    canBookmarkRecipe = false

                                    val builder = AlertDialog.Builder(requireContext())
                                    builder.setMessage("Bookmark this Recipe?")
                                        .setPositiveButton("Confirm") { dialog, _ ->
                                            if (instructions != null) {
                                                if (ingredients != null) {
                                                    updateBookmarkedRecipes(
                                                        name,
                                                        total_calories_per_serving.toDouble(),
                                                        total_carbohydrates_per_serving.toDouble(),
                                                        total_protein_per_serving.toDouble(),
                                                        total_fat_per_serving.toDouble(),
                                                        number_of_servings.toDouble(),
                                                        ingredients,
                                                        instructions)
                                                }
                                            }
                                            dialog.dismiss()
                                            canBookmarkRecipe = true
                                        }
                                        .setNegativeButton("Cancel") { dialog, _ ->
                                            dialog.dismiss()
                                            canBookmarkRecipe = true
                                        }
                                        .setOnDismissListener {
                                            canBookmarkRecipe = true
                                        }
                                        .setOnCancelListener{
                                            canBookmarkRecipe = true
                                        }
                                    val alert = builder.create()
                                    alert.show()
                                }
                                true
                            }
                            R.id.menu_option2 -> {
                                createRecipePDF(
                                    name,
                                    total_calories_per_serving.toString(),
                                    total_carbohydrates_per_serving.toString(),
                                    total_protein_per_serving.toString(),
                                    total_fat_per_serving.toString(),
                                    number_of_servings.toString(),
                                    ingredients as ArrayList<String>,
                                    instructions as ArrayList<String>)
                                createPDFFromWorkbook(requireContext(), workbook, name)
                                Toast.makeText(context, "PDF Generated", Toast.LENGTH_SHORT).show()
                                true
                            }
                            R.id.menu_option3 -> {
                                saveFoodRecord(
                                    name,
                                    number_of_servings.toDouble(),
                                    total_calories_per_serving.toDouble(),
                                    total_carbohydrates_per_serving.toDouble(),
                                    total_protein_per_serving.toDouble(),
                                    total_fat_per_serving.toDouble())
                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                }
            }

            index++
        }

    }

    private fun updateBookmarkedRecipes(foodLabel: String, calories: Double, carbs: Double, protein: Double, fat: Double, quantity: Double, ingredients: List<String>, instructions: List<String>){
        if (bookmerked){
            Toast.makeText(context, "You've already bookmarked this recipe", Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        val db = FirebaseFirestore.getInstance()
        val documentReference = db.collection("users").document(uid.toString())

        val newElement = hashMapOf(
            "foodLabel" to foodLabel,
            "ingredients" to ingredients,
            "instructions" to instructions,
            "calories" to calories,
            "carbs" to carbs,
            "protein" to protein,
            "fat" to fat,
            "quantity" to quantity,
        )

        documentReference.update("bookmarkedRecipes", FieldValue.arrayUnion(newElement))
            .addOnSuccessListener {
                Log.d(LoginActivity.TAG, "DocumentSnapshot successfully updated!")
                Toast.makeText(context, "Bookmarked Recipe", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w(LoginActivity.TAG, "Error updating document", e)
            }

        bookmerked = true
    }

    private fun saveFoodRecord(foodLabel: String, quantity: Double, calories: Double, carbohydrates: Double, protein: Double, fat: Double){
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
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
            }
            .setOnCancelListener{
            }
        val alert = builder.create()
        alert.show()

    }

    fun roundToTwoDecimalPlaces(number: Double): Double {
        return "%.${2}f".format(number).toDouble()
    }

    fun createRecipePDF(name: String, calories: String, carbs: String, protein: String, fat: String, servings: String, ingredients: ArrayList<String>, instructions: ArrayList<String>) {
        // Create a new Excel workbook
        workbook = HSSFWorkbook() // uses a global variable, var workbook: Workbook

        // Create a sheet in the workbook
        val sheet: Sheet = workbook.createSheet("Sheet1")

        var index = 0

        // name
        var row: Row = sheet.createRow(index)
        var cell: Cell = row.createCell(0)
        cell.setCellValue("Name")
        var cell2: Cell = row.createCell(1)
        cell2.setCellValue(name)
        index++

        // calories
        row = sheet.createRow(index)
        cell = row.createCell(0)
        cell.setCellValue("Calories")
        cell2 = row.createCell(1)
        cell2.setCellValue("$calories kcal")
        index++

        // carbs
        row = sheet.createRow(index)
        cell = row.createCell(0)
        cell.setCellValue("Carbs")
        cell2 = row.createCell(1)
        cell2.setCellValue("$carbs grams")
        index++

        // protein
        row = sheet.createRow(index)
        cell = row.createCell(0)
        cell.setCellValue("Protein")
        cell2 = row.createCell(1)
        cell2.setCellValue("$protein grams")
        index++

        // fat
        row = sheet.createRow(index)
        cell = row.createCell(0)
        cell.setCellValue("Fat")
        cell2 = row.createCell(1)
        cell2.setCellValue("$fat grams")
        index++

        // servings
        row = sheet.createRow(index)
        cell = row.createCell(0)
        cell.setCellValue("Servings")
        cell2 = row.createCell(1)
        cell2.setCellValue("$servings servings")
        index++

        // ingredients
        row = sheet.createRow(index)
        cell = row.createCell(0)
        cell.setCellValue("Ingredients")
        index++

        // loop to populate sheet
        for (item in ingredients) {
            val row: Row = sheet.createRow(index)
            val cell: Cell = row.createCell(0)
            cell.setCellValue(item)
            index++
        }

        // instructions
        row = sheet.createRow(index)
        cell = row.createCell(0)
        cell.setCellValue("Instructions")
        index++

        for (item in instructions) {
            val row: Row = sheet.createRow(index)
            val cell: Cell = row.createCell(0)
            cell.setCellValue(item)
            index++
        }
    }

    fun createPDFFromWorkbook(context: Context, workbook: Workbook, pdfFileName: String) {
        val externalStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        val pdfFilePath = File(externalStorageDir, pdfFileName).toString()

        val document = Document(PageSize.LETTER)
        document.setMargins(72f, 72f, 72f, 72f)
        val pdfWriter = PdfWriter.getInstance(document, FileOutputStream(pdfFilePath))
        document.open()

        val baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED)
        val contentByte: PdfContentByte = pdfWriter.directContent

        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)

            document.newPage()
            contentByte.setFontAndSize(baseFont, 12f)

            // Track the vertical position on the current page
            var verticalPosition = document.top()
            var horizontalPosition = document.left()
            val lineSpacing = 3
            val lineHeight = 20
            val entriesPerColumn = 25
            val columnsPerPage = 2
            val columnSpacing = 300f

            var currentColumn = 1

            // show column labels
            val rowLabels = sheet.getRow(0)
            for (cellIndex in 0 until rowLabels.physicalNumberOfCells) {
                val cell = rowLabels.getCell(cellIndex)
                contentByte.setTextMatrix(cellIndex.toFloat() * 100 + horizontalPosition, verticalPosition)
                contentByte.showText(cell.toString())
            }
            // Update the vertical position for the next row
            verticalPosition -= (lineSpacing + lineHeight)

            for (rowIndex in 1 until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(rowIndex)

                // Check if the row will fit on the current page
                if (rowIndex % entriesPerColumn == 1 && rowIndex != 1) {
                    currentColumn++
                    // check if column number has exceeded columns per page
                    // if not, adjust horizontal position to the next column
                    if (currentColumn <= columnsPerPage){
                        horizontalPosition += columnSpacing
                        verticalPosition = document.top()
                    } else { // if it has exceeded
                        // create a new page
                        document.newPage()
                        contentByte.setFontAndSize(baseFont, 12f)
                        verticalPosition = document.top()
                        horizontalPosition = document.left()
                        currentColumn = 1
                    }

                    // show column labels
                    val rowLabels = sheet.getRow(0)
                    for (cellIndex in 0 until rowLabels.physicalNumberOfCells) {
                        val cell = rowLabels.getCell(cellIndex)
                        contentByte.setTextMatrix(cellIndex.toFloat() * 100 + horizontalPosition, verticalPosition)
                        contentByte.showText(cell.toString())
                    }

                    // Update the vertical position for the next row
                    verticalPosition -= (lineSpacing + lineHeight)
                    println("vertical position: $verticalPosition")
                }

                // show cell item
                for (cellIndex in 0 until row.physicalNumberOfCells) {
                    val cell = row.getCell(cellIndex)

                    contentByte.setTextMatrix(cellIndex.toFloat() * 100 + horizontalPosition, verticalPosition)
                    contentByte.showText(cell.toString())
                }

                // Update the vertical position for the next row
                verticalPosition -= (lineSpacing + lineHeight)
                println("vertical position: $verticalPosition")
            }

        }

        document.close()

        // Use MediaStore to insert the PDF file into the database
        val contentValues = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, pdfFileName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val pdfUri = context.contentResolver.insert(contentUri, contentValues)

        pdfUri?.let {
            try {
                val outputStream = context.contentResolver.openOutputStream(pdfUri)
                if (outputStream != null) {
                    val inputStream = File(pdfFilePath).inputStream()
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        viewLifecycleOwner.lifecycleScope.cancel() // Cancel all coroutines associated with the LifecycleScope
        super.onDestroyView()
    }

}