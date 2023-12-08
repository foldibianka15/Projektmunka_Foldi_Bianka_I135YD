package com.example.projektmunka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.example.projektmunka.databinding.ActivityLoginBinding
import com.example.projektmunka.databinding.ActivityRouteDescriptionBinding
import com.example.projektmunka.fragment.RouteDetailFragment
import com.example.projektmunka.fragment.RouteListFragment
import com.example.projektmunka.viewModel.LoginViewModel
import com.example.projektmunka.viewModel.UserDataViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteDescriptionActivity : AppCompatActivity(), RouteListFragment.OnRouteListItemSelectedListener,
                                                    RouteDetailFragment.OnStartButtonClickListener{

    private lateinit var binding: ActivityRouteDescriptionBinding
    private val userDataViewModel: UserDataViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_description)

        binding = ActivityRouteDescriptionBinding.inflate(layoutInflater)
        binding.viewModel = userDataViewModel

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RouteListFragment())
                .commit()
        }
    }

    override fun onStartButtonClicked(data: Int) {
        // Handle the "Start" button click
        // Start the MapActivity or perform any other desired action
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("key", data)
        startActivity(intent)
    }

    override fun onRouteListItemSelected(selectedPosition: Int, transitionBundle: Bundle?) {
        Log.d("RouteDescriptionActivity", "Item selected at position: $selectedPosition")
        val routeListFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? RouteListFragment
        val tourList = routeListFragment?.getTourList() ?: ArrayList()

        // Replace the ListFragment with the DetailFragment
        val detailFragment = RouteDetailFragment.newInstance(selectedPosition, tourList)
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                detailFragment)
            .addToBackStack(null)  // Add to back stack
            .commit()

        Log.d("RouteDescriptionActivity", "Replacing fragment with RouteDetailFragment")
    }
}