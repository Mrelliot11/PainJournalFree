package com.example.paintrackerfree.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.paintrackerfree.PainTrackerApp
import com.example.paintrackerfree.R
import com.example.paintrackerfree.databinding.FragmentHomeBinding
import com.example.paintrackerfree.ui.history.HistoryAdapter
import com.example.paintrackerfree.ui.history.HistoryItem
import com.example.paintrackerfree.util.ViewModelFactory
import com.example.paintrackerfree.util.applyStatusBarPadding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PainTrackerApp).repository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBar.applyStatusBarPadding()

        val adapter = HistoryAdapter { entry ->
            findNavController().navigate(R.id.action_home_to_logEntry,
                Bundle().apply { putLong("entryId", entry.id) })
        }
        binding.rvRecentEntries.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentEntries.adapter = adapter
        binding.rvRecentEntries.isNestedScrollingEnabled = false

        viewModel.recentEntries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries.map { HistoryItem.Entry(it) })
            binding.tvNoEntries.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.todayEntries.observe(viewLifecycleOwner) { entries ->
            binding.tvTodayCount.text = resources.getQuantityString(
                R.plurals.today_entries, entries.size, entries.size
            )
        }

        viewModel.todayAvgPain.observe(viewLifecycleOwner) { avg ->
            if (avg != null) {
                binding.tvTodayAvg.text = getString(R.string.avg_pain_fmt, avg)
                binding.tvTodayAvg.visibility = View.VISIBLE
            } else {
                binding.tvTodayAvg.visibility = View.INVISIBLE
            }
        }

        binding.fabLogPain.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_logEntry)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
