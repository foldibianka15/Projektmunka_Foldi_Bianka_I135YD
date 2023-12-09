package com.example.projektmunka

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.projektmunka.databinding.ActivityBaseBinding
import com.example.projektmunka.viewModel.UserDataViewModel
import com.google.android.material.navigation.NavigationView

abstract class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private val qrCodeViewModel: UserDataViewModel by viewModels()

    override fun setContentView(view: View?) {
        drawerLayout = layoutInflater.inflate(R.layout.activity_base, null).findViewById(R.id.drawer_layout)
        val container = drawerLayout.findViewById<FrameLayout>(R.id.activityContainer)
        container.addView(view)
        super.setContentView(drawerLayout) // Abstract method to get layout resource ID

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here
        when (item.itemId) {
            // Handle menu items
            R.id.nav_routes -> {
                val intent = Intent(this@BaseActivity, RouteDescriptionActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_reports -> {
                // Handle the click on menu item 1
                // startActivity(Intent(this, AnotherActivity::class.java))
            }
            R.id.nav_profile -> {
                val intent = Intent(this@BaseActivity, ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_friends -> {

            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    abstract fun getLayoutResourceId(): Int // Abstract method to get layout resource ID
}
