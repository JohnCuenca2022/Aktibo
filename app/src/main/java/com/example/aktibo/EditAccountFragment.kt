package com.example.aktibo

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.Nullable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.aktibo.LoginActivity.Companion.TAG
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID


class EditAccountFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var usernameInput: TextInputEditText
    private val MY_PERMISSIONS_REQUEST_CAMERA = 345
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 456

    private lateinit var cardView: CardView

    private lateinit var textWarningCardView: CardView
    private lateinit var textWarningTextView: TextView

    private lateinit var saveButtonProgressBar: ProgressBar

    val workflow = "wfl_f5yBhhGuUn9BpIyZwaJMZ"
    val apiUser = "1227574749"
    val apiSecret = "NLvWeA9iUfYBqg6rMyx6VsaJXy"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit_account, container, false)

        //show username and profile image
        val args = arguments
        if (args != null) {
            val username = args.getString("username")
            val imageURL = args.getString("imageURL")

            usernameInput = view.findViewById(R.id.username)
            imageView = view.findViewById(R.id.userProfileImage)

            // Use the retrieved username and userImage
            usernameInput.setText(username)

            if (imageURL != "") {
                Picasso.get()
                    .load(imageURL)
                    .placeholder(R.drawable.placeholder_image) // Optional placeholder image
                    .into(imageView)
            }
        }

        //change user profile image
        cardView = view.findViewById(R.id.cardView)
        cardView.setOnClickListener{
            selectImage(view)
        }

        //change username
        saveButtonProgressBar = view.findViewById(R.id.saveButtonProgressBar)

        val button_save = view.findViewById<Button>(R.id.button_save)
        button_save.setOnClickListener{
            hideTextWarningMessage()
            disableAllInputs()
            disableButton(button_save)

            val newUsername = usernameInput.text.toString().trim()

            val currentUser = FirebaseAuth.getInstance().currentUser
            val db = Firebase.firestore
            val userRef = currentUser?.let { it1 -> db.collection("users").document(it1.uid) }

            if (userRef != null) {
                if (newUsername.length < 3){
                    Toast.makeText(context, "New username must have at least 3 characters", Toast.LENGTH_SHORT).show()

                    enableButton(button_save)
                    enableAllInputs()
                } else {
                    CoroutineScope(IO).launch{
                        val isSafeEnglish = async {
                            checkProfanity(newUsername, "en", apiUser, apiSecret)
                        }.await()

                        if (!isSafeEnglish){ // stop if has profanity in English or an error happened
                            withContext(Dispatchers.Main) {
                                enableButton(button_save)
                                enableAllInputs()
                            }
                            return@launch
                        }

                        val isSafeFilipino = async {
                            checkProfanity(newUsername, "tl", apiUser, apiSecret)
                        }.await()

                        if (!isSafeFilipino){ // stop if has profanity in Tagalog/Filipino or an error happened
                            withContext(Dispatchers.Main) {
                                enableButton(button_save)
                                enableAllInputs()
                            }
                            return@launch
                        }

                        userRef
                            .update("username", newUsername)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                                val fragmentManager = getParentFragmentManager()
                                fragmentManager.popBackStack()
                            }
                            .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
                    }
                }
            }

        }

        textWarningCardView = view.findViewById(R.id.textWarningCardView)
        textWarningTextView = view.findViewById(R.id.textWarningTextView)

        return view
    }

    suspend fun analyzeImage(context: Context, imageUri: Uri, workflow: String, apiUser: String, apiSecret: String): Boolean {
        val isPassing = CompletableDeferred<Boolean>()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.sightengine.com/1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(SightEngineService::class.java)

        val contentResolver: ContentResolver = context.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(imageUri)

        if (inputStream != null) {
            val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), inputStream.readBytes())
            val imagePart = MultipartBody.Part.createFormData("media", "image.jpg", requestBody)

            val call = service.checkWorkflow(imagePart, workflow, apiUser, apiSecret)
            call.enqueue(object : Callback<SightEngineResponse> {
                override fun onResponse(call: Call<SightEngineResponse>, response: Response<SightEngineResponse>) {
                    if (response.isSuccessful) {
                        val sightEngineResponse = response.body()
                        if (sightEngineResponse != null) {
                            // Handle the response from SightEngine here
                            val status = sightEngineResponse.status
                            val summaryAction = sightEngineResponse.summary.action
                            val summaryReason = sightEngineResponse.summary.reject_reason


                            // Process the response as needed
                            Log.e("SightEngine API Status", status)
                            Log.e("SightEngine API Action", summaryAction)
                            summaryReason.forEach { item ->
                                Log.e("SightEngine API Violations", "${item.id}, ${item.text}")
                            }
                            println(sightEngineResponse.toString())

                            // image has flagged image
                            if (summaryAction == "reject"){
                                var warningMessage = "User Guidelines Violation.\nImage contains:"
                                summaryReason.forEach { item ->
                                    val reasonText = " ${item.text},"
                                    warningMessage += reasonText
                                }
                                warningMessage = warningMessage.removeSuffix(",")
                                textWarningTextView.text = warningMessage
                                textWarningCardView.visibility = View.VISIBLE
                                isPassing.complete(false)
                            } else {
                                textWarningCardView.visibility = View.GONE
                                isPassing.complete(true)
                            }

                        } else {
                            Log.e("SightEngine API", "Empty response body")
                            Toast.makeText(requireContext(), "Failed to scan image. Please try again.", Toast.LENGTH_SHORT).show()
                            isPassing.complete(false)
                        }
                    } else {
                        // Handle the error
                        Log.e("SightEngine API", "Error: ${response.code()}")
                        Toast.makeText(requireContext(), "Failed to scan image. Please try again.", Toast.LENGTH_SHORT).show()
                        isPassing.complete(false)
                    }
                }

                override fun onFailure(call: Call<SightEngineResponse>, t: Throwable) {
                    // Handle network errors here
                    Log.e("SightEngine API", "Network error: ${t.message}")
                    Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                    isPassing.complete(false)
                }
            })
        } else {
            Log.e("SightEngine API", "Failed to open InputStream for image URI")
            Toast.makeText(requireContext(), "Failed to scan image. Please try again.", Toast.LENGTH_SHORT).show()
        }
        return isPassing.await()
    }

    suspend fun analyzeBitmap(context: Context, bitmap: Bitmap, workflow: String, apiUser: String, apiSecret: String): Boolean {
        val isPassing = CompletableDeferred<Boolean>()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.sightengine.com/1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(SightEngineService::class.java)

        // Convert Bitmap to byte array
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val bitmapData = byteArrayOutputStream.toByteArray()

        val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), bitmapData)
        val imagePart = MultipartBody.Part.createFormData("media", "image.jpg", requestBody)

        val call = service.checkWorkflow(imagePart, workflow, apiUser, apiSecret)
        call.enqueue(object : Callback<SightEngineResponse> {
            override fun onResponse(call: Call<SightEngineResponse>, response: Response<SightEngineResponse>) {
                if (response.isSuccessful) {
                    val sightEngineResponse = response.body()
                    if (sightEngineResponse != null) {
                        // Handle the response from SightEngine here
                        val status = sightEngineResponse.status
                        val summaryAction = sightEngineResponse.summary.action
                        val summaryReason = sightEngineResponse.summary.reject_reason


                        // Process the response as needed
                        Log.e("SightEngine API Status", status)
                        Log.e("SightEngine API Action", summaryAction)
                        summaryReason.forEach { item ->
                            Log.e("SightEngine API Violations", "${item.id}, ${item.text}")
                        }
                        println(sightEngineResponse.toString())

                        // image has flagged image
                        if (summaryAction == "reject"){
                            var warningMessage = "User Guidelines Violation.\nImage contains:"
                            summaryReason.forEach { item ->
                                val reasonText = " ${item.text},"
                                warningMessage += reasonText
                            }
                            warningMessage = warningMessage.removeSuffix(",")
                            textWarningTextView.text = warningMessage
                            textWarningCardView.visibility = View.VISIBLE
                            isPassing.complete(false)
                        } else {
                            textWarningCardView.visibility = View.GONE
                            isPassing.complete(true)
                        }

                    } else {
                        Log.e("SightEngine API", "Empty response body")
                        Toast.makeText(requireContext(), "Failed to scan image. Please try again.", Toast.LENGTH_SHORT).show()
                        isPassing.complete(false)
                    }
                } else {
                    // Handle the error
                    Log.e("SightEngine API", "Error: ${response.code()}")
                    Toast.makeText(requireContext(), "Failed to scan image. Please try again.", Toast.LENGTH_SHORT).show()
                    isPassing.complete(false)
                }
            }

            override fun onFailure(call: Call<SightEngineResponse>, t: Throwable) {
                // Handle network errors here
                Log.e("SightEngine API", "Network error: ${t.message}")
                Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                isPassing.complete(false)
            }
        })
        return isPassing.await()
    }

    private fun hideTextWarningMessage(){
        val warningMessage = "User Guidelines Violation.\nInappropriate words: "
        textWarningCardView.visibility = View.GONE
        textWarningTextView.text = warningMessage
    }

    private fun disableButton(button: Button) {
        button.isEnabled = false
        button.isClickable = false
        saveButtonProgressBar.visibility = View.VISIBLE
    }
    private fun enableButton(button: Button) {
        button.isEnabled = true
        button.isClickable = true
        saveButtonProgressBar.visibility = View.GONE
    }

    private fun disableAllInputs(){
        usernameInput.isEnabled = false
        cardView.isEnabled = false
    }

    private fun enableAllInputs(){
        usernameInput.isEnabled = true
        cardView.isEnabled = true
    }

    val PICK_IMAGE_REQUEST = 1
    val CAMERA_REQUEST = 2

    fun selectImage(view: View) {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    // Handle image selection from gallery
                    hideTextWarningMessage()
                    val selectedImage = data?.data

                    if (selectedImage != null){
                        CoroutineScope(IO).launch{
                            val isImageSafe = async {
                                analyzeImage(requireContext(), selectedImage, workflow, apiUser, apiSecret)
                            }.await()

                            if (!isImageSafe){ // stop if has violation or an error happened
                                return@launch
                            }
                            uploadImageToFirebase(selectedImage)
                        }
                    }


                }
                CAMERA_REQUEST -> {
                    // Handle captured photo from the camera
                    hideTextWarningMessage()
                    val photo = data?.extras?.get("data") as Bitmap

                    CoroutineScope(IO).launch{
                        val isImageSafe = async {
                            analyzeBitmap(requireContext(), photo, workflow, apiUser, apiSecret)
                        }.await()

                        if (!isImageSafe){ // stop if has violation or an error happened
                            return@launch
                        }
                        uploadBitmapToFirebase(photo)
                    }
                }
            }
        }
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
        println(requestCode)
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

    private suspend fun checkProfanity(text: String, lang: String, apiKey: String, apiSecret: String): Boolean {
        val isPassing = CompletableDeferred<Boolean>()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.sightengine.com/1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(SightEngineServiceText::class.java)

        val call = service.checkText(text, "standard", lang, "us,gb,fr,ph", apiKey, apiSecret)
        call.enqueue(object : Callback<SightEngineResponseText> {
            override fun onResponse(call: Call<SightEngineResponseText>, response: Response<SightEngineResponseText>) {
                if (response.isSuccessful) {
                    val sightEngineResponseText = response.body()
                    if (sightEngineResponseText != null) {
                        val status = sightEngineResponseText.status
                        val profanityMatches = sightEngineResponseText.profanity.matches

                        // Handle the profanity check results here
                        Log.e("SightEngine API Text Status", status)
                        Log.e("SightEngine API Text Profanity Matches", profanityMatches.toString())

                        if (profanityMatches.isEmpty()){
                            Log.e("SightEngine API Text Profanity", "no profanity")
                            isPassing.complete(true)
                        } else {
                            // text has flagged words
                            var warningMessage = "User Guidelines Violation.\nInappropriate username:"
                            profanityMatches.forEach { item ->
                                val reasonText = " ${item.match},"
                                warningMessage += reasonText
                                println(warningMessage)
                            }
                            warningMessage = warningMessage.removeSuffix(",")

                            textWarningTextView.text = warningMessage
                            textWarningCardView.visibility = View.VISIBLE

                            isPassing.complete(false)
                        }

                    }
                } else {
                    Log.e("SightEngine API Text", "Empty response body")
                    Toast.makeText(requireContext(), "Failed to scan text. Please try again.", Toast.LENGTH_SHORT).show()
                    isPassing.complete(false)
                }
            }

            override fun onFailure(call: Call<SightEngineResponseText>, t: Throwable) {
                Log.e("SightEngine API Text", "Network error: ${t.message}")
                Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                isPassing.complete(false)
            }
        })

        return isPassing.await()
    }

    private suspend fun uploadImageToFirebase(imageUri: Uri?) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show()
        }
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = Firebase.firestore

        val inputStream = imageUri?.let { context?.contentResolver?.openInputStream(it) }
        val buffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val bufferData = ByteArray(bufferSize)
        var bytesRead: Int = 0
        var totalBytes = 0

        while (inputStream?.read(bufferData, 0, bufferSize).also {
                if (it != null) {
                    bytesRead = it
                }
            } != -1) {
            buffer.write(bufferData, 0, bytesRead)
            totalBytes += bytesRead

            // Check if the total size exceeds 3MB (3 * 1024 * 1024 bytes)
            if (totalBytes > 3 * 1024 * 1024) {
                // Handle the case where the image size exceeds the limit
                // You can show an error message to the user
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Image size must not exceed 3MB", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        if (imageUri != null) {
            val imageRef = storageRef.child("user_images/${currentUser?.uid}/${UUID.randomUUID()}")

            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Image upload success
                    // You can get the download URL here
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        // Handle the URL as needed, e.g., save it to a database

                        val userRef = currentUser?.let { it1 -> db.collection("users").document(it1.uid) }

                        if (userRef != null) {
                            userRef
                                .update("userImage", imageUrl)
                                .addOnSuccessListener {
                                    imageView.setImageURI(imageUri)
                                    Toast.makeText(context, "Profile Image Updated", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
                        }

                    }
                }
                .addOnFailureListener { exception ->
                    // Handle unsuccessful upload
                    // You can log an error message or notify the user
                    Toast.makeText(context, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private suspend fun uploadBitmapToFirebase(bitmap: Bitmap) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show()
        }
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = Firebase.firestore
        val imageRef = storageRef.child("user_images/${currentUser?.uid}/${UUID.randomUUID()}.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        if (data.size <= (3 * 1024 * 1024)) {
            val uploadTask = imageRef.putBytes(data)

            uploadTask.addOnSuccessListener { taskSnapshot ->
                // Image upload success
                // You can get the download URL here
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    val userRef = currentUser?.let { it1 -> db.collection("users").document(it1.uid) }

                    if (userRef != null) {
                        userRef
                            .update("userImage", imageUrl)
                            .addOnSuccessListener {
                                imageView.setImageBitmap(bitmap)
                                Toast.makeText(context, "Profile Image Updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
                    }
                }
            }.addOnFailureListener { exception ->
                // Handle unsuccessful upload
                // You can log an error message or notify the user
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image size must not exceed 3MB", Toast.LENGTH_SHORT).show()
            }
        }


    }

}