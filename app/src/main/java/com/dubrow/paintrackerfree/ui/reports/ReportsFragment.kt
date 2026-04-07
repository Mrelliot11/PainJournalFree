package com.dubrow.paintrackerfree.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dubrow.paintrackerfree.PainTrackerApp
import com.dubrow.paintrackerfree.R
import com.dubrow.paintrackerfree.databinding.FragmentReportsBinding
import com.dubrow.paintrackerfree.util.ViewModelFactory
import com.dubrow.paintrackerfree.util.applyStatusBarPadding
import com.google.android.material.chip.Chip

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PainTrackerApp).repository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBar.applyStatusBarPadding()

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.selectedDays.value = when (checkedId) {
                    R.id.btn7days -> 7
                    R.id.btn30days -> 30
                    else -> 90
                }
            }
        }
        binding.btn30days.isChecked = true

        viewModel.filteredEntries.observe(viewLifecycleOwner) { entries ->
            binding.chart.entries = entries
        }

        viewModel.availablePainTypes.observe(viewLifecycleOwner) { types ->
            rebuildPainTypeChips(types)
        }

        viewModel.availableLocations.observe(viewLifecycleOwner) { locations ->
            rebuildLocationChips(locations)
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            if (stats == null) {
                binding.cardStats.visibility = View.GONE
                binding.tvNoData.visibility = View.VISIBLE
            } else {
                binding.cardStats.visibility = View.VISIBLE
                binding.tvNoData.visibility = View.GONE
                binding.tvAvgPain.text = getString(R.string.stat_avg_pain, stats.avgPain)
                binding.tvEntryCount.text = getString(R.string.stat_count, stats.entryCount)
                binding.tvPainRange.text = getString(R.string.stat_range, stats.minPain, stats.maxPain)
                binding.tvTopLocation.text = getString(R.string.stat_top_location, stats.topLocation)
                binding.tvTopTrigger.text = getString(R.string.stat_top_trigger, stats.topTrigger)
            }
        }

        viewModel.triggerInsights.observe(viewLifecycleOwner) { categories ->
            binding.llInsightsCategories.removeAllViews()
            if (categories.isEmpty()) {
                binding.cardInsights.visibility = View.GONE
                return@observe
            }
            binding.cardInsights.visibility = View.VISIBLE
            categories.forEachIndexed { index, category ->
                if (index > 0) addDivider()
                addCategorySection(category)
            }
        }

    }

    private fun rebuildPainTypeChips(types: List<String>) {
        binding.chipGroupPainType.removeAllViews()

        // "All" chip
        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.filter_all)
            isCheckable = true
            isChecked = viewModel.selectedPainType.value == null
        }
        allChip.setOnClickListener {
            viewModel.selectedPainType.value = null
        }
        binding.chipGroupPainType.addView(allChip)

        types.forEach { type ->
            val chip = Chip(requireContext()).apply {
                text = type
                isCheckable = true
                isChecked = viewModel.selectedPainType.value == type
            }
            chip.setOnClickListener {
                viewModel.selectedPainType.value = type
            }
            binding.chipGroupPainType.addView(chip)
        }

        // Show/hide the filter row depending on whether there are any types to filter
        binding.painTypeFilterRow.visibility = if (types.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun rebuildLocationChips(locations: List<String>) {
        binding.chipGroupLocation.removeAllViews()

        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.filter_all)
            isCheckable = true
            isChecked = viewModel.selectedLocation.value == null
        }
        allChip.setOnClickListener {
            viewModel.selectedLocation.value = null
        }
        binding.chipGroupLocation.addView(allChip)

        locations.forEach { location ->
            val chip = Chip(requireContext()).apply {
                text = location
                isCheckable = true
                isChecked = viewModel.selectedLocation.value == location
            }
            chip.setOnClickListener {
                viewModel.selectedLocation.value = location
            }
            binding.chipGroupLocation.addView(chip)
        }

        binding.locationFilterRow.visibility = if (locations.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun addCategorySection(category: InsightCategory) {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density

        val header = TextView(ctx).apply {
            text = category.title
            textSize = 14f
            setTextColor(ctx.getColor(R.color.text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, (8 * dp).toInt(), 0, (6 * dp).toInt())
        }
        binding.llInsightsCategories.addView(header)

        category.items.forEach { (label, count) ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (2 * dp).toInt(), 0, (2 * dp).toInt())
            }
            val tvLabel = TextView(ctx).apply {
                text = label
                textSize = 13f
                setTextColor(ctx.getColor(R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvCount = TextView(ctx).apply {
                text = resources.getQuantityString(R.plurals.insights_count, count, count)
                textSize = 13f
                setTextColor(ctx.getColor(R.color.text_secondary))
            }
            row.addView(tvLabel)
            row.addView(tvCount)
            binding.llInsightsCategories.addView(row)
        }
    }

    private fun addDivider() {
        val dp = resources.displayMetrics.density
        val divider = View(requireContext()).apply {
            setBackgroundColor(requireContext().getColor(R.color.divider))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).apply { topMargin = (8 * dp).toInt() }
        }
        binding.llInsightsCategories.addView(divider)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
