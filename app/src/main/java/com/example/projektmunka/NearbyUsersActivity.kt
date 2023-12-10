package com.example.projektmunka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ListView
import androidx.activity.viewModels
import com.example.projektmunka.RouteUtils.calculateGeodesicDistanceInMeters
import com.example.projektmunka.adapter.NearbyUsersAdapter
import com.example.projektmunka.data.User
import com.example.projektmunka.databinding.ActivityMapBinding
import com.example.projektmunka.databinding.ActivityNearbyUsersBinding
import com.example.projektmunka.uiData.NearbyUserItem
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
        setContentView(binding.root)

        listViewNearbyUsers = binding.listViewNearbyUsers
        nearbyUsersAdapter = NearbyUsersAdapter(this, R.layout.nearby_user_list_item, mutableListOf())
        listViewNearbyUsers.adapter = nearbyUsersAdapter

        updateNearbyUsers(500.0)

        listViewNearbyUsers.setOnItemClickListener(AdapterView.OnItemClickListener { _, _, position, _ ->
            // Handle item click, get the user at the clicked position
            val clickedUser = nearbyUsersAdapter.getItem(position)

            // Start QRCodeActivity and pass necessary data
            val intent = Intent(this, QRCodeActivity::class.java)
            intent.putExtra("userId", clickedUser?.userId)
            startActivity(intent)
        })
    }

    private fun getNearbyUsers(friendZone: Double): MutableList<User> {
        val currentUser = userDataViewModel.currentUserData.value ?: return mutableListOf()
        val currentUserLocation = userDataViewModel.getUserLocation(currentUser)
        val allUsers = userDataViewModel.getAllUsers()
        allUsers.remove(currentUser)
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

    private fun updateNearbyUsers(friendZone: Double) {
        val nearbyUsers = getNearbyUsers(friendZone)
        val nearbyUserItems = nearbyUsers.map {
            NearbyUserItem(
                userId = it.id,
                firstName = it.firstName,
                age = it.age.toString(),
                gender = it.gender,
                distance = calculateGeodesicDistanceInMeters(
                userDataViewModel.currentUserData.value?.let { it1 ->
                    userDataViewModel.getUserLocation(
                        it1
                    )?.geoPoint
                }!!,
                userDataViewModel.getUserLocation(it)?.geoPoint!!
            )
            )
        }
        nearbyUsersAdapter.clear()
        nearbyUsersAdapter.addAll(nearbyUserItems)
    }
}