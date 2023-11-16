package com.example.aktibo

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class CustomMaterialDialogFragment : DialogFragment() {

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
