package com.example.romilauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Discovers launchable apps through [android.content.pm.PackageManager] and keeps the list current
 * when packages are installed, updated, changed or removed.
 */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private var refreshJob: Job? = null

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = refresh()
        }, filter)
        refresh()
    }

    /** Coalesces bursts of package broadcasts (an update sends several) into one scan. */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay(250)
            _apps.value = withContext(Dispatchers.IO) { queryApps() }
            _loaded.value = true
        }
    }

    fun launch(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return false
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }

    private fun queryApps(): List<AppEntry> {
        val pm = context.packageManager
        val found = LinkedHashMap<String, AppEntry>()
        // Leanback entries first so a package with both keeps its TV activity.
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER to true, Intent.CATEGORY_LAUNCHER to false)
        for ((category, leanback) in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in runCatching { pm.queryIntentActivities(query, 0) }.getOrDefault(emptyList())) {
                val activity = info.activityInfo ?: continue
                val pkg = activity.packageName
                if (pkg == context.packageName || !activity.exported || !activity.enabled) continue
                if (found.containsKey(pkg)) continue
                val pkgInfo = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull() ?: continue
                val flags = activity.applicationInfo?.flags ?: 0
                val pureSystem = flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                    flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
                found[pkg] = AppEntry(
                    packageName = pkg,
                    activityName = activity.name,
                    label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg,
                    isLeanback = leanback,
                    isSystem = pureSystem,
                    lastUpdateTime = pkgInfo.lastUpdateTime,
                )
            }
        }
        return found.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}

class AppImage(val bitmap: ImageBitmap, val isBanner: Boolean)

/** Loads banners (preferred on TV) or icons off the main thread and caches them in memory. */
class AppImageLoader(private val context: Context) {

    private val cache = LruCache<String, AppImage>(160)

    private fun key(app: AppEntry) = "${app.packageName}:${app.lastUpdateTime}"

    fun peek(app: AppEntry): AppImage? = cache.get(key(app))

    suspend fun load(app: AppEntry): AppImage? {
        peek(app)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val info = pm.getActivityInfo(app.component, 0)
                val banner: Drawable? = info.loadBanner(pm)
                val image = if (banner != null) {
                    AppImage(banner.toBitmap(320, 180).asImageBitmap(), isBanner = true)
                } else {
                    AppImage(info.loadIcon(pm).toBitmap(192, 192).asImageBitmap(), isBanner = false)
                }
                cache.put(key(app), image)
                image
            }.getOrNull()
        }
    }
}
