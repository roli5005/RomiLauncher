package com.example.romilauncher

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.romilauncher.ui.ActivityRequest
import com.example.romilauncher.ui.RomiRoot
import com.example.romilauncher.ui.RomiViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: RomiViewModel by viewModels()

    private val pickWallpaper =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) lifecycleScope.launch { vm.importWallpaper(uri) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RomiRoot(vm) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                vm.requests.collect(::handle)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshContent()
    }

    // singleTask: pressing Home while Romi is running delivers the intent here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHomePressed()
    }

    private fun handle(request: ActivityRequest) {
        when (request) {
            ActivityRequest.PickWallpaper -> try {
                pickWallpaper.launch(arrayOf("image/*"))
            } catch (_: ActivityNotFoundException) {
                vm.showNotice("This TV has no file picker, so a custom image can't be chosen.")
            }
            ActivityRequest.SetDefaultHome -> setDefaultHome()
        }
    }

    private fun setDefaultHome() {
        val roles = getSystemService(RoleManager::class.java)
        try {
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                    vm.showNotice("Romi is already the home app.")
                } else {
                    @Suppress("DEPRECATION")
                    startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), 1)
                }
            } else {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        } catch (_: ActivityNotFoundException) {
            vm.showNotice("This TV chooses its home app from the Home button or system settings.")
        }
    }
}
