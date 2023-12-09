package com.example.projektmunka.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.example.projektmunka.R
import com.example.projektmunka.databinding.FragmentRouteDetailBinding
import com.example.projektmunka.uiData.TourItem

class RouteDetailFragment : Fragment() {
    interface OnStartButtonClickListener {
        fun onStartButtonClicked(data: Int)
    }

    private var listener: OnStartButtonClickListener? = null
    private lateinit var binding: FragmentRouteDetailBinding
    private var mapListener: RouteListFragment.OnMapItemSelectedListener? = null

    companion object {
        private const val ARG_POSITION = "position"
        private const val ARG_TOUR_LIST = "tourList"

        fun newInstance(position: Int, tourList: ArrayList<TourItem>): RouteDetailFragment {
            val fragment = RouteDetailFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            args.putParcelableArrayList(ARG_TOUR_LIST, tourList)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnStartButtonClickListener) {
            listener = context
        } else {
            throw ClassCastException("$context must implement OnStartButtonClickListener")
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
        Log.d("RouteDetailFragment", "onViewCreated called")

        println("alma")

        val position = arguments?.getInt(ARG_POSITION) ?: 0
        val tourList = arguments?.getParcelableArrayList<TourItem>(ARG_TOUR_LIST) ?: arrayListOf()

        Log.d("RouteDetailFragment", "Position: $position, TourList size: ${tourList.size}")

        if (position < tourList.size) {
            val currentTourItem = tourList[position]

            // Set data to the views using ViewBinding
            binding.tourImageDetail.setImageResource(currentTourItem.imageId)
            binding.tourNameDetail.text = currentTourItem.name
            binding.tourDescriptionDetail.text = currentTourItem.detailedDescription

            ViewCompat.setTransitionName(binding.tourImageDetail, getString(R.string.transition_tour_image))
        }

        binding.startButton.setOnClickListener {
            // Handle the "Start" button click
            // Notify the activity through the listener
            if (listener != null) {
                //sendDataToActivity(position)
                mapListener?.onMapItemSelected(position)
                listener?.onStartButtonClicked(position)
            } else {
                Log.e("RouteDetailFragment", "Listener is null!")
            }
        }
    }
}