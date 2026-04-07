package com.dubrow.paintrackerfree.ui.logentry

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isNotEmpty
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dubrow.paintrackerfree.PainTrackerApp
import com.dubrow.paintrackerfree.R
import com.dubrow.paintrackerfree.data.model.PainEntry
import com.dubrow.paintrackerfree.databinding.FragmentLogEntryBinding
import com.dubrow.paintrackerfree.util.CustomOptionsStore
import com.dubrow.paintrackerfree.util.DateUtils
import com.dubrow.paintrackerfree.util.ViewModelFactory
import com.dubrow.paintrackerfree.util.applyStatusBarPadding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
        val duplicateFromId = arguments?.getLong("duplicateFromId", 0L) ?: 0L

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        when {
            entryId > 0L -> {
                binding.toolbar.title = getString(R.string.edit_entry)
                binding.btnDelete.visibility = View.VISIBLE
                // Hide until entry data arrives to prevent default→saved value jitter
                binding.scrollContent.alpha = 0f
                viewModel.loadEntry(entryId)
            }
            duplicateFromId > 0L -> {
                binding.toolbar.title = getString(R.string.log_pain)
                binding.btnDelete.visibility = View.GONE
                binding.scrollContent.alpha = 0f
                viewModel.loadDuplicate(duplicateFromId)
            }
            else -> {
                binding.toolbar.title = getString(R.string.log_pain)
                binding.btnDelete.visibility = View.GONE
                viewModel.checkTodayEntry()
            }
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
            binding.scrollContent.animate().alpha(1f).setDuration(150).start()
        }

        viewModel.hasTodayEntry.observe(viewLifecycleOwner) { hasEntry ->
            if (entryId == 0L) {
                binding.layoutSleepQuality.visibility = if (hasEntry) View.GONE else View.VISIBLE
            }
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
        val ctx = requireContext()
        addChips(binding.chipGroupLocations, resources.getStringArray(R.array.body_locations))
        addChips(binding.chipGroupLocations, CustomOptionsStore.getLocations(ctx).toTypedArray())
        addAddCustomChip(binding.chipGroupLocations) { value ->
            CustomOptionsStore.addLocation(ctx, value)
            addChips(binding.chipGroupLocations, arrayOf(value), beforeLast = true)
            checkChipByTag(binding.chipGroupLocations, value)
        }

        addChips(binding.chipGroupPainTypes, resources.getStringArray(R.array.pain_types))
        addChips(binding.chipGroupPainTypes, CustomOptionsStore.getPainTypes(ctx).toTypedArray())
        addAddCustomChip(binding.chipGroupPainTypes) { value ->
            CustomOptionsStore.addPainType(ctx, value)
            addChips(binding.chipGroupPainTypes, arrayOf(value), beforeLast = true)
            checkChipByTag(binding.chipGroupPainTypes, value)
        }

        addChips(binding.chipGroupTriggers, resources.getStringArray(R.array.triggers))
        addChips(binding.chipGroupTriggers, CustomOptionsStore.getTriggers(ctx).toTypedArray())
        addAddCustomChip(binding.chipGroupTriggers) { value ->
            CustomOptionsStore.addTrigger(ctx, value)
            addChips(binding.chipGroupTriggers, arrayOf(value), beforeLast = true)
            checkChipByTag(binding.chipGroupTriggers, value)
        }
    }

    private fun addChips(
        group: ChipGroup,
        labels: Array<String>,
        beforeLast: Boolean = false
    ) {
        labels.forEach { label ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                tag = label
            }
            if (beforeLast && group.isNotEmpty()) {
                group.addView(chip, group.childCount - 1)
            } else {
                group.addView(chip)
            }
        }
    }

    private fun addAddCustomChip(group: ChipGroup, onAdded: (String) -> Unit) {
        val chip = Chip(requireContext()).apply {
            text = getString(R.string.add_custom)
            isCheckable = false
            chipIcon = androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), R.drawable.ic_add)
            isChipIconVisible = true
            setOnClickListener { showAddCustomDialog(onAdded) }
        }
        group.addView(chip)
    }

    private fun showAddCustomDialog(onAdded: (String) -> Unit) {
        val ctx = requireContext()
        val dp16 = (16 * resources.displayMetrics.density).toInt()
        val dp8 = (8 * resources.displayMetrics.density).toInt()

        val input = TextInputEditText(ctx).apply { setSingleLine() }
        val til = TextInputLayout(ctx, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = getString(R.string.custom_option_hint)
            addView(input)
        }
        val container = FrameLayout(ctx).apply {
            setPadding(dp16 + dp8, dp8, dp16 + dp8, 0)
            addView(til)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_custom_title)
            .setView(container)
            .setPositiveButton(R.string.add) { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) onAdded(value)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun checkChipByTag(group: ChipGroup, tag: String) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            if (chip.tag == tag) { chip.isChecked = true; break }
        }
    }

    private fun setCheckedChips(group: ChipGroup, csv: String) {
        val selected = csv.split(",").map { it.trim() }.toSet()
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            chip.isChecked = chip.tag in selected
        }
    }

    private fun getCheckedChips(group: ChipGroup): String {
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
