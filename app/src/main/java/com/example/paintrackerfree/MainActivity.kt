package com.example.paintrackerfree

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission granted or denied — reminders already scheduled, system handles delivery */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pad the bottom nav by the navigation bar inset so it sits above the gesture bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(0, 0, 0, navBar.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.visibility = when (destination.id) {
                R.id.logEntryFragment -> View.GONE
                else -> View.VISIBLE
            }
        }

        if (intent.getBooleanExtra(EXTRA_OPEN_LOG_ENTRY, false)) {
            navController.navigate(R.id.action_home_to_logEntry)
        }

        if (intent.action == ACTION_QUICK_LOG) {
            handleQuickLog()
        }

        requestNotificationPermissionIfNeeded()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_QUICK_LOG) {
            handleQuickLog()
        }
    }

    private fun handleQuickLog() {
        val repo = (application as PainTrackerApp).repository
        lifecycleScope.launch {
            repo.insert(PainEntry(painLevel = 5))
        }
        Toast.makeText(this, getString(R.string.quick_log_saved, 5), Toast.LENGTH_SHORT).show()
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_LOG_ENTRY = "open_log_entry"
        const val ACTION_QUICK_LOG = "com.example.paintrackerfree.QUICK_LOG"
    }
}
