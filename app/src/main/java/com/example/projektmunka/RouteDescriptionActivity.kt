package com.example.projektmunka

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class RouteDescriptionActivity : AppCompatActivity(), RouteListFragment.OnItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_description)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RouteListFragment())

        }
    }

    override fun onItemSelected(selectedPosition: Int) {
        // Replace the ListFragment with the DetailFragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RouteDetailFragment.newInstance(selectedPosition))
            .addToBackStack(null)
            .commit()
    }
}