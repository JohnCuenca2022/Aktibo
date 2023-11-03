package com.example.aktibo

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.InputStream
import kotlin.coroutines.resumeWithException

class NewMomentFragment : Fragment() {

    private lateinit var newMomentCaption: TextInputEditText

    private lateinit var imageCardView: CardView
    private lateinit var newMomentImage: ImageView
    private lateinit var newMomentImageUri: Uri

    private lateinit var imageWarningCardView: CardView
    private lateinit var imageWarningTextView: TextView

    private lateinit var createNewMomentButton: Button

    var isValid = false

    val workflow = "wfl_f5yBhhGuUn9BpIyZwaJMZ"
    val apiUser = "1227574749"
    val apiSecret = "NLvWeA9iUfYBqg6rMyx6VsaJXy"

    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 456

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_new_moment, container, false)

        newMomentCaption = view.findViewById(R.id.newMomentCaption)

        imageCardView = view.findViewById(R.id.imageCardView)
        newMomentImage = view.findViewById(R.id.newMomentImage)

        imageCardView.setOnClickListener{
            selectImage(view)
        }

        imageWarningCardView = view.findViewById(R.id.imageWarningCardView)
        imageWarningTextView = view.findViewById(R.id.imageWarningTextView)

        createNewMomentButton = view.findViewById(R.id.createNewMomentButton)
        createNewMomentButton.setOnClickListener{
            createMoment()
        }

        return view
    }

    val PICK_IMAGE_REQUEST = 1
    fun selectImage(view: View) {
        checkAndRequestExternalStoragePermission()
    }

    private fun checkAndRequestExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
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

            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with capturing image
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(intent, PICK_IMAGE_REQUEST)
                } else {
                    // Permission denied, handle it gracefully (e.g., show a message to the user)
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
                    val selectedImage = data?.data
                    newMomentImage.setImageURI(selectedImage)
                    if (selectedImage != null) {
                        newMomentImageUri = selectedImage
                    }

                    Toast.makeText(context, selectedImage.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun analyzeImage(context: Context, imageUri: Uri, workflow: String, apiUser: String, apiSecret: String) {
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
                                var warningMessage = "User Guideline Violation.\nImage contains: "
                                summaryReason.forEach { item ->
                                    val reasonText = "${item.text}, "
                                    warningMessage += reasonText.removeSuffix(", ")
                                }
                                imageWarningTextView.text = warningMessage
                                imageWarningCardView.visibility = View.VISIBLE
                            } else {
                                imageWarningCardView.visibility = View.INVISIBLE
                            }

                        } else {
                            Log.e("SightEngine API", "Empty response body")
                            Toast.makeText(requireContext(), "Failed to scan image. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Handle the error
                        Log.e("SightEngine API", "Error: ${response.code()}")
                        Toast.makeText(requireContext(), "Failed to scan image. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SightEngineResponse>, t: Throwable) {
                    // Handle network errors here
                    Log.e("SightEngine API", "Network error: ${t.message}")
                    Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Log.e("SightEngine API", "Failed to open InputStream for image URI")
            Toast.makeText(requireContext(), "Failed to scan image. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkProfanity(text: String, lang: String, apiKey: String, apiSecret: String) {

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
                            isValid = true
                        } else {
                            profanityMatches.forEach { item ->
                                Log.e("SightEngine API Text Profanity", item.match)
                            }
                        }

                    }
                } else {
                    Log.e("SightEngine API Text", "Empty response body")
                    Toast.makeText(requireContext(), "Failed to scan text. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SightEngineResponseText>, t: Throwable) {
                Log.e("SightEngine API Text", "Network error: ${t.message}")
                Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun createMoment(){
        val text = newMomentCaption.text.toString().trim() // get caption
        if (text == "" && !::newMomentImageUri.isInitialized){ // check if there is no caption and no image
            Toast.makeText(requireContext(), "Write a caption or upload an image", Toast.LENGTH_SHORT).show()
            return
        }

        // has caption and/or image
        Toast.makeText(requireContext(), "Verifying post. Please wait.", Toast.LENGTH_SHORT).show()

        checkProfanity(text, "e", apiUser, apiSecret)

        println(isValid)
        // checkProfanity(text, "tl", apiUser, apiSecret)
        // analyzeImage(requireContext(),newMomentImageUri,workflow,apiUser,apiSecret)

    }

}