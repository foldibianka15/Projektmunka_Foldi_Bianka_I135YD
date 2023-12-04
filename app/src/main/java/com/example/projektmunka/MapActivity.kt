package com.example.projektmunka

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.projektmunka.databinding.ActivityMapBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapActivity : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var binding: ActivityMapBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_map)

        // Set up the bottom sheet
        /*val bottomSheet: FrameLayout = findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN*/

        BottomSheetBehavior.from(findViewById(R.id.bottom_sheet)).apply {
            peekHeight=100
            this.state=BottomSheetBehavior.STATE_COLLAPSED
        }

        val data: Int = intent.getIntExtra("key", 0)
        onMapItemSelected(data)

    }

    // Method to show the bottom sheet
    fun showBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // Method to hide the bottom sheet
    fun hideBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }


    private fun inflateFormLayout(formType: Int): View {
        val inflater = LayoutInflater.from(this)
        return when (formType) {
            1 -> inflater.inflate(R.layout.fragment_form1, null)
            2 -> inflater.inflate(R.layout.fragment_form2, null)
            3 -> inflater.inflate(R.layout.fragment_form3, null)
            4 -> inflater.inflate(R.layout.fragment_form4, null)
            else -> inflater.inflate(R.layout.fragment_form1, null) // Default to the first form
        }
    }

    private fun replaceBottomSheetContent(contentView: View) {
        val bottomSheet = findViewById<FrameLayout>(R.id.bottom_sheet)
        bottomSheet.removeAllViews()
        bottomSheet.addView(contentView)

        // Expand the bottom sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

      fun onMapItemSelected(selectedPosition: Int) {
        Log.d("MapActivity", "Item selected at position: $selectedPosition")

        // Determine which form type is selected
        val selectedFormType = when (selectedPosition) {
            0 -> 1
            1 -> 2
            2 -> 3
            3 -> 4
            else -> 1 // Default to the first form
        }

        // Inflate the layout for the selected form
        val formLayout = inflateFormLayout(selectedFormType)

        // Replace the content of the bottom sheet with the selected form layout
        replaceBottomSheetContent(formLayout)

        showBottomSheet()
    }
}