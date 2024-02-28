package com.example.projektmunka.fragment

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.example.projektmunka.RouteOptimizers.CircularDifficultRouteOptimizer
import com.example.projektmunka.RouteUtils.calculateROpt
import com.example.projektmunka.RouteUtils.calculateSearchArea
import com.example.projektmunka.RouteUtils.displayCircularRoute
import com.example.projektmunka.RouteUtils.evaluateNodes
import com.example.projektmunka.RouteUtils.fetchCityGraph
import com.example.projektmunka.RouteUtils.fetchNodes
import com.example.projektmunka.RouteUtils.findClosestNonIsolatedNode
import com.example.projektmunka.RouteUtils.findNearestOSMNode
import com.example.projektmunka.RouteUtils.getElevationData
import com.example.projektmunka.RouteUtils.selectImportantPOIs
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.User
import com.example.projektmunka.databinding.FragmentForm2Binding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.osmdroid.views.MapView

class Form1Fragment() : Fragment() {

    private lateinit var binding: FragmentForm2Binding
    lateinit var editTextAddress: TextInputEditText
    lateinit var radioGroupLocation: RadioGroup
    lateinit var editTextTargetLocation: TextInputEditText


    private var poiToClosestNonIsolatedNode: MutableMap<Node, Node> = mutableMapOf()
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