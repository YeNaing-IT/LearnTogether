package com.learntogether.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.learntogether.util.MediaUrlsPartition

private val urlRegex = Regex("""(https?://[^\s]+)""", RegexOption.IGNORE_CASE)
private val imageExt = Regex("""\.(jpg|jpeg|png|gif|webp)(\?.*)?$""", RegexOption.IGNORE_CASE)
private val videoExt = Regex("""\.(mp4|webm|m3u8)(\?.*)?$""", RegexOption.IGNORE_CASE)
private val audioExt = Regex("""\.(mp3|m4a|aac|wav|ogg)(\?.*)?$""", RegexOption.IGNORE_CASE)

@Composable
fun PostRichTextContent(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val segments = remember(text) { splitWithUrls(text) }
    Column(modifier = modifier) {
        segments.forEach { seg ->
            when (seg) {
                is Segment.TextSeg -> {
                    if (seg.value.isNotEmpty()) {
                        Text(
                            text = seg.value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is Segment.UrlSeg -> {
                    val u = seg.url
                    val lower = u.lowercase()
                    when {
                        imageExt.containsMatchIn(lower) -> {
                            AsyncImage(
                                model = u,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        videoExt.containsMatchIn(lower) -> {
                            InlineMediaPlayer(uri = Uri.parse(u))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        audioExt.containsMatchIn(lower) -> {
                            InlineMediaPlayer(uri = Uri.parse(u), minHeight = 56.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        else -> {
                            val link = buildAnnotatedString {
                                pushStringAnnotation(tag = "URL", annotation = u)
                                pushStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                                append(u)
                                pop()
                                pop()
                            }
                            ClickableText(
                                text = link,
                                style = MaterialTheme.typography.bodyLarge,
                                onClick = { offset ->
                                    link.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ann.item)))
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Renders video and/or audio attachment fields from a post (separate from inline URLs in [text]). */
@Composable
fun PostAttachedMedia(
    videoUrl: String?,
    audioUrl: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val videos = MediaUrlsPartition.parseCommaSeparated(videoUrl?.trim().orEmpty())
        val audios = MediaUrlsPartition.parseCommaSeparated(audioUrl?.trim().orEmpty())
        videos.forEach { u ->
            InlineMediaPlayer(uri = Uri.parse(u))
            Spacer(modifier = Modifier.height(8.dp))
        }
        audios.forEach { u ->
            InlineMediaPlayer(uri = Uri.parse(u), minHeight = 56.dp)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InlineMediaPlayer(uri: Uri, minHeight: Dp = 220.dp) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .height(if (minHeight < 100.dp) minHeight else 220.dp)
            .clip(RoundedCornerShape(12.dp)),
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        }
    )
}

private sealed class Segment {
    data class TextSeg(val value: String) : Segment()
    data class UrlSeg(val url: String) : Segment()
}

private fun splitWithUrls(input: String): List<Segment> {
    if (input.isBlank()) return emptyList()
    val out = mutableListOf<Segment>()
    var idx = 0
    urlRegex.findAll(input).forEach { m ->
        if (m.range.first > idx) {
            out.add(Segment.TextSeg(input.substring(idx, m.range.first)))
        }
        var url = m.value.trimEnd('.', ',', ';', ')')
        out.add(Segment.UrlSeg(url))
        idx = m.range.last + 1
    }
    if (idx < input.length) out.add(Segment.TextSeg(input.substring(idx)))
    return out
}
