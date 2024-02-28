package com.example.projektmunka.fragment

import GeneratePaths
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.example.projektmunka.RouteUtils.ShenandoahsHikingDifficulty
import com.example.projektmunka.RouteUtils.addMarker
import com.example.projektmunka.RouteUtils.drawRoute
import com.example.projektmunka.RouteUtils.findNearestNode
import com.example.projektmunka.RouteUtils.getGraph
import com.example.projektmunka.RouteUtils.performReverseGeocoding
import com.example.projektmunka.RouteUtils.performReverseGeocodingBlocking
import com.example.projektmunka.data.Route
import com.example.projektmunka.data.User
import com.example.projektmunka.databinding.FragmentForm2Binding
import com.example.projektmunka.utils.NominatimReverseGeocoding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.osmdroid.views.MapView
import java.util.concurrent.CountDownLatch

class Form2Fragment() : Fragment() {

    private lateinit var binding: FragmentForm2Binding
    lateinit var editTextAddress: TextInputEditText
    lateinit var radioGroupLocation: RadioGroup
    lateinit var editTextTargetLocation: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForm2Binding.inflate(inflater, container, false)
        val rootView = binding.root

        editTextAddress = binding.editTextAddress
        radioGroupLocation = binding.radioGroupLocation
        editTextTargetLocation = binding.editTextTargetLocation
        val btnSetActualAddress = binding.btnSetActualAddress
        val btnCreateRoute = binding.btnCreateRoute

        btnSetActualAddress.setOnClickListener {
            // Handle the "Set my actual address" button click here
            // You might want to implement a separate logic for this button
            //setActualLocationMethod()
        }

        btnCreateRoute.setOnClickListener {

        }
        return rootView
    }



}
