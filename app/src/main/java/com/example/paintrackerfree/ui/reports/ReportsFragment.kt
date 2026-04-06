package com.example.paintrackerfree.ui.reports

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.paintrackerfree.PainTrackerApp
import com.example.paintrackerfree.R
import com.example.paintrackerfree.databinding.FragmentReportsBinding
import com.example.paintrackerfree.util.applyStatusBarPadding
import com.example.paintrackerfree.util.CsvExporter
import com.example.paintrackerfree.util.CsvImporter
import com.example.paintrackerfree.util.PdfExporter
import com.example.paintrackerfree.util.ViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PainTrackerApp).repository)
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runInBackground {
            when (val result = CsvImporter.parseCsv(requireContext(), uri)) {
                is CsvImporter.Result.Failure -> showToast(getString(R.string.import_failed, result.message))
                is CsvImporter.Result.Success -> {
                    if (result.entries.isEmpty()) {
                        showToast(getString(R.string.import_nothing))
                        return@runInBackground
                    }
                    val repo = (requireActivity().application as PainTrackerApp).repository
                    // Deduplicate in memory, then insert in one transaction so Room
                    // emits a single Flow update and all LiveData observers refresh together.
                    val existingTimestamps = result.entries
                        .filter { repo.existsByTimestamp(it.timestamp) }
                        .map { it.timestamp }
                        .toHashSet()
                    val toInsert = result.entries.filter { it.timestamp !in existingTimestamps }
                    if (toInsert.isNotEmpty()) repo.insertAll(toInsert)
                    val duplicates = existingTimestamps.size
                    val inserted = toInsert.size
                    val totalSkipped = result.skipped + duplicates
                    val msg = if (totalSkipped > 0)
                        getString(R.string.import_success_skipped, inserted, totalSkipped)
                    else
                        getString(R.string.import_success, inserted)
                    showToast(msg)
                }
            }
        }
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

        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        }

        binding.btnExport.setOnClickListener {
            val entries = viewModel.filteredEntries.value ?: return@setOnClickListener
            if (entries.isEmpty()) return@setOnClickListener

            val options = arrayOf(
                getString(R.string.export_save_downloads_csv),
                getString(R.string.export_share_csv),
                getString(R.string.export_save_downloads_pdf),
                getString(R.string.export_share_pdf)
            )

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.export_choose_title)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> runInBackground {
                            val name = CsvExporter.saveToDownloads(requireContext(), entries)
                            showSaveResult(name)
                        }
                        1 -> runInBackground {
                            val intent = CsvExporter.buildShareIntent(requireContext(), entries)
                            withContext(Dispatchers.Main) {
                                startActivity(android.content.Intent.createChooser(intent, getString(R.string.export_title)))
                            }
                        }
                        2 -> runInBackground {
                            val name = PdfExporter.saveToDownloads(requireContext(), entries)
                            showSaveResult(name)
                        }
                        3 -> runInBackground {
                            val intent = PdfExporter.buildShareIntent(requireContext(), entries)
                            withContext(Dispatchers.Main) {
                                startActivity(android.content.Intent.createChooser(intent, getString(R.string.export_title)))
                            }
                        }
                    }
                }
                .show()
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

    private fun runInBackground(block: suspend CoroutineScope.() -> Unit) {
        CoroutineScope(Dispatchers.IO).launch(block = block)
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun showSaveResult(fileName: String?) {
        val message = if (fileName != null)
            getString(R.string.export_saved_downloads, fileName)
        else
            getString(R.string.export_save_failed)
        showToast(message)
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
