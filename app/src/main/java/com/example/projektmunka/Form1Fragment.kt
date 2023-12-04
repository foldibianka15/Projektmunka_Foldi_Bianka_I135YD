package com.example.projektmunka

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.projektmunka.R
import com.example.projektmunka.databinding.FragmentForm1Binding

class Form1Fragment : Fragment() {

    private lateinit var binding: FragmentForm1Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForm1Binding.inflate(inflater, container, false)
        return binding.root
    }
}