package com.example.projektmunka.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.projektmunka.R
import com.example.projektmunka.data.User

import com.example.projektmunka.databinding.NearbyUserListItemBinding

class NearbyUsersAdapter(
    context: Context,
    resource: Int,
    users: List<User>
) : ArrayAdapter<User>(context, resource, users) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = NearbyUserListItemBinding.inflate(LayoutInflater.from(context), parent, false)
        val user = getItem(position)

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.nearby_user_list_item, parent, false)

        // Update the UI elements with user data
        binding.textViewUsername.text = user?.firstName
        binding.textViewAge.text = user?.age
        binding.textViewGender.text = user?.gender

        return view
    }
}