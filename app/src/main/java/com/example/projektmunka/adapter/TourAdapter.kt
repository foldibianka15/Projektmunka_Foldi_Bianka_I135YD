package com.example.projektmunka.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.projektmunka.databinding.ListItemBinding
import com.example.projektmunka.uiData.TourItem

class TourAdapter(context: Context, private val tourList: ArrayList<TourItem>) :
    ArrayAdapter<TourItem>(context, 0, tourList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ListItemBinding.inflate(LayoutInflater.from(context), parent, false)

        val currentTourItem = tourList[position]

        // Set data to the views using ViewBinding
        binding.tourPic.setImageResource(currentTourItem.imageId)
        binding.tourName.text = currentTourItem.name
        binding.tourDescription.text = currentTourItem.description

        return binding.root
    }
}