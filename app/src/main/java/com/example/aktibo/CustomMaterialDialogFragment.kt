package com.example.aktibo

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.aktibo.ml.ModelV23
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CustomMaterialDialogFragment : DialogFragment() {

    val PICK_IMAGE_REQUEST = 1
    val CAMERA_REQUEST = 2
    private val MY_PERMISSIONS_REQUEST_CAMERA = 345
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 456

    // TensorFlow
    val imageSize = 256

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.custom_material_dialog_layout, null)

        val closeButton = view.findViewById<View>(R.id.closeButton)
        val editText = view.findViewById<TextInputEditText>(R.id.editText)
        val button_capture = view.findViewById<View>(R.id.button_capture)
        val button_confirm = view.findViewById<View>(R.id.button_confirm)

        closeButton.setOnClickListener {
            dismiss()
        }

        button_capture.setOnClickListener() {
            selectImage()
        }

        button_confirm.setOnClickListener {
            val foodDescription = editText.text.toString().trim()
            if (foodDescription == ""){
                Toast.makeText(context, "Input a food description or upload a picture", Toast.LENGTH_SHORT).show();
            } else if (foodDescription.length < 2){
                Toast.makeText(context, "Food description must have at least 2 characters", Toast.LENGTH_SHORT).show();
            } else {
                replaceFragmentWithAnimAndData(foodDescription)
                dismiss()
            }
        }

        builder.setView(view)

        return builder.create()
    }


    fun selectImage() {
        val options = arrayOf("Select from Gallery", "Take a Photo")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Image")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    checkAndRequestExternalStoragePermission()
                }
                1 -> {
                    checkAndRequestCameraPermission()

                }
            }
        }
        builder.show()
    }

    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                MY_PERMISSIONS_REQUEST_CAMERA
            )
        } else {
            // Permission is already granted
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_REQUEST)
        }
    }

    private fun checkAndRequestExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            if (Build.VERSION.SDK_INT >= 33){
                requestPermissions(
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                )
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                )
            }

        } else {
            // Permission is already granted
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {

            MY_PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with capturing image
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, CAMERA_REQUEST)
                } else {
                    // Permission denied, handle it gracefully (e.g., show a message to the user)
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }

            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with capturing image
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(intent, PICK_IMAGE_REQUEST)
                } else {
                    // Permission denied, handle it gracefully (e.g., show a message to the user)
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }

        }
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    // Handle image selection from gallery

                    val selectedImage = data?.data // image URI
                    val selectedImageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedImage)
                    val imageRescaled = Bitmap.createScaledBitmap(selectedImageBitmap, imageSize, imageSize, false)

                    classifyImage(imageRescaled)
                }

                CAMERA_REQUEST -> {
                    // Handle captured photo from the camera
                    val photo = data?.extras?.get("data") as Bitmap
                    val imageRescaled = Bitmap.createScaledBitmap(photo, imageSize, imageSize, false)

                    classifyImage(imageRescaled)
                }
            }
        }
    }

    private fun classifyImage(image: Bitmap) {
        try {
            val model = ModelV23.newInstance(requireContext())

            // Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)

            // bytes a float takes * image length * image width * rgb colors
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
            var pixel = 0
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val rgbVal = intValues[pixel++] // RGB
                    byteBuffer.putFloat((rgbVal shr 16 and 0xFF) * (1f / 255))
                    byteBuffer.putFloat((rgbVal shr 8 and 0xFF) * (1f / 255))
                    byteBuffer.putFloat((rgbVal and 0xFF) * (1f / 255))
                }
            }

            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val foodClassLabels = arrayOf(
                "adobo", "apple_pie", "arroz_caldo", "baby_back_ribs", "bagnet", "baklava", "balut", "bangus",
                "beef_carpaccio", "beef_tartare", "beet_salad", "beignets", "bibimbap", "bibingka", "bicol_express",
                "bread_pudding", "breakfast_burrito", "bruschetta", "buko_pie", "bulalo", "caesar_salad", "cannoli",
                "caprese_salad", "carrot_cake", "ceviche", "champorado", "cheese_plate", "cheesecake", "chicken_curry",
                "chicken_inasal", "chicken_quesadilla", "chicken_wings", "chocolate_cake", "chocolate_mousse", "churros",
                "clam_chowder", "club_sandwich", "crab_cakes", "creme_brulee", "crispy_pata", "croque_madame", "cup_cakes",
                "deviled_eggs", "dinuguan", "donuts", "dumplings", "edamame", "eggs_benedict", "ensaymada", "escargots",
                "falafel", "filet_mignon", "fish_and_chips", "foie_gras", "french_fries", "french_onion_soup", "french_toast",
                "fried_calamari", "fried_rice", "frozen_yogurt", "garlic_bread", "ginataang_gulay", "gnocchi", "greek_salad",
                "grilled_cheese_sandwich", "grilled_salmon", "guacamole", "gulaman", "gyoza", "halo-halo", "hamburger",
                "hipon", "hot_and_sour_soup", "hot_dog", "huevos_rancheros", "hummus", "ice_cream", "ilocos_empanada",
                "inihaw_na_Liempo", "isaw", "kaldereta", "kare-kare", "kwek-kwek", "laing", "lasagna", "leche_flan", "lechon",
                "lobster_bisque", "lobster_roll_sandwich", "longaniza", "lumpia", "macaroni_and_cheese", "macarons", "mami",
                "miso_soup", "monggo", "mussels", "nachos", "omelette", "onion_rings", "oysters", "pad_thai", "paella",
                "pancakes", "pancit_habhab", "pancit_palabok", "pandesal", "panna_cotta", "pares", "pastillas", "peking_duck",
                "pho", "pinakbet", "pizza", "pork_barbecue", "pork_chop", "poutine", "prime_rib", "pulled_pork_sandwich",
                "puto_bumbong", "ramen", "ravioli", "red_velvet_cake", "risotto", "samosa", "sashimi", "scallops",
                "seaweed_salad", "shrimp_and_grits", "sinigang", "sisig", "spaghetti_bolognese", "spaghetti_carbonara",
                "spring_rolls", "steak", "strawberry_sho rtcake", "suman", "sushi", "tacos", "taho", "takoyaki", "talangka",
                "tapa", "tinola", "tiramisu", "tokwa_t_baboy", "tortang_talong", "tuna_tartare", "turon", "ube", "waffles"
            )

            val confidences = outputFeature0.floatArray
            // find the index of the class with the biggest confidence.
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidences.indices) {
                println(foodClassLabels[i])
                println(confidences[i])
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i]
                    maxPos = i
                }
            }

            val indexOfMaxValue = confidences.indexOfFirst { it == confidences.maxOrNull() }
            val result = foodClassLabels[indexOfMaxValue]

            val prediction = replaceUnderscoresWithSpaces(result)

            Toast.makeText(requireContext(), "Image AI thinks it's ${prediction} with ${String.format("%.2f", maxConfidence*100).toDouble()}% confidence.", Toast.LENGTH_SHORT).show()

            replaceFragmentWithAnimAndData(prediction)
            dismiss()

            // Releases model resources if no longer used.
            model.close()
        } catch (e: Exception){
            // TODO:  error message here
        }
    }

    fun replaceUnderscoresWithSpaces(input: String): String {
        return input.replace('_', ' ')
    }

    private fun replaceFragmentWithAnimAndData(foodDescription: String) {
        val fragment = NewFoodRecordFragment()
        val args = Bundle()
        args.putString("foodDescription", foodDescription)
        fragment.setArguments(args)

        val fragmentManager = getParentFragmentManager()
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
