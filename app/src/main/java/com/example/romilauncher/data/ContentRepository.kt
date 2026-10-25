package com.example.romilauncher.data

/**
 * A provider of media rows. Android exposes no public API to read other apps' watch history or
 * recommendations, so Romi ships with no sources. Nothing is invented: when no source is registered,
 * the content sections stay empty and the UI says so.
 */
interface ContentSource {
    val id: String
    val name: String
    suspend fun load(section: MediaSection): List<MediaItem> = emptyList()
    suspend fun search(query: String): List<MediaItem> = emptyList()
}

data class ContentUi(
    val configured: Boolean = false,
    val sections: Map<MediaSection, List<MediaItem>> = emptyMap(),
) {
    fun items(section: MediaSection): List<MediaItem> = sections[section].orEmpty()
}

class ContentRepository(private val sources: List<ContentSource> = emptyList()) {

    val isConfigured: Boolean get() = sources.isNotEmpty()

    suspend fun snapshot(): ContentUi {
        if (!isConfigured) return ContentUi()
        val sections = MediaSection.entries.associateWith { section ->
            sources.flatMap { source -> runCatching { source.load(section) }.getOrDefault(emptyList()) }
        }
        return ContentUi(configured = true, sections = sections)
    }

    suspend fun search(query: String): List<MediaItem> =
        sources.flatMap { source -> runCatching { source.search(query) }.getOrDefault(emptyList()) }
}
