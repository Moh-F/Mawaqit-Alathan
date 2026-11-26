package com.mohdev.alathan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohdev.alathan.core.Settings

/** Diagonal corner ribbon at the bottom-end corner with the version number (standard in every app). */
@Composable
fun VersionRibbon(label: String, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val measurer = rememberTextMeasurer()
    val bg = Settings.color("accentColor").copy(alpha = 0.92f)
    val fg = if (bg.luminance() > 0.5f) Color.Black else Color.White
    val layout = measurer.measure(AnnotatedString(label), TextStyle(color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold))
    Canvas(modifier.size(110.dp).clipToBounds()) {
        val cx = if (rtl) 0f else size.width
        val cy = size.height
        translate(cx, cy) {
            rotate(if (rtl) 45f else -45f, pivot = Offset.Zero) {
                val h = 22.dp.toPx()
                val mid = -30.dp.toPx()
                drawRect(bg, topLeft = Offset(-90.dp.toPx(), mid - h / 2), size = Size(180.dp.toPx(), h))
                drawText(layout, topLeft = Offset(-layout.size.width / 2f, mid - layout.size.height / 2f))
            }
        }
    }
}
