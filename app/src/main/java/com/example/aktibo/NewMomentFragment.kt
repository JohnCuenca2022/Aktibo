package com.example.aktibo

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.textfield.TextInputEditText

class NewMomentFragment : Fragment() {

    private lateinit var editText: TextInputEditText
    private lateinit var counterTextView: TextView

    private val PICK_IMAGE_REQUEST = 1
    private lateinit var viewPager: ViewPager
    private lateinit var selectImageButton: Button
    private lateinit var imagePagerAdapter: ImagePagerAdapter
    private val selectedImageUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_new_moment, container, false)

        editText = view.findViewById(R.id.yourEditText)

        viewPager = view.findViewById(R.id.viewPager)
        selectImageButton = view.findViewById(R.id.selectImageButton)

        imagePagerAdapter = ImagePagerAdapter(requireContext(), selectedImageUris)
        viewPager.adapter = imagePagerAdapter

        selectImageButton.setOnClickListener {
            // Open the image picker when the button is clicked
            openImagePicker()
        }

        return view
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data

            if (selectedImageUris.size < 3) {
                selectedImageUris.add(selectedImageUri!!)
                imagePagerAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

}

class ImagePagerAdapter(private val context: Context, private val imageUris: MutableList<Uri>) : PagerAdapter() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return imageUris.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val itemView = layoutInflater.inflate(R.layout.image_pager_item, container, false)
        val imageView = itemView.findViewById<ImageView>(R.id.imageView)
        val closeButton = itemView.findViewById<ImageButton>(R.id.closeButton)

        imageView.setImageURI(imageUris[position])

        // Handle the click event for the close button
        closeButton.setOnClickListener {
            if (position >= 0 && position < imageUris.size) {
                imageUris.removeAt(position)
                notifyDataSetChanged()
            }
        }

        container.addView(itemView)

        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}