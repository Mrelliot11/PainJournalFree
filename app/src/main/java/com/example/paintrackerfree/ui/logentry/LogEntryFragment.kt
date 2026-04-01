package com.example.paintrackerfree.ui.logentry

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.paintrackerfree.PainTrackerApp
import com.example.paintrackerfree.R
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.databinding.FragmentLogEntryBinding
import com.example.paintrackerfree.util.DateUtils
import com.example.paintrackerfree.util.ViewModelFactory
import com.example.paintrackerfree.util.applyStatusBarPadding
import com.google.android.material.chip.Chip
import java.util.Calendar

class LogEntryFragment : Fragment() {

    private var _binding: FragmentLogEntryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogEntryViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PainTrackerApp).repository)
    }

    private var entryId = 0L
    private var selectedTimestamp = System.currentTimeMillis()
    private var existingEntry: PainEntry? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBar.applyStatusBarPadding()

        entryId = arguments?.getLong("entryId", 0L) ?: 0L

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        if (entryId > 0L) {
            binding.toolbar.title = getString(R.string.edit_entry)
            binding.btnDelete.visibility = View.VISIBLE
            viewModel.loadEntry(entryId)
        } else {
            binding.toolbar.title = getString(R.string.log_pain)
            binding.btnDelete.visibility = View.GONE
        }

        setupChips()
        updateDateTimeDisplay()

        binding.tvDateTime.setOnClickListener { pickDate() }

        binding.sliderPainLevel.addOnChangeListener { _, value, _ ->
            binding.tvPainLevelValue.text = value.toInt().toString()
            binding.tvPainLevelValue.setTextColor(painLevelColor(value.toInt()))
        }

        viewModel.existingEntry.observe(viewLifecycleOwner) { entry ->
            entry ?: return@observe
            existingEntry = entry
            selectedTimestamp = entry.timestamp
            updateDateTimeDisplay()
            binding.sliderPainLevel.value = entry.painLevel.toFloat()
            binding.tvPainLevelValue.text = entry.painLevel.toString()
            binding.tvPainLevelValue.setTextColor(painLevelColor(entry.painLevel))
            setCheckedChips(binding.chipGroupLocations, entry.locations)
            setCheckedChips(binding.chipGroupPainTypes, entry.painTypes)
            setCheckedChips(binding.chipGroupTriggers, entry.triggers)
            binding.etMedications.setText(entry.medications)
            binding.ratingMood.rating = entry.mood.toFloat()
            binding.ratingSleep.rating = entry.sleepQuality.toFloat()
            binding.etNotes.setText(entry.notes)
        }

        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved == true) findNavController().navigateUp()
        }

        viewModel.deleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted == true) {
                Toast.makeText(requireContext(), R.string.entry_deleted, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        binding.btnSave.setOnClickListener { saveEntry() }

        binding.btnDelete.setOnClickListener {
            existingEntry?.let { viewModel.deleteEntry(it) }
        }
    }

    private fun setupChips() {
        val locations = resources.getStringArray(R.array.body_locations)
        val painTypes = resources.getStringArray(R.array.pain_types)
        val triggers = resources.getStringArray(R.array.triggers)

        addChips(binding.chipGroupLocations, locations)
        addChips(binding.chipGroupPainTypes, painTypes)
        addChips(binding.chipGroupTriggers, triggers)
    }

    private fun addChips(group: com.google.android.material.chip.ChipGroup, labels: Array<String>) {
        labels.forEach { label ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                tag = label
            }
            group.addView(chip)
        }
    }

    private fun setCheckedChips(group: com.google.android.material.chip.ChipGroup, csv: String) {
        val selected = csv.split(",").map { it.trim() }.toSet()
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            chip.isChecked = chip.tag in selected
        }
    }

    private fun getCheckedChips(group: com.google.android.material.chip.ChipGroup): String {
        val checked = mutableListOf<String>()
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) checked.add(chip.tag as String)
        }
        return checked.joinToString(", ")
    }

    private fun updateDateTimeDisplay() {
        binding.tvDateTime.text = DateUtils.formatDateTime(selectedTimestamp)
    }

    private fun pickDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d)
            TimePickerDialog(requireContext(), { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, min)
                selectedTimestamp = cal.timeInMillis
                updateDateTimeDisplay()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveEntry() {
        viewModel.saveEntry(
            entryId = entryId,
            timestamp = selectedTimestamp,
            painLevel = binding.sliderPainLevel.value.toInt(),
            locations = getCheckedChips(binding.chipGroupLocations),
            painTypes = getCheckedChips(binding.chipGroupPainTypes),
            triggers = getCheckedChips(binding.chipGroupTriggers),
            medications = binding.etMedications.text?.toString()?.trim() ?: "",
            mood = binding.ratingMood.rating.toInt().coerceIn(1, 5),
            sleepQuality = binding.ratingSleep.rating.toInt().coerceIn(1, 5),
            notes = binding.etNotes.text?.toString()?.trim() ?: ""
        )
    }

    private fun painLevelColor(level: Int): Int {
        val colorRes = when {
            level <= 3 -> R.color.pain_low
            level <= 6 -> R.color.pain_mid
            else -> R.color.pain_high
        }
        return requireContext().getColor(colorRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
