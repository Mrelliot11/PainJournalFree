package com.dubrow.paintrackerfree.ui.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dubrow.paintrackerfree.PainTrackerApp
import com.dubrow.paintrackerfree.R
import com.dubrow.paintrackerfree.databinding.FragmentHistoryBinding
import com.dubrow.paintrackerfree.util.BehaviourStore
import com.dubrow.paintrackerfree.util.ViewModelFactory
import com.dubrow.paintrackerfree.util.applyStatusBarPadding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.RangeSlider
import com.google.android.material.snackbar.Snackbar

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PainTrackerApp).repository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBar.applyStatusBarPadding()

        val adapter = HistoryAdapter(
            onEntryClick = { entry ->
                findNavController().navigate(R.id.action_history_to_logEntry,
                    Bundle().apply { putLong("entryId", entry.id) })
            },
            onEntryLongClick = { entry ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.duplicate_entry_title)
                    .setMessage(R.string.duplicate_entry_message)
                    .setPositiveButton(R.string.duplicate) { _, _ ->
                        findNavController().navigate(R.id.action_history_to_logEntry,
                            Bundle().apply { putLong("duplicateFromId", entry.id) })
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        viewModel.historyItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.tvEmpty.isVisible = items.isEmpty()
        }

        viewModel.entryCount.observe(viewLifecycleOwner) { count ->
            binding.toolbar.subtitle = resources.getQuantityString(R.plurals.entry_count, count, count)
        }

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_history_to_logEntry)
        }

        setupSearch()
        setupFilterPanel()
        setupSwipeToDelete(adapter)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchQuery.value = s?.toString() ?: ""
            }
        })
    }

    private fun setupFilterPanel() {
        binding.btnFilterToggle.setOnClickListener {
            val visible = binding.llFilters.visibility == View.VISIBLE
            binding.llFilters.visibility = if (visible) View.GONE else View.VISIBLE
        }

        // Pain range slider
        updatePainRangeLabel(0, 10)
        binding.sliderPain.addOnChangeListener { slider, _, _ ->
            val min = slider.values[0].toInt()
            val max = slider.values[1].toInt()
            viewModel.minPain.value = min
            viewModel.maxPain.value = max
            updatePainRangeLabel(min, max)
        }

        // Location chips — rebuild when available locations OR current selection changes
        val rebuildLocations = {
            rebuildFilterChips(
                group = binding.chipGroupLocationFilter,
                items = viewModel.availableLocations.value ?: emptyList(),
                currentSelection = viewModel.locationFilter.value,
                onSelect = { viewModel.locationFilter.value = it }
            )
        }
        viewModel.availableLocations.observe(viewLifecycleOwner) { rebuildLocations() }
        viewModel.locationFilter.observe(viewLifecycleOwner) { rebuildLocations() }

        // Trigger chips — rebuild when available triggers OR current selection changes
        val rebuildTriggers = {
            rebuildFilterChips(
                group = binding.chipGroupTriggerFilter,
                items = viewModel.availableTriggers.value ?: emptyList(),
                currentSelection = viewModel.triggerFilter.value,
                onSelect = { viewModel.triggerFilter.value = it }
            )
        }
        viewModel.availableTriggers.observe(viewLifecycleOwner) { rebuildTriggers() }
        viewModel.triggerFilter.observe(viewLifecycleOwner) { rebuildTriggers() }

        binding.btnClearFilters.setOnClickListener {
            viewModel.clearFilters()
            binding.etSearch.setText("")
            binding.sliderPain.values = listOf(0f, 10f)
            updatePainRangeLabel(0, 10)
        }
    }

    private fun updatePainRangeLabel(min: Int, max: Int) {
        binding.tvPainRangeLabel.text = getString(R.string.history_pain_range_label, min, max)
    }

    private fun rebuildFilterChips(
        group: com.google.android.material.chip.ChipGroup,
        items: List<String>,
        currentSelection: String?,
        onSelect: (String?) -> Unit
    ) {
        group.removeAllViews()
        items.forEach { item ->
            val chip = Chip(requireContext()).apply {
                text = item
                isCheckable = true
                isChecked = item == currentSelection
                setOnCheckedChangeListener { _, checked ->
                    if (checked) onSelect(item) else onSelect(null)
                }
            }
            group.addView(chip)
        }
    }

    private fun setupSwipeToDelete(adapter: HistoryAdapter) {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                if (!BehaviourStore.isSwipeToDeleteEnabled(requireContext())) return 0
                val pos = vh.bindingAdapterPosition
                return if (pos != RecyclerView.NO_POSITION && adapter.getEntryAt(pos) != null)
                    super.getSwipeDirs(rv, vh) else 0
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.bindingAdapterPosition
                val entry = if (pos != RecyclerView.NO_POSITION) adapter.getEntryAt(pos) else null
                entry ?: return
                viewModel.deleteEntry(entry)
                Snackbar.make(binding.root, R.string.entry_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.restoreLastDeleted() }
                    .show()
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvHistory)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
