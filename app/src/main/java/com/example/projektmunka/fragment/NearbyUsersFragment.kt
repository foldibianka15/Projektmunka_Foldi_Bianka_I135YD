package com.example.projektmunka.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.projektmunka.R
import com.example.projektmunka.adapter.NearbyUsersAdapter
import com.example.projektmunka.data.User
import com.example.projektmunka.viewModel.UserDataViewModel


class NearbyUsersFragment : Fragment() {

    private val listViewNearbyUsers: ListView? = null
    private lateinit var nearbyUsersAdapter: NearbyUsersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_nearby_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nearbyUsersAdapter = NearbyUsersAdapter(requireContext(), R.layout.nearby_user_list_item, mutableListOf())
        listViewNearbyUsers?.adapter = nearbyUsersAdapter

        observeUserData()
    }

    private fun observeUserData() {

            val nearbyUsersList: List<User> = mutableListOf() // Replace with actual logic

            // Update the adapter with the nearby users
            nearbyUsersAdapter.clear()
            nearbyUsersAdapter.addAll(nearbyUsersList)
            nearbyUsersAdapter.notifyDataSetChanged()
        }

}