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

class CustomMaterialDialogMealRecipeFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.custom_material_dialog_layout_meal_recipe, null)

        val closeButton = view.findViewById<View>(R.id.closeButton)
        val editText = view.findViewById<TextInputEditText>(R.id.editText)
        val button_random = view.findViewById<View>(R.id.button_random)
        val button_confirm = view.findViewById<View>(R.id.button_confirm)

        closeButton.setOnClickListener {
            dismiss()
        }

        button_random.setOnClickListener {
            replaceFragmentWithAnimAndData("surprise me")
            dismiss()
        }

        button_confirm.setOnClickListener {
            val foodDescription = editText.text.toString().trim()
            if (foodDescription == ""){
                Toast.makeText(context, "Input at least one ingredient", Toast.LENGTH_SHORT).show();
            } else if (foodDescription.length < 2){
                Toast.makeText(context, "ingredients list must have at least 2 characters", Toast.LENGTH_SHORT).show();
            } else {
                replaceFragmentWithAnimAndData(foodDescription)
                dismiss()
            }
        }

        builder.setView(view)

        return builder.create()
    }

    private fun replaceFragmentWithAnimAndData(ingredients: String) {
        val fragment = MealRecipeFragment()
        val args = Bundle()
        args.putString("ingredients", ingredients)
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
