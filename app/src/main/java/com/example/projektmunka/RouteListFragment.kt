package com.example.projektmunka

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.example.projektmunka.adapter.TourAdapter
import com.example.projektmunka.databinding.FragmentRouteBinding
import com.example.projektmunka.databinding.FragmentRouteListBinding
import com.example.projektmunka.uiData.TourItem
import java.lang.ClassCastException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class RouteListFragment : Fragment() {
    interface OnItemSelectedListener {
        fun onItemSelected(position: Int)
    }

    private var listener: OnItemSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnItemSelectedListener
        if (listener == null) {
            throw ClassCastException("$context must implement OnItemSelectedListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        val binding = FragmentRouteListBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize your ListView and populate it with data
        val listView: ListView = binding.listview

        val names = arrayOf("Tour 1", "Tour 2", "Tour 3", "Tour 4")
        val descriptions = arrayOf("Description 1", "Description 2", "Description 3", "Description 4")
        val imageIds = intArrayOf(
            R.drawable.default_image, R.drawable.default_image, R.drawable.default_image, R.drawable.default_image
        )

        val tourList = ArrayList<TourItem>()

        for (i in names.indices) {
            val tourItem = TourItem(names[i], descriptions[i], imageIds[i])
            tourList.add(tourItem)
        }

        val tourAdapter = TourAdapter(requireContext(), tourList)
        listView.adapter = tourAdapter

        // Set item click listener for the ListView
        listView.setOnItemClickListener { _, _, position, _ ->
            listener?.onItemSelected(position)
        }
        return view
    }
}