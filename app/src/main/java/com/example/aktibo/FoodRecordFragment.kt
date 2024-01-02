package com.example.aktibo

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FoodRecordFragment : Fragment() {

    private lateinit var button_newFoodRecord: Button

    //Tensorflow
    private lateinit var tflite: Interpreter

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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

}