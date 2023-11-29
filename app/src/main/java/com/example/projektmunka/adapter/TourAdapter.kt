package com.example.projektmunka.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.projektmunka.databinding.ListItemBinding

class TourAdapter(private val context: Activity, private val names: Array<String>, private val descriptions: Array<String>, private val imageIds: IntArray) :
    ArrayAdapter<String>(context, 0, names) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ListItemBinding.inflate(LayoutInflater.from(context), parent, false)

        // Set data to the views using ViewBinding
        binding.tourPic.setImageResource(imageIds[position])
        binding.name.text = names[position]
        binding.description.text = descriptions[position]

        return binding.root
    }
}