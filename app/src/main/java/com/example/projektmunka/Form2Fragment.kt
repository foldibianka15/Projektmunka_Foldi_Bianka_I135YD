package com.example.projektmunka

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.projektmunka.databinding.FragmentForm2Binding

class Form2Fragment : Fragment() {

    private lateinit var binding: FragmentForm2Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForm2Binding.inflate(inflater, container, false)
        return binding.root
    }
}
