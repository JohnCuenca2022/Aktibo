package com.example.aktibo

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
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
import androidx.core.content.ContextCompat
import androidx.core.view.KeyEventDispatcher.dispatchKeyEvent
import com.example.aktibo.LoginActivity.Companion.TAG
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
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

class NewMomentFragment : Fragment() {

    private lateinit var newMomentCaption: TextInputEditText

    private lateinit var imageCardView: CardView
    private lateinit var newMomentImage: ImageView
    private lateinit var newMomentImageUri: Uri
    private lateinit var newMomentBitmap: Bitmap
    private lateinit var placeholderImage: ImageView

    private lateinit var imageWarningCardView: CardView
    private lateinit var imageWarningTextView: TextView

    private lateinit var textWarningCardView: CardView
    private lateinit var textWarningTextView: TextView

    private lateinit var createNewMomentButton: Button
    private lateinit var createNewMomentButtonProgressBar: ProgressBar

    val workflow = "wfl_f5yBhhGuUn9BpIyZwaJMZ"
    val apiUser = "1227574749"
    val apiSecret = "NLvWeA9iUfYBqg6rMyx6VsaJXy"

    val maxImageSizeinMB = 3.0

    private val MY_PERMISSIONS_REQUEST_CAMERA = 345
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 456

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_new_moment, container, false)

        newMomentCaption = view.findViewById(R.id.newMomentCaption)
        newMomentCaption.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                hideTextWarningMessage()
            }
        }

        imageCardView = view.findViewById(R.id.imageCardView)
        newMomentImage = view.findViewById(R.id.newMomentImage)
        placeholderImage = view.findViewById(R.id.placeholderImage)

        imageCardView.setOnClickListener{
            selectImage(view)
        }

        imageWarningCardView = view.findViewById(R.id.imageWarningCardView)
        imageWarningTextView = view.findViewById(R.id.imageWarningTextView)

        textWarningCardView = view.findViewById(R.id.textWarningCardView)
        textWarningTextView = view.findViewById(R.id.textWarningTextView)

        createNewMomentButton = view.findViewById(R.id.createNewMomentButton)
        createNewMomentButton.setOnClickListener{
            createMoment()
        }
        createNewMomentButtonProgressBar = view.findViewById(R.id.createNewMomentButtonProgressBar)

        return view
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
                    hideImageWarningMessage()
                    placeholderImage.visibility = View.GONE

                    // Handle image selection from gallery
                    val helper = MyHelperFunctions()
                    val selectedImage = data?.data // image URI

                    // get image size
                    val imageSize = selectedImage?.let {
                        helper.getImageSizeInMB(requireContext().contentResolver,
                            it
                        )
                    }

                    // check if image size is greater than max image size
                    if (imageSize != null) {
                        if (imageSize >= maxImageSizeinMB) {
                            Toast.makeText(requireContext(), "Image size cannot exceed 3MB.", Toast.LENGTH_SHORT).show()
                        } else {
                            newMomentImage.setImageURI(selectedImage) // change image view
                            newMomentImageUri = selectedImage // intialize image URI
                        }
                    }
                }

                CAMERA_REQUEST -> {
                    hideImageWarningMessage()
                    placeholderImage.visibility = View.GONE

                    // Handle captured photo from the camera
                    val helper = MyHelperFunctions()
                    val photo = data?.extras?.get("data") as Bitmap

                    // get image size
                    val imageSize = helper.getBitmapSizeInMB(photo)

                    if (imageSize >= maxImageSizeinMB) {
                        Toast.makeText(requireContext(), "Image size cannot exceed 3MB.", Toast.LENGTH_SHORT).show()
                    } else {
                        newMomentImage.setImageBitmap(photo) // change image view
                        newMomentBitmap = photo // intialize image Bitmap
                    }
                }
            }
        }
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
                                imageWarningTextView.text = warningMessage
                                imageWarningCardView.visibility = View.VISIBLE
                                isPassing.complete(false)
                            } else {
                                imageWarningCardView.visibility = View.GONE
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
                            imageWarningTextView.text = warningMessage
                            imageWarningCardView.visibility = View.VISIBLE
                            isPassing.complete(false)
                        } else {
                            imageWarningCardView.visibility = View.GONE
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
                            var warningMessage = "User Guidelines Violation.\nInappropriate words:"
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

    private fun hideTextWarningMessage(){
        val warningMessage = "User Guidelines Violation.\nInappropriate words: "
        textWarningCardView.visibility = View.GONE
        textWarningTextView.text = warningMessage
    }

    private fun hideImageWarningMessage(){
        val warningMessage = "User Guidelines Violation.\nImage contains: "
        imageWarningCardView.visibility = View.GONE
        imageWarningTextView.text = warningMessage
    }

    private fun disableButton(button: Button) {
        button.isEnabled = false
        button.isClickable = false
        createNewMomentButtonProgressBar.visibility = View.VISIBLE
    }

    private fun enableButton(button: Button) {
        button.isEnabled = true
        button.isClickable = true
        createNewMomentButtonProgressBar.visibility = View.GONE
    }

    private fun disableAllInputs(){
        newMomentCaption.isEnabled = false
        imageCardView.isEnabled = false
    }

    private fun enableAllInputs(){
        newMomentCaption.isEnabled = true
        imageCardView.isEnabled = true
    }

    private suspend fun uploadImageToFirebase(imageUri: Uri?): String {
        val imageSrc = CompletableDeferred<String>()

        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val currentUser = FirebaseAuth.getInstance().currentUser

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
        }

        // Check if the total size exceeds 3MB (3 * 1024 * 1024 bytes)
        if (totalBytes > 3 * 1024 * 1024) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image size must not exceed 3MB", Toast.LENGTH_SHORT).show()
            }
            imageSrc.complete("")
        }

        if (imageUri != null) {
            val imageRef = storageRef.child("moments_images/${currentUser?.uid}/${UUID.randomUUID()}")

            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Image upload success
                    // You can get the download URL here
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        // Handle the URL as needed, e.g., save it to a database
                        imageSrc.complete(imageUrl)
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle unsuccessful upload
                    Toast.makeText(context, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                    imageSrc.complete("")
                }
        }

        return imageSrc.await()
    }

    private suspend fun uploadBitmapToFirebase(bitmap: Bitmap): String {
        val imageSrc = CompletableDeferred<String>()

        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val currentUser = FirebaseAuth.getInstance().currentUser

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Check if the total size exceeds 3MB (3 * 1024 * 1024 bytes)
        Log.e("IMAGE BITMAP SIZE", data.size.toString())
        if (data.size >= (3 * 1024 * 1024)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image size must not exceed 3MB", Toast.LENGTH_SHORT).show()
            }
            imageSrc.complete("")
        }

        val imageRef = storageRef.child("moments_images/${currentUser?.uid}/${UUID.randomUUID()}")

        val uploadTask = imageRef.putBytes(data)

        uploadTask.addOnSuccessListener{ taskSnapshot ->
            // Image upload success
            // You can get the download URL here
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                // Handle the URL as needed, e.g., save it to a database
                imageSrc.complete(imageUrl)
            }.addOnFailureListener { exception ->
                // Handle unsuccessful upload
                Toast.makeText(context, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                imageSrc.complete("")
            }
        }


        return imageSrc.await()
    }

    private fun createMoment(){
        hideTextWarningMessage()
        hideImageWarningMessage()
        disableAllInputs()
        disableButton(createNewMomentButton)

        val text = newMomentCaption.text.toString().trim() // get caption
        if (text == "" && !::newMomentImageUri.isInitialized && !::newMomentBitmap.isInitialized){ // check if there is no caption and no image
            Toast.makeText(requireContext(), "Write a caption or upload an image", Toast.LENGTH_SHORT).show()
            enableButton(createNewMomentButton)
            enableAllInputs()
            return
        }

        // has caption and/or image
        Toast.makeText(requireContext(), "Verifying post. Please wait.", Toast.LENGTH_SHORT).show()

        CoroutineScope(IO).launch{
            // scan caption for profanity if there is a caption
            if (text != "") {
                val isSafeEnglish = async {
                    checkProfanity(text, "en", apiUser, apiSecret)
                }.await()

                if (!isSafeEnglish){ // stop if has profanity in English or an error happened
                    withContext(Dispatchers.Main) {
                        enableButton(createNewMomentButton)
                        enableAllInputs()
                    }
                    return@launch
                }

                val isSafeFilipino = async {
                    checkProfanity(text, "tl", apiUser, apiSecret)
                }.await()

                if (!isSafeFilipino){ // stop if has profanity in Tagalog/Filipino or an error happened
                    withContext(Dispatchers.Main) {
                        enableButton(createNewMomentButton)
                        enableAllInputs()
                    }
                    return@launch
                }
            }

            // image url in the database
            var imageSrc = ""

            // scan image for profanity if there is an image
            if (::newMomentImageUri.isInitialized) {
                val isImageSafe = async {
                    analyzeImage(requireContext(), newMomentImageUri, workflow, apiUser, apiSecret)
                }.await()

                if (!isImageSafe){ // stop if has violation or an error happened
                    withContext(Dispatchers.Main) {
                        enableButton(createNewMomentButton)
                        enableAllInputs()
                    }
                    return@launch
                }

                imageSrc = async {
                    uploadImageToFirebase(newMomentImageUri)
                }.await()

                if (imageSrc == ""){ // stop if something failed in image upload
                    withContext(Dispatchers.Main) {
                        enableButton(createNewMomentButton)
                        enableAllInputs()
                    }
                    return@launch
                }
            } else if (::newMomentBitmap.isInitialized) {
                val isImageSafe = async {
                    analyzeBitmap(requireContext(), newMomentBitmap, workflow, apiUser, apiSecret)
                }.await()

                if (!isImageSafe){ // stop if has violation or an error happened
                    withContext(Dispatchers.Main) {
                        enableButton(createNewMomentButton)
                        enableAllInputs()
                    }
                    return@launch
                }

                imageSrc = async {
                    uploadBitmapToFirebase(newMomentBitmap)
                }.await()

                if (imageSrc == ""){ // stop if something failed in image upload
                    withContext(Dispatchers.Main) {
                        enableButton(createNewMomentButton)
                        enableAllInputs()
                    }
                    return@launch
                }
            }

            // continue to create Moment if text and/or image is safe and image has been uploaded
            val db = Firebase.firestore
            val currentUser = FirebaseAuth.getInstance().currentUser
            val firestore = FirebaseFirestore.getInstance()
            val usersCollection = firestore.collection("users")
            currentUser?.let { user ->
                val userId = user.uid
                usersCollection.document(userId).get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val documentSnapshot = task.result
                            // Check if the document exists and has the required fields
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                val username = documentSnapshot.getString("username") ?: ""
                                val userImageSrc = documentSnapshot.getString("userImage") ?: ""
                                val momentsData = documentSnapshot.get("posts")as? ArrayList<Map<Any, Any>> ?: ArrayList()

                                if (username == ""){
                                    Toast.makeText(requireContext(), "User has no username. Please add a username", Toast.LENGTH_SHORT).show()
                                    enableAllInputs()
                                    enableButton(createNewMomentButton)
                                    parentFragmentManager.popBackStack()
                                    return@addOnCompleteListener
                                }

                                var postsToday = 0
                                for (moment in momentsData){
                                    val date = moment["datePosted"] as Timestamp
                                    val today = Calendar.getInstance()
                                    if (isSameDate(date,today)){
                                        postsToday += 1
                                    }
                                }

                                if (postsToday >= 3){
                                    Toast.makeText(requireContext(), "Post Limit Exceeded. Please try again tomorrow.", Toast.LENGTH_SHORT).show()
                                    enableAllInputs()
                                    enableButton(createNewMomentButton)
                                    parentFragmentManager.popBackStack()
                                    return@addOnCompleteListener
                                }

                                val caption = text

                                val likes = 0
                                val comments = 0
                                val commentsList = arrayListOf<Any>()

                                // Add a new document with a generated id.
                                val data = hashMapOf(
                                    "username" to username,
                                    "userImageSrc" to userImageSrc,
                                    "userID" to userId,
                                    "caption" to caption,
                                    "imageSrc" to imageSrc,
                                    "likes" to likes,
                                    "comments" to comments,
                                    "commentsList" to commentsList,
                                    "datePosted" to FieldValue.serverTimestamp()

                                )

                                db.collection("moments")
                                    .add(data)
                                    .addOnSuccessListener { documentReference ->
                                        Toast.makeText(requireContext(), "New Moment Posted", Toast.LENGTH_SHORT).show()

                                        val momentData = hashMapOf(
                                            "momentID" to documentReference.id,
                                            "datePosted" to Timestamp.now(),
                                        )

                                        val userRef = db.collection("users").document(userId)
                                        userRef.update("posts", FieldValue.arrayUnion(momentData))

                                        enableAllInputs()
                                        enableButton(createNewMomentButton)
                                        parentFragmentManager.popBackStack()
                                        Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), "Network error. Please try again", Toast.LENGTH_SHORT).show()
                                        enableAllInputs()
                                        enableButton(createNewMomentButton)
                                        Log.w(TAG, "Error adding document", e)
                                    }

                            } else { // User doesn't have a document
                                enableAllInputs()
                                enableButton(createNewMomentButton)
                                Toast.makeText(requireContext(), "Database error. No user data", Toast.LENGTH_SHORT).show()
                            }
                        } else { // task was not successful
                            // Handle the error
                            enableAllInputs()
                            enableButton(createNewMomentButton)
                            Toast.makeText(requireContext(), "Network error. Please try again", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

        }
    }

    fun isSameDate(timestamp: Timestamp, calendar: Calendar): Boolean {
        val firebaseDate = Calendar.getInstance()
        firebaseDate.time = timestamp.toDate()

        return (calendar.get(Calendar.YEAR) == firebaseDate.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == firebaseDate.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == firebaseDate.get(Calendar.DAY_OF_MONTH))
    }

}