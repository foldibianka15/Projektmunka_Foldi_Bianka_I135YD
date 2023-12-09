package com.example.projektmunka

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.projektmunka.databinding.ActivityBaseBinding
import com.example.projektmunka.databinding.ActivityMapBinding
import com.google.android.material.navigation.NavigationView

abstract class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var binding: ActivityBaseBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(drawerLayout) // Abstract method to get layout resource ID

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here
        when (item.itemId) {
            // Handle menu items
            R.id.nav_reports -> {
                // Handle the click on menu item 1
                // startActivity(Intent(this, AnotherActivity::class.java))
            }

            R.id.nav_routes -> {
                // Handle the click on menu item 2
            }

            R.id.nav_friends -> {
                // Handle the click on menu item 3
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    abstract fun getLayoutResourceId(): Int // Abstract method to get layout resource ID
}
