package com.example.projektmunka

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.example.projektmunka.adapter.TourAdapter
import com.example.projektmunka.databinding.FragmentRouteBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RouteFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RouteFragment : Fragment() {

    private lateinit var binding: FragmentRouteBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentRouteBinding.inflate(inflater, container, false)
        val view = binding.root
        // Initialize your ListView and populate it with data
        val listView: ListView = binding.listview

        val name = arrayOf("Simple route", "Circle route", "Sightseeing route")
        val description = arrayOf("Great experience", "", "Explore the city")
        val imageId = intArrayOf(
            R.drawable.lake, R.drawable.lake, R.drawable.wolf
        )

        val tourAdapter = TourAdapter(requireActivity(), name, description, imageId)
        listView.adapter = tourAdapter

        // Set item click listener for the ListView
        listView.setOnItemClickListener { parent, view, position, id ->
            // Handle item click here
            val selectedTourName = name[position]

            // For example, you can navigate to a detailed view of the selected tour
            // using the Navigation component or startActivity
            // You can pass the data (selectedTourName, description[position], imageId[position]) to the detailed view if needed.

            // For now, let's show a toast with the selected tour's name
            Toast.makeText(
                requireContext(),
                "Selected Tour: $selectedTourName",
                Toast.LENGTH_SHORT
            ).show()
        }

        return view
    }

}