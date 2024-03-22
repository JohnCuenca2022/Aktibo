package com.example.aktibo

import android.content.ContentValues
import android.content.Context
import android.icu.text.SimpleDateFormat
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
import com.elyeproj.loaderviewlibrary.LoaderTextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfWriter
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

class MealRecipeFragmentViewing : Fragment() {

    private lateinit var textViewCalories: TextView
    private lateinit var textViewCarbs: TextView
    private lateinit var textViewProtein: TextView
    private lateinit var textViewFat: TextView
    private lateinit var servingSize: TextView
    private lateinit var textViewExerciseName: TextView
    private lateinit var textViewExerciseNameLoader: LoaderTextView
    private lateinit var layoutIngredients: LinearLayout
    private lateinit var layoutInstructions: LinearLayout

    private lateinit var moreButton: ImageButton

    private lateinit var foodLabel: String
    private lateinit var calories: String
    private lateinit var carbs: String
    private lateinit var protein: String
    private lateinit var fat: String
    private lateinit var ingredients: ArrayList<String>
    private lateinit var instructions: ArrayList<String>
    private lateinit var quantity: String
    var bookmerked = false
    private lateinit var inflater2: LayoutInflater
    private lateinit var workbook: Workbook

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_meal_recipe_viewing, container, false)

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
            foodLabel = args.getString("foodLabel").toString()
            calories = args.getString("calories").toString()
            carbs = args.getString("carbs").toString()
            protein = args.getString("protein").toString()
            fat = args.getString("fat").toString()
            ingredients = args.getStringArrayList("ingredients")!!
            instructions = args.getStringArrayList("instructions")!!
            quantity = args.getString("quantity").toString()
            bookmerked = args.getBoolean("bookmerked")
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewExerciseName.text = foodLabel

        textViewExerciseNameLoader.visibility = View.INVISIBLE
        textViewExerciseName.visibility = View.VISIBLE

        AnimationUtil.animateRecipeTextViewMacros(textViewCalories, "g\nCalories", 0, calories.toDouble().toInt(), 1000)
        AnimationUtil.animateRecipeTextViewMacros(textViewCarbs, "g\nCarbs", 0, carbs.toDouble().toInt(), 1000)
        AnimationUtil.animateRecipeTextViewMacros(textViewProtein, "g\nProtein", 0, protein.toDouble().toInt(), 1000)
        AnimationUtil.animateRecipeTextViewMacros(textViewFat, "g\nFat", 0, fat.toDouble().toInt(), 1000)

        servingSize.text = "per serving (makes ${quantity} serving/s)"

        layoutIngredients.removeAllViews()
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

        layoutInstructions.removeAllViews()
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

        moreButton.setOnClickListener{
            val popupMenu = PopupMenu(requireContext(), moreButton)
            popupMenu.inflate(R.menu.recipe_popup_menu) // Create a menu resource file (e.g., res/menu/popup_menu.xml)
            0

            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_option1 -> {
                        if (!bookmerked){
                            bookmerked = true

                            val builder = AlertDialog.Builder(requireContext())
                            builder.setMessage("Bookmark this Recipe?")
                                .setPositiveButton("Confirm") { dialog, _ ->
                                    updateBookmarkedRecipes(
                                        foodLabel,
                                        calories.toDouble(),
                                        carbs.toDouble(),
                                        protein.toDouble(),
                                        fat.toDouble(),
                                        quantity.toDouble(),
                                        ingredients,
                                        instructions,
                                        true)
                                    dialog.dismiss()
                                    bookmerked = true
                                }
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                    bookmerked = false
                                }
                                .setOnDismissListener {
                                    bookmerked = false
                                }
                                .setOnCancelListener{
                                    bookmerked = false
                                }
                            val alert = builder.create()
                            alert.show()
                        } else {
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setMessage("You've already bookmarked this recipe, do you want to remove it from your bookmarks?")
                                .setPositiveButton("Confirm") { dialog, _ ->
                                    updateBookmarkedRecipes(
                                        foodLabel,
                                        calories.toDouble(),
                                        carbs.toDouble(),
                                        protein.toDouble(),
                                        fat.toDouble(),
                                        quantity.toDouble(),
                                        ingredients,
                                        instructions,
                                        false)
                                    dialog.dismiss()
                                    bookmerked = true
                                }
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                    bookmerked = false
                                }
                                .setOnDismissListener {
                                    bookmerked = false
                                }
                                .setOnCancelListener{
                                    bookmerked = false
                                }
                            val alert = builder.create()
                            alert.show()
                        }
                        true
                    }
                    R.id.menu_option2 -> {
                        createRecipePDF(
                            foodLabel,
                            calories.toDouble().toString(),
                            carbs.toDouble().toString(),
                            protein.toDouble().toString(),
                            fat.toDouble().toString(),
                            quantity.toDouble().toString(),
                            ingredients,
                            instructions)
                        createPDFFromWorkbook(requireContext(), workbook, foodLabel)
                        Toast.makeText(context, "PDF Generated", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.menu_option3 -> {
                        saveFoodRecord(foodLabel, quantity.toDouble(), calories.toDouble(), carbs.toDouble(), protein.toDouble(), fat.toDouble())
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
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

    private fun updateBookmarkedRecipes(
        foodLabel: String,
        calories: Double,
        carbs: Double,
        protein: Double,
        fat: Double,
        quantity: Double,
        ingredients: ArrayList<String>,
        instructions: ArrayList<String>,
        bookmarkIt: Boolean
        ){

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

        if (bookmarkIt) {
            documentReference.update("bookmarkedRecipes", FieldValue.arrayUnion(newElement))
                .addOnSuccessListener {
                    Log.d(LoginActivity.TAG, "DocumentSnapshot successfully updated!")
                    Toast.makeText(context, "Bookmarked Recipe", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.w(LoginActivity.TAG, "Error updating document", e)
                }
        } else {
            documentReference.update("bookmarkedRecipes", FieldValue.arrayRemove(newElement))
                .addOnSuccessListener {
                    Log.d(LoginActivity.TAG, "DocumentSnapshot successfully updated!")
                    Toast.makeText(context, "Removed Recipe from Bookmarks", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.w(LoginActivity.TAG, "Error updating document", e)
                }
        }



        bookmerked = true
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
}