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
        Log.d("RouteDescriptionActivity", "Item selected at position: $selectedPosition")
        val routeListFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? RouteListFragment
        val tourList = routeListFragment?.getTourList() ?: ArrayList()

        // Replace the ListFragment with the DetailFragment
        val detailFragment = RouteDetailFragment.newInstance(selectedPosition, tourList)
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                detailFragment
            )
            .commitNow()

        Log.d("RouteDescriptionActivity", "Replacing fragment with RouteDetailFragment")
    }

}