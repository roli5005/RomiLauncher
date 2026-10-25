package com.example.romilauncher

import android.app.Application
import android.content.Context
import com.example.romilauncher.data.AppImageLoader
import com.example.romilauncher.data.AppRepository
import com.example.romilauncher.data.ContentRepository
import com.example.romilauncher.data.SettingsStore
import com.example.romilauncher.data.WallpaperStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = SettingsStore(context)
    val apps = AppRepository(context, scope)
    val images = AppImageLoader(context)
    val content = ContentRepository()
    val wallpaper = WallpaperStore(context)
}

class RomiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
