/*
 * Copyright 2026 Duck Apps Contributor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eltavine.duckdetector.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.eltavine.duckdetector.features.deviceinfo.ui.model.DeviceInfoCardModel

/**
 * A subtle device-identity watermark drawn behind all content.
 *
 * Displays key device facts (brand, model, Android version, SDK) as repeated
 * text at very low opacity.  The watermark is fixed in place (does not scroll)
 * and does not intercept touch events, so normal user interaction is
 * unaffected.
 *
 * @param deviceInfoCard  Device info model providing the facts to display.
 * @param alpha           Text opacity (0..1).  Default 0.04 — barely visible.
 * @param modifier        Standard Compose modifier.
 */
@Composable
fun DeviceInfoWatermark(
    deviceInfoCard: DeviceInfoCardModel,
    alpha: Float = 0.04f,
    modifier: Modifier = Modifier,
) {
    val text = remember(deviceInfoCard) { buildWatermarkText(deviceInfoCard) }
    if (text.isBlank()) return

    val density = LocalDensity.current
    val fontSizePx = with(density) { 10.sp.toPx() }
    val rowHeightPx = with(density) { 64.sp.toPx() }
    val skewAngle = -22f

    Canvas(modifier = modifier.fillMaxSize()) {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                0, 0, 0,
            )
            textSize = fontSizePx
            isAntiAlias = true
            textSkewX = Math.tan(Math.toRadians(skewAngle.toDouble())).toFloat()
        }

        val textWidth = paint.measureText(text)
        val repetitions = if (textWidth > 0) (size.width / textWidth).toInt() + 2 else 1

        var y = -rowHeightPx
        while (y < size.height + rowHeightPx) {
            val offsetX = ((y / rowHeightPx).toInt() % 3) * (textWidth / 3f)
            for (i in 0 until repetitions) {
                val x = i * textWidth + offsetX
                drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
            }
            y += rowHeightPx
        }
    }
}

/**
 * Build a concise watermark string from device identity fields.
 *
 * Format: "Brand Model  ·  Android 14  ·  SDK 34  ·  fingerprint…"
 */
private fun buildWatermarkText(card: DeviceInfoCardModel): String {
    val map = mutableMapOf<String, String>()
    card.sections.forEach { section ->
        section.rows.forEach { row ->
            map[row.label] = row.value
        }
    }

    val brand = map["Brand"]?.takeIf { it != "Unavailable" } ?: ""
    val model = map["Model"]?.takeIf { it != "Unavailable" } ?: ""
    val release = map["Release"]?.takeIf { it != "Unavailable" } ?: ""
    val sdk = map["SDK"]?.takeIf { it != "Unavailable" } ?: ""
    val fingerprint = map["Fingerprint"]?.takeIf { it != "Unavailable" }
        ?.let { if (it.length > 40) it.take(40) + "…" else it }
        ?: ""
    val soc = map["SOC Model"]?.takeIf { it != "Unavailable" }
        ?: map["Board Platform"]?.takeIf { it != "Unavailable" } ?: ""

    return buildString {
        if (brand.isNotEmpty() || model.isNotEmpty()) {
            append("$brand $model".trim())
        }
        if (release.isNotEmpty()) {
            if (isNotEmpty()) append("  ·  ")
            append("Android $release")
        }
        if (sdk.isNotEmpty()) {
            if (isNotEmpty()) append("  ·  ")
            append("SDK $sdk")
        }
        if (soc.isNotEmpty()) {
            if (isNotEmpty()) append("  ·  ")
            append(soc)
        }
        if (fingerprint.isNotEmpty()) {
            if (isNotEmpty()) append("  ·  ")
            append(fingerprint)
        }
        if (isEmpty()) append("Duck Detector")
    }
}
