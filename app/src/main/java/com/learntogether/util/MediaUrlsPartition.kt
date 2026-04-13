package com.learntogether.util

/**
 * Parses a single comma-separated media URL field into the three storage columns
 * (image URLs list, video URLs, audio URLs) used by posts and chapters.
 */
object MediaUrlsPartition {

    private val imageExt = Regex("""\.(jpg|jpeg|png|gif|webp|bmp|svg)(\?.*)?$""", RegexOption.IGNORE_CASE)
    private val videoExt = Regex("""\.(mp4|webm|m3u8|mov|mkv)(\?.*)?$""", RegexOption.IGNORE_CASE)
    private val audioExt = Regex("""\.(mp3|m4a|aac|wav|ogg|flac)(\?.*)?$""", RegexOption.IGNORE_CASE)

    enum class Kind { IMAGE, VIDEO, AUDIO, UNKNOWN }

    fun parseCommaSeparated(raw: String): List<String> =
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun classify(url: String): Kind {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com/") || lower.contains("youtu.be/") -> Kind.VIDEO
            lower.contains("vimeo.com") -> Kind.VIDEO
            lower.contains("soundcloud.com") -> Kind.AUDIO
            lower.contains("open.spotify.com/") -> Kind.AUDIO
            // Common image CDNs / hosts without a clear file extension in the path
            lower.contains("unsplash.com") -> Kind.IMAGE
            lower.contains("pexels.com") -> Kind.IMAGE
            lower.contains("pixabay.com") -> Kind.IMAGE
            lower.contains("pbs.twimg.com") || lower.contains("twimg.com") -> Kind.IMAGE
            imageExt.containsMatchIn(lower) -> Kind.IMAGE
            videoExt.containsMatchIn(lower) -> Kind.VIDEO
            audioExt.containsMatchIn(lower) -> Kind.AUDIO
            else -> Kind.UNKNOWN
        }
    }

    /**
     * [UNKNOWN] goes to images: many real image URLs have no extension (CDNs, signed URLs). The UI only
     * renders galleries from the image column; misclassified extension-less video links fail softly in Coil.
     */
    fun partition(urls: List<String>): Triple<List<String>, List<String>, List<String>> {
        val images = mutableListOf<String>()
        val videos = mutableListOf<String>()
        val audios = mutableListOf<String>()
        for (u in urls) {
            when (classify(u)) {
                Kind.IMAGE -> images.add(u)
                Kind.VIDEO -> videos.add(u)
                Kind.AUDIO -> audios.add(u)
                Kind.UNKNOWN -> images.add(u)
            }
        }
        return Triple(images, videos, audios)
    }

    /** For create/update: one text field → entity fields. */
    fun splitToStorageFields(combinedInput: String): Triple<String, String, String> {
        val (i, v, a) = partition(parseCommaSeparated(combinedInput))
        return Triple(
            i.joinToString(","),
            v.joinToString(","),
            a.joinToString(",")
        )
    }

    /** For edit UI: entity fields → one text field (order: images, then videos, then audio). */
    fun mergeFromStorage(imageUrls: String, videoUrl: String, audioUrl: String): String =
        buildList {
            addAll(parseCommaSeparated(imageUrls))
            addAll(parseCommaSeparated(videoUrl))
            addAll(parseCommaSeparated(audioUrl))
        }.joinToString(", ")

    /** First URL in a comma-separated list (for course cover, etc.). Coil cannot load multiple URLs at once. */
    fun firstCommaSeparatedUrl(raw: String): String? =
        parseCommaSeparated(raw).firstOrNull()

    /** True if the stored video field contains at least one URL classified as video (not image-like legacy data). */
    fun hasClassifiedVideoUrls(videoUrlField: String): Boolean =
        parseCommaSeparated(videoUrlField).any { classify(it) == Kind.VIDEO }

    /** True if the stored audio field contains at least one URL classified as audio. */
    fun hasClassifiedAudioUrls(audioUrlField: String): Boolean =
        parseCommaSeparated(audioUrlField).any { classify(it) == Kind.AUDIO }

    /**
     * All image-like URLs for UI, including legacy rows where extension-less images were saved under video/audio.
     */
    fun imageUrlsForDisplay(imageUrls: String, videoUrl: String, audioUrl: String): List<String> {
        val out = LinkedHashSet<String>()
        parseCommaSeparated(imageUrls).forEach { out.add(it) }
        parseCommaSeparated(videoUrl).forEach { u ->
            when (classify(u)) {
                Kind.IMAGE, Kind.UNKNOWN -> out.add(u)
                else -> {}
            }
        }
        parseCommaSeparated(audioUrl).forEach { u ->
            when (classify(u)) {
                Kind.IMAGE, Kind.UNKNOWN -> out.add(u)
                else -> {}
            }
        }
        return out.toList()
    }
}
