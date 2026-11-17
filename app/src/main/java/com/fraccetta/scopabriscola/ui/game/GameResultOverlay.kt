package com.fraccetta.scopabriscola.ui.game

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fraccetta.scopabriscola.ui.theme.CardCream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GameResultOverlay(
    isLocalWin: Boolean,
    isDraw: Boolean = false,
    titleText: String,
    buttonText: String,
    onContinue: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    // rememberSaveable survives rotation; pick filename once, reload bitmap after rotation
    var pickedFilename by rememberSaveable { mutableStateOf<String?>(null) }
    var resultImage by remember { mutableStateOf<ImageBitmap?>(null) }

    val resultKey = if (isDraw) "draw" else if (isLocalWin) "win" else "lose"
    LaunchedEffect(resultKey) {
        val prefix = "${resultKey}_"
        if (pickedFilename == null || !pickedFilename!!.startsWith(prefix)) {
            val files = withContext(Dispatchers.IO) {
                val all = context.assets.list("result")
                    ?.filter { f -> f.endsWith(".jpg") || f.endsWith(".png") || f.endsWith(".webp") }
                    ?: emptyList()
                // Try draw_ first; fall back to lose_ if no draw images exist
                val preferred = all.filter { it.startsWith(prefix) }
                preferred.ifEmpty {
                    if (isDraw) all.filter { it.startsWith("lose_") } else emptyList()
                }
            }
            pickedFilename = if (files.isNotEmpty()) files.random() else null
        }
        val filename = pickedFilename ?: return@LaunchedEffect
        resultImage = withContext(Dispatchers.IO) {
            try {
                context.assets.open("result/$filename").use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            } catch (e: Exception) { null }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-screen image background
            if (resultImage != null) {
                Image(
                    bitmap = resultImage!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D1F33))
                )
            }

            // Gradient scrim: darkens top and bottom so text is readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // Title pinned at top
            Text(
                text = titleText,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardCream,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 6f
                    )
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 32.dp)
                    .padding(horizontal = 24.dp)
            )

            // Scores + button pinned at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.55f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    content()
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = buttonText, color = CardCream, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
