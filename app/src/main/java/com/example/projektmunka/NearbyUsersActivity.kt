package com.example.projektmunka

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import androidx.activity.viewModels
import com.example.projektmunka.RouteUtils.calculateGeodesicDistanceInMeters
import com.example.projektmunka.adapter.NearbyUsersAdapter
import com.example.projektmunka.data.User
import com.example.projektmunka.databinding.ActivityMapBinding
import com.example.projektmunka.databinding.ActivityNearbyUsersBinding
import com.example.projektmunka.viewModel.UserDataViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NearbyUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNearbyUsersBinding
    private lateinit var nearbyUsersAdapter: NearbyUsersAdapter
    private lateinit var listViewNearbyUsers: ListView

    private val userDataViewModel: UserDataViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_users)
        binding = ActivityNearbyUsersBinding.inflate(layoutInflater)
        binding.viewModel = userDataViewModel

        listViewNearbyUsers = binding.listViewNearbyUsers
        nearbyUsersAdapter = NearbyUsersAdapter(this, R.layout.nearby_user_list_item, mutableListOf())
        listViewNearbyUsers.adapter = nearbyUsersAdapter

        observeUserData()
    }

    private fun getNearbyUsers(friendZone: Double): MutableList<User> {
        val currentUser = userDataViewModel.currentUserData.value ?: return mutableListOf()
        val currentUserLocation = userDataViewModel.getUserLocation(currentUser)
        val allUsers = userDataViewModel.getAllUsers()
        val nearbyUsers = mutableListOf<User>()

        for (user in allUsers) {
            val userLocation = userDataViewModel.getUserLocation(user)
            val distanceBetweenUsers =
                calculateGeodesicDistanceInMeters(currentUserLocation?.geoPoint!!, userLocation?.geoPoint!!)
            if (distanceBetweenUsers <= friendZone && !currentUser.friends.contains(user)) {
                nearbyUsers.add(user)
            }
        }

        return nearbyUsers
    }

    private fun observeUserData() {
        val nearbyUsersList = getNearbyUsers(1000.0)

        nearbyUsersAdapter.clear()
        nearbyUsersAdapter.addAll(nearbyUsersList)
        nearbyUsersAdapter.notifyDataSetChanged()
    }
}