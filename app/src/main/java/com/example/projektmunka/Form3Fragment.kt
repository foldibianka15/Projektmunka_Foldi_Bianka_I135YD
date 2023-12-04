package com.example.projektmunka

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.projektmunka.databinding.FragmentForm3Binding

class Form3Fragment : Fragment() {

    private lateinit var binding: FragmentForm3Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForm3Binding.inflate(inflater, container, false)
        return binding.root
    }
}
