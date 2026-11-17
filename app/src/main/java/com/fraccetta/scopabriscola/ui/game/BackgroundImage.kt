package com.fraccetta.scopabriscola.ui.game

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fullscreen asset image with a dark overlay for legibility.
 * [assetPath] e.g. "backgrounds/arena_scopa.jpg"
 * [overlayAlpha] 0.0 = transparent, 1.0 = fully black
 */
@Composable
fun AssetBackground(
    assetPath: String,
    overlayAlpha: Float = 0.55f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(assetPath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(assetPath) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) { null }
        }
    }

    Box(modifier = modifier) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )
    }
}

/**
 * Picks a random image from assets/backgrounds/ matching [prefix] at composition time.
 * E.g. prefix="arena_" matches arena_1.jpg, arena_2.jpg, ...
 *      prefix="menu_"  matches menu_1.jpg, menu_2.jpg, ...
 * Falls back to plain dark color if none found.
 */
@Composable
fun RandomBackground(
    prefix: String = "arena_",
    overlayAlpha: Float = 0.55f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val assetPath = remember(prefix) {
        try {
            val files = context.assets.list("backgrounds") ?: emptyArray()
            val matches = files.filter {
                it.startsWith(prefix) && (it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".jpeg") || it.endsWith(".webp"))
            }
            if (matches.isNotEmpty()) "backgrounds/${matches.random()}" else null
        } catch (_: Exception) { null }
    }

    if (assetPath != null) {
        AssetBackground(assetPath = assetPath, overlayAlpha = overlayAlpha, modifier = modifier)
    } else {
        Box(modifier = modifier.background(Color(0xFF0D1F33)))
    }
}

/**
 * Arena background with optional BT sync.
 * Server: picks random arena, reports name via [onArenaSelected].
 * Client: uses [arenaName] received from server.
 * Solo: picks randomly, [onArenaSelected] unused.
 */
@Composable
fun RandomArenaBackground(
    arenaName: String? = null,
    overlayAlpha: Float = 0.55f,
    modifier: Modifier = Modifier,
    onArenaSelected: ((String) -> Unit)? = null
) {
    if (arenaName != null) {
        AssetBackground(assetPath = "backgrounds/$arenaName", overlayAlpha = overlayAlpha, modifier = modifier)
    } else {
        val context = LocalContext.current
        val picked = remember {
            try {
                val files = context.assets.list("backgrounds") ?: emptyArray()
                val matches = files.filter {
                    it.startsWith("arena_") && (it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".jpeg") || it.endsWith(".webp"))
                }
                if (matches.isNotEmpty()) matches.random() else null
            } catch (_: Exception) { null }
        }
        LaunchedEffect(picked) {
            if (picked != null) onArenaSelected?.invoke(picked)
        }
        if (picked != null) {
            AssetBackground(assetPath = "backgrounds/$picked", overlayAlpha = overlayAlpha, modifier = modifier)
        } else {
            Box(modifier = modifier.background(Color(0xFF0D1F33)))
        }
    }
}

@Composable
fun RandomMenuBackground(overlayAlpha: Float = 0.45f, modifier: Modifier = Modifier) =
    RandomBackground(prefix = "menu_", overlayAlpha = overlayAlpha, modifier = modifier)
