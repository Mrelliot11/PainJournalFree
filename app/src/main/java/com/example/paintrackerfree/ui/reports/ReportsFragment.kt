package com.example.paintrackerfree.ui.reports

import android.os.Bundle
import android.view.*
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
            val result = CsvImporter.parseCsv(requireContext(), uri)
            when (result) {
                is CsvImporter.Result.Failure -> showToast(getString(R.string.import_failed, result.message))
                is CsvImporter.Result.Success -> {
                    if (result.entries.isEmpty()) {
                        showToast(getString(R.string.import_nothing))
                        return@runInBackground
                    }
                    val repo = (requireActivity().application as PainTrackerApp).repository
                    var inserted = 0
                    var duplicates = 0
                    result.entries.forEach { entry ->
                        if (repo.existsByTimestamp(entry.timestamp)) {
                            duplicates++
                        } else {
                            repo.insert(entry)
                            inserted++
                        }
                    }
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

        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            binding.chart.entries = entries
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

        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        }

        binding.btnExport.setOnClickListener {
            val entries = viewModel.entries.value ?: return@setOnClickListener
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
