package com.example.projektmunka

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.projektmunka.databinding.FragmentForm4Binding

class Form4Fragment : Fragment() {

    private lateinit var binding: FragmentForm4Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForm4Binding.inflate(inflater, container, false)
        return binding.root
    }
}