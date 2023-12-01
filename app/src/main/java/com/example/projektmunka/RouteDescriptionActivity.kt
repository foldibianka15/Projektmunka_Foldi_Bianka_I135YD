package com.example.projektmunka

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class RouteDescriptionActivity : AppCompatActivity(), RouteListFragment.OnItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_description)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RouteListFragment())
                .commit()
        }
    }

    override fun onItemSelected(selectedPosition: Int, transitionBundle: Bundle?) {
        // Retrieve the current tourList from the RouteListFragment
        println("alma")
        Log.d("RouteDescriptionActivity", "Item selected at position: $selectedPosition")
        val routeListFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? RouteListFragment
        val tourList = routeListFragment?.getTourList() ?: ArrayList()

        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            Log.d("RouteDescriptionActivity", "Replacing fragment with RouteDetailFragment")
            // Replace the ListFragment with the DetailFragment
            val detailFragment = RouteDetailFragment.newInstance(selectedPosition, tourList)
            detailFragment.arguments = transitionBundle
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    detailFragment
                )
                .addToBackStack(null)
                .commit()
        }
    }
}