package com.example.projektmunka

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.projektmunka.RouteUtils.calculateGeodesicDistanceInMeters
import com.example.projektmunka.adapter.NearbyUsersAdapter
import com.example.projektmunka.data.User
import com.example.projektmunka.databinding.ActivityMapBinding
import com.example.projektmunka.databinding.ActivityNearbyUsersBinding
import com.example.projektmunka.viewModel.NearbyUsersViewModel
import com.example.projektmunka.viewModel.UserDataViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NearbyUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNearbyUsersBinding
    private lateinit var nearbyUsersAdapter: NearbyUsersAdapter
    private lateinit var listViewNearbyUsers: ListView

    private val nearbyUsersViewModel : NearbyUsersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_users)
        binding = ActivityNearbyUsersBinding.inflate(layoutInflater)
        binding.viewModel = nearbyUsersViewModel

        listViewNearbyUsers = binding.listViewNearbyUsers
        nearbyUsersAdapter = NearbyUsersAdapter(this, R.layout.nearby_user_list_item, mutableListOf())
        listViewNearbyUsers.adapter = nearbyUsersAdapter

        observeUserData()
    }


    private fun observeUserData() {

        nearbyUsersViewModel.getNearbyUsers(1000.0)
        lifecycleScope.launchWhenCreated {
            nearbyUsersViewModel.nearbyUsers.collect{ users ->
                nearbyUsersAdapter.clear()
                nearbyUsersAdapter.addAll(users)
                nearbyUsersAdapter.notifyDataSetChanged()
            }
        }
    }
}