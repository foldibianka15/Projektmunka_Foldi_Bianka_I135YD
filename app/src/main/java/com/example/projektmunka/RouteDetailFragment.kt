package com.example.projektmunka

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.projektmunka.databinding.FragmentRouteDetailBinding

class RouteDetailFragment : Fragment() {
    private lateinit var binding: FragmentRouteDetailBinding

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): RouteDetailFragment {
            val fragment = RouteDetailFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRouteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val position = arguments?.getInt(ARG_POSITION) ?: 0
        val currentTourItem = (requireActivity() as RouteListFragment).tourList[position]

        // Set data to the views using ViewBinding
        binding.tourImageDetail.setImageResource(currentTourItem.imageId)
        binding.tourNameDetail.text = currentTourItem.name
        binding.tourDescriptionDetail.text = currentTourItem.description
    }
}