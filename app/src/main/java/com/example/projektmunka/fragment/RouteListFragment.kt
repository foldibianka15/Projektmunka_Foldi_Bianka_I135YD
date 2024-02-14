package com.example.projektmunka.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import com.example.projektmunka.R
import com.example.projektmunka.adapter.TourAdapter
import com.example.projektmunka.databinding.FragmentRouteListBinding
import com.example.projektmunka.uiData.TourItem

class RouteListFragment : Fragment() {

    interface OnMapItemSelectedListener {
        fun onMapItemSelected(selectedPosition: Int)
    }

    interface OnRouteListItemSelectedListener {
        fun onRouteListItemSelected(selectedPosition: Int, transitionBundle: Bundle?)
    }

    /* interface OnItemSelectedListener {
         fun onItemSelected(position: Int, transitionBundle: Bundle?)
     }*/

    private var routeListener: OnRouteListItemSelectedListener? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        routeListener = context as? OnRouteListItemSelectedListener
        if (routeListener == null) {
            throw ClassCastException("$context must implement OnItemSelectedListener")
        }
        Log.d("RouteListFragment", "onAttach called. Listener: $routeListener")
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

            routeListener?.onRouteListItemSelected(position, options.toBundle())
        }
        return view
    }

    // New method to retrieve the current tourList
    fun getTourList(): ArrayList<TourItem> {
        val names =
            arrayOf("Explore the city", "Fitness Focus", "TimeTrail Fitness", "Burning calories")
        val descriptions = arrayOf(
            "Explore scenic routes highlighting city landmarks",
            "Tailor routes to your fitness level for an effective workout.",
            "Set start and end points, walk for a specified time on varied terrain.",
            "Reach your destination while burning calories, tailored to your profile."
        )
        val detailedDescription = arrayOf(
            "Explore picturesque routes and enjoy the beauty of the city with this scenic route option. " +
                    "The algorithm considers attractive locations and landmarks along the way.",
            "Tailor your route based on fitness goals. " +
                    "The algorithm considers your fitness level and adjusts the route for optimal physical activity, ensuring a challenging and " +
                    "rewarding experience.",
            "Set your start and end points along with the desired walking time. The algorithm then factors in " +
                    "your fitness level to suggest routes with varying terrain difficulty, ensuring a timed fitness trek.",
            "Specify your route" +
                    " from point A to point B and set a calorie-burning goal. The algorithm takes into account your weight" +
                    " to calculate an optimal route that helps you achieve your calorie-burning target."
        )
        val imageIds = intArrayOf(
            R.drawable.nature1, R.drawable.nature3, R.drawable.nature2, R.drawable.nature4
        )

        val tourList = ArrayList<TourItem>()

        for (i in names.indices) {
            val tourItem = TourItem(names[i], descriptions[i], detailedDescription[i], imageIds[i])
            tourList.add(tourItem)
        }

        return tourList
    }
}