package com.example.paintrackerfree.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.paintrackerfree.PainTrackerApp
import com.example.paintrackerfree.R
import com.example.paintrackerfree.databinding.FragmentHomeBinding
import com.example.paintrackerfree.ui.history.HistoryAdapter
import com.example.paintrackerfree.ui.history.HistoryItem
import com.example.paintrackerfree.util.ViewModelFactory
import com.example.paintrackerfree.util.applyStatusBarPadding
import com.google.android.material.snackbar.Snackbar

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

        val adapter = HistoryAdapter(
            onEntryClick = { entry ->
                findNavController().navigate(R.id.action_home_to_logEntry,
                    Bundle().apply { putLong("entryId", entry.id) })
            },
            showDate = true
        )
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

        setupSwipeToDelete(adapter)
    }

    private fun setupSwipeToDelete(adapter: HistoryAdapter) {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                val pos = vh.adapterPosition
                return if (pos != RecyclerView.NO_POSITION && adapter.getEntryAt(pos) != null)
                    super.getSwipeDirs(rv, vh) else 0
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.adapterPosition
                val entry = if (pos != RecyclerView.NO_POSITION) adapter.getEntryAt(pos) else null
                entry ?: return
                viewModel.deleteEntry(entry)
                Snackbar.make(binding.root, R.string.entry_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.restoreLastDeleted() }
                    .show()
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvRecentEntries)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
