package com.example.projektmunka.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.projektmunka.R
import com.example.projektmunka.data.User

import com.example.projektmunka.databinding.NearbyUserListItemBinding
import com.example.projektmunka.uiData.NearbyUserItem

class NearbyUsersAdapter(
    context: Context,
    resource: Int,
    users: List<NearbyUserItem> // Change the type to List<NearbyUserItem>
) : ArrayAdapter<NearbyUserItem>(context, resource, users) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: NearbyUserListItemBinding
        val view: View

        if (convertView == null) {
            binding = NearbyUserListItemBinding.inflate(LayoutInflater.from(context), parent, false)
            view = binding.root
            view.tag = binding
        } else {
            binding = convertView.tag as NearbyUserListItemBinding
            view = convertView
        }

        val user = getItem(position)

        // Update the UI elements with user data
        binding.textViewFirstName.text = user?.firstName
        binding.textViewAge.text = user?.age
        binding.textViewGender.text = user?.gender
        binding.textViewDistance.text = "Distance: ${user?.distance} meters" // Set distance value

        return view
    }

    override fun addAll(collection: Collection<out NearbyUserItem>) {
        super.addAll(collection)
    }
}