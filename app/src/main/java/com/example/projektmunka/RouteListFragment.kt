package com.example.projektmunka

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.core.app.ActivityOptionsCompat
import com.example.projektmunka.adapter.TourAdapter
import com.example.projektmunka.databinding.FragmentRouteListBinding
import com.example.projektmunka.uiData.TourItem
import java.lang.ClassCastException

class RouteListFragment : Fragment() {
    interface OnItemSelectedListener {
        fun onItemSelected(position: Int, transitionBundle: Bundle?)

    }

    private var listener: OnItemSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnItemSelectedListener
        if (listener == null) {
            throw ClassCastException("$context must implement OnItemSelectedListener")
        }
        Log.d("RouteListFragment", "onAttach called. Listener: $listener")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentRouteListBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize your ListView and populate it with data
        val listView: ListView = binding.listview

        // Retrieve the current tourList
        val tourList = getTourList()

        val tourAdapter = TourAdapter(requireContext(), tourList)
        listView.adapter = tourAdapter

        // Set item click listener for the ListView
        listView.setOnItemClickListener { _, view, position, _ ->
            Log.d("RouteListFragment", "Item clicked at position: $position")
            val transitionName = getString(R.string.transition_tour_image)

            // Pass the transition name to the detail fragment
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                view.findViewById(R.id.tour_pic),
                transitionName
            )

            listener?.onItemSelected(position, options.toBundle())
        }
        return view
    }

    // New method to retrieve the current tourList
    fun getTourList(): ArrayList<TourItem> {
        val names = arrayOf("Explore the city", "Fitness Focus", "TimeTrail Fitness", "Burning calories")
        val descriptions = arrayOf("Explore scenic routes highlighting city landmarks",
            "Tailor routes to your fitness level for an effective workout.",
            "Set start and end points, walk for a specified time on varied terrain.",
            "Reach your destination while burning calories, tailored to your profile.")
        val imageIds = intArrayOf(
            R.drawable.nature1, R.drawable.nature3, R.drawable.nature2, R.drawable.nature3
        )

        val tourList = ArrayList<TourItem>()

        for (i in names.indices) {
            val tourItem = TourItem(names[i], descriptions[i], imageIds[i])
            tourList.add(tourItem)
        }

        return tourList
    }
}