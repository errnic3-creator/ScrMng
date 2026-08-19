package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppIconView(
    packageName: String,
    appName: String = "",
    category: String = "Other",
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        loadAppIconBitmap(context, packageName)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "$appName icon",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.25f))
        )
    } else {
        // Fallback Category / Letter Badge
        val bgColor = remember(packageName) {
            val colors = listOf(
                Color(0xFF4F46E5), Color(0xFF0D9488), Color(0xFFD97706),
                Color(0xFFE11D48), Color(0xFF7C3AED), Color(0xFF2563EB)
            )
            val index = Math.abs(packageName.hashCode()) % colors.size
            colors[index]
        }

        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.25f))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            val displayName = if (appName.isNotBlank()) appName else packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
            Text(
                text = initial,
                color = Color.White,
                fontSize = (size.value * 0.45f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun loadAppIconBitmap(context: Context, packageName: String): Bitmap? {
    return try {
        val pm = context.packageManager
        val drawable = pm.getApplicationIcon(packageName)
        drawableToBitmap(drawable)
    } catch (e: Exception) {
        null
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
