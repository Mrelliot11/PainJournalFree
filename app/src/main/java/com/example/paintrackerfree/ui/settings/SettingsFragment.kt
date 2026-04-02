package com.example.paintrackerfree.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.billingclient.api.*
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.paintrackerfree.PainTrackerApp
import com.example.paintrackerfree.R
import com.example.paintrackerfree.databinding.FragmentSettingsBinding
import com.example.paintrackerfree.util.CustomOptionsStore
import com.example.paintrackerfree.util.ThemeStore
import com.example.paintrackerfree.util.ViewModelFactory
import com.example.paintrackerfree.util.applyStatusBarPadding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory((requireActivity().application as PainTrackerApp).repository)
    }

    private var billingClient: BillingClient? = null

    // Product IDs — these must match what you create in the Google Play Console
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
        setupCustomOptions()
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
