package com.example.paintrackerfree.ui.settings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.paintrackerfree.MainActivity
import com.example.paintrackerfree.PainTrackerApp
import com.example.paintrackerfree.R
import com.example.paintrackerfree.databinding.FragmentSettingsBinding
import com.example.paintrackerfree.util.CsvExporter
import com.example.paintrackerfree.util.CsvImporter
import com.example.paintrackerfree.util.CustomOptionsStore
import com.example.paintrackerfree.util.DateUtils
import com.example.paintrackerfree.util.PdfExporter
import com.example.paintrackerfree.util.ReminderScheduler
import com.example.paintrackerfree.util.ReminderStore
import com.example.paintrackerfree.util.ThemeStore
import com.example.paintrackerfree.util.ViewModelFactory
import com.example.paintrackerfree.util.applyStatusBarPadding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PainTrackerApp).repository)
    }

    private var billingClient: BillingClient? = null

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        CoroutineScope(Dispatchers.IO).launch {
            when (val result = CsvImporter.parseCsv(requireContext(), uri)) {
                is CsvImporter.Result.Failure -> showToast(getString(R.string.import_failed, result.message))
                is CsvImporter.Result.Success -> {
                    if (result.entries.isEmpty()) {
                        showToast(getString(R.string.import_nothing))
                        return@launch
                    }
                    val repo = (requireActivity().application as PainTrackerApp).repository
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

    // Product IDs — these must match what you create in the Google Play Console
    @Suppress("PrivatePropertyName", "PrivatePropertyName", "PrivatePropertyName")
    private val TIP_SMALL = "tip_small"
    private val TIP_MEDIUM = "tip_medium"
    private val TIP_LARGE = "tip_large"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBar.applyStatusBarPadding()

        setupThemeSelector()
        setupReminders()
        setupCustomOptions()
        setupImportExport()
        setupDeleteAll()
        setupBilling()
    }

    // --- Theme ---

    private fun setupThemeSelector() {
        val currentMode = ThemeStore.getMode(requireContext())
        binding.rgTheme.check(
            when (currentMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.rb_theme_light
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.rb_theme_dark
                else -> R.id.rb_theme_system
            }
        )
        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rb_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rb_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            ThemeStore.setMode(requireContext(), mode)
        }
    }

    // --- Reminders ---

    private fun setupReminders() {
        (activity as? MainActivity)?.requestNotificationPermissionIfNeeded()
        refreshReminderList()
        binding.btnAddReminder.setOnClickListener { showAddReminderPicker() }
    }

    private fun refreshReminderList() {
        binding.llReminders.removeAllViews()
        val times = ReminderStore.getTimes(requireContext())
        if (times.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = getString(R.string.settings_no_reminders)
                textSize = 13f
                setTextColor(requireContext().getColor(R.color.text_secondary))
            }
            binding.llReminders.addView(tv)
        } else {
            val dp = resources.displayMetrics.density
            times.forEach { hhmm ->
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, (4 * dp).toInt(), 0, (4 * dp).toInt())
                }
                val tvTime = TextView(requireContext()).apply {
                    text = formatHhmm(hhmm)
                    textSize = 15f
                    setTextColor(requireContext().getColor(R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val btnRemove = android.widget.Button(
                    requireContext(),
                    null,
                    android.R.attr.borderlessButtonStyle
                ).apply {
                    text = getString(R.string.settings_reminder_remove)
                    textSize = 13f
                    setTextColor(requireContext().getColor(R.color.pain_high))
                    setOnClickListener {
                        ReminderScheduler.cancel(requireContext(), hhmm)
                        ReminderStore.removeTime(requireContext(), hhmm)
                        refreshReminderList()
                    }
                }
                row.addView(tvTime)
                row.addView(btnRemove)
                binding.llReminders.addView(row)
            }
        }
    }

    private fun showAddReminderPicker() {
        val cal = java.util.Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                val hhmm = String.format(Locale.US, "%02d:%02d", hour, minute)
                ReminderStore.addTime(requireContext(), hhmm)
                ReminderScheduler.schedule(requireContext(), hhmm)
                refreshReminderList()
            },
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
            false
        ).show()
    }

    private fun formatHhmm(hhmm: String): String {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        val ampm = if (h < 12) "AM" else "PM"
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, m, ampm)
    }

    // --- Custom options ---

    private fun setupCustomOptions() {
        refreshChips()

        binding.btnAddLocation.setOnClickListener {
            showAddDialog { value ->
                CustomOptionsStore.addLocation(requireContext(), value)
                refreshChips()
            }
        }
        binding.btnAddPainType.setOnClickListener {
            showAddDialog { value ->
                CustomOptionsStore.addPainType(requireContext(), value)
                refreshChips()
            }
        }
        binding.btnAddTrigger.setOnClickListener {
            showAddDialog { value ->
                CustomOptionsStore.addTrigger(requireContext(), value)
                refreshChips()
            }
        }
    }

    private fun refreshChips() {
        populateChipGroup(
            binding.cgCustomLocations,
            CustomOptionsStore.getLocations(requireContext())
        ) { value -> CustomOptionsStore.removeLocation(requireContext(), value); refreshChips() }

        populateChipGroup(
            binding.cgCustomPainTypes,
            CustomOptionsStore.getPainTypes(requireContext())
        ) { value -> CustomOptionsStore.removePainType(requireContext(), value); refreshChips() }

        populateChipGroup(
            binding.cgCustomTriggers,
            CustomOptionsStore.getTriggers(requireContext())
        ) { value -> CustomOptionsStore.removeTrigger(requireContext(), value); refreshChips() }
    }

    private fun populateChipGroup(
        group: com.google.android.material.chip.ChipGroup,
        items: List<String>,
        onRemove: (String) -> Unit
    ) {
        group.removeAllViews()
        if (items.isEmpty()) {
            val empty = Chip(requireContext()).apply {
                text = getString(R.string.settings_no_custom)
                isEnabled = false
            }
            group.addView(empty)
        } else {
            items.forEach { item ->
                val chip = Chip(requireContext()).apply {
                    text = item
                    isCloseIconVisible = true
                    setOnCloseIconClickListener { onRemove(item) }
                }
                group.addView(chip)
            }
        }
    }

    private fun showAddDialog(onAdd: (String) -> Unit) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.custom_option_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding(48, 24, 48, 8)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_custom_title)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) onAdd(value)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // --- Import & Export ---

    private fun setupImportExport() {
        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        }

        binding.btnExport.setOnClickListener {
            showExportRangeDialog()
        }
    }

    private fun showExportRangeDialog() {
        val rangeOptions = arrayOf(
            getString(R.string.export_range_all),
            getString(R.string.export_range_30),
            getString(R.string.export_range_90),
            getString(R.string.export_range_custom)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.export_range_title)
            .setItems(rangeOptions) { _, which ->
                when (which) {
                    0 -> exportWithEntries { viewModel.allEntries.value ?: emptyList() }
                    1 -> exportWithEntries {
                        (viewModel.allEntries.value ?: emptyList())
                            .filter { it.timestamp >= DateUtils.daysAgoMs(30) }
                    }
                    2 -> exportWithEntries {
                        (viewModel.allEntries.value ?: emptyList())
                            .filter { it.timestamp >= DateUtils.daysAgoMs(90) }
                    }
                    3 -> showCustomRangePicker()
                }
            }
            .show()
    }

    private fun showCustomRangePicker() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, startYear, startMonth, startDay ->
                val startCal = Calendar.getInstance().apply {
                    set(startYear, startMonth, startDay, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDay ->
                        val endCal = Calendar.getInstance().apply {
                            set(endYear, endMonth, endDay, 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        val startMs = startCal.timeInMillis
                        val endMs = endCal.timeInMillis
                        exportWithEntries {
                            (viewModel.allEntries.value ?: emptyList())
                                .filter { it.timestamp in startMs..endMs }
                        }
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setTitle(getString(R.string.export_date_end))
                    datePicker.minDate = startCal.timeInMillis
                }.show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle(getString(R.string.export_date_start))
        }.show()
    }

    private fun exportWithEntries(getEntries: () -> List<com.example.paintrackerfree.data.model.PainEntry>) {
        val entries = getEntries()
        if (entries.isEmpty()) {
            Toast.makeText(requireContext(), R.string.export_range_empty, Toast.LENGTH_SHORT).show()
            return
        }

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
                    0 -> CoroutineScope(Dispatchers.IO).launch {
                        val name = CsvExporter.saveToDownloads(requireContext(), entries)
                        showSaveResult(name)
                    }
                    1 -> CoroutineScope(Dispatchers.IO).launch {
                        val intent = CsvExporter.buildShareIntent(requireContext(), entries)
                        withContext(Dispatchers.Main) {
                            startActivity(android.content.Intent.createChooser(intent, getString(R.string.export_title)))
                        }
                    }
                    2 -> CoroutineScope(Dispatchers.IO).launch {
                        val name = PdfExporter.saveToDownloads(requireContext(), entries)
                        showSaveResult(name)
                    }
                    3 -> CoroutineScope(Dispatchers.IO).launch {
                        val intent = PdfExporter.buildShareIntent(requireContext(), entries)
                        withContext(Dispatchers.Main) {
                            startActivity(android.content.Intent.createChooser(intent, getString(R.string.export_title)))
                        }
                    }
                }
            }
            .show()
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

    // --- Delete all ---

    private fun setupDeleteAll() {
        binding.btnDeleteAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_delete_all_confirm_title)
                .setMessage(R.string.settings_delete_all_confirm_message)
                .setPositiveButton(R.string.settings_delete_all_confirm_button) { _, _ ->
                    viewModel.deleteAllEntries()
                    Snackbar.make(binding.root, R.string.settings_delete_all_success, Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    // --- Billing / Tip jar ---

    private fun setupBilling() {
        val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient?.acknowledgePurchase(params) { _ -> }
                    }
                }
                Snackbar.make(binding.root, R.string.settings_tip_thank_you, Snackbar.LENGTH_LONG).show()
            } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                Snackbar.make(binding.root, R.string.settings_tip_error, Snackbar.LENGTH_SHORT).show()
            }
        }

        billingClient = BillingClient.newBuilder(requireContext())
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                // Connection ready — buttons will query product details on click
            }
            override fun onBillingServiceDisconnected() { /* will retry on next click */ }
        })

        binding.btnTipSmall.setOnClickListener { launchTip(TIP_SMALL) }
        binding.btnTipMedium.setOnClickListener { launchTip(TIP_MEDIUM) }
        binding.btnTipLarge.setOnClickListener { launchTip(TIP_LARGE) }
    }

    private fun launchTip(productId: String) {
        val client = billingClient
        if (client == null || !client.isReady) {
            Snackbar.make(binding.root, R.string.settings_tip_unavailable, Snackbar.LENGTH_SHORT).show()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK
                || productDetailsList.isEmpty()
            ) {
                activity?.runOnUiThread {
                    Snackbar.make(binding.root, R.string.settings_tip_unavailable, Snackbar.LENGTH_SHORT).show()
                }
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList[0]
            val offerToken = productDetails.oneTimePurchaseOfferDetails?.zza() ?: ""

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()

            activity?.runOnUiThread {
                activity?.let { client.launchBillingFlow(it, billingFlowParams) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        billingClient?.endConnection()
        billingClient = null
        _binding = null
    }
}
