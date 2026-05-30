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

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.eltavine.duckdetector.features.deviceinfo.ui.model.DeviceInfoCardModel
import java.util.zip.CRC32
import kotlin.math.abs

/**
 * Invisible spread-spectrum digital watermark.
 *
 * Embeds device-identity data into the rendered UI by adding +1 to the
 * blue channel of selected pixels.  Pixel positions are determined by a
 * seeded PRNG, spreading each data bit across ~32 screen locations.
 *
 * ## Properties
 * - **Completely invisible** — single-bit blue-channel change.
 * - **Screenshot-safe** — pixel-perfect capture preserves every bit.
 * - **Compression-resistant** — 32× repetition per bit enables
 *   majority-vote recovery after JPEG/PNG compression.
 * - **No touch interception** — pure draw layer behind content.
 *
 * ## Payload (binary)
 * ```
 * [CRC32:4 bytes BE] [identity-text:UTF-8]
 * ```
 */
@Composable
fun DigitalWatermark(
    deviceInfoCard: DeviceInfoCardModel,
    modifier: Modifier = Modifier,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val watermarkBitmap: Bitmap? = remember(containerSize, deviceInfoCard) {
        if (containerSize.width < 16 || containerSize.height < 16) return@remember null
        buildWatermarkBitmap(containerSize.width, containerSize.height, deviceInfoCard)
    }

    Box(modifier = modifier.fillMaxSize().onSizeChanged { containerSize = it }) {
        if (watermarkBitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paint = android.graphics.Paint().apply {
                    xfermode = android.graphics.PorterDuffXfermode(
                        android.graphics.PorterDuff.Mode.ADD,
                    )
                }
                drawContext.canvas.nativeCanvas.drawBitmap(
                    watermarkBitmap!!, 0f, 0f, paint,
                )
            }
        }
    }
}

// ── Bitmap generation ─────────────────────────────────────────────

private const val PRNG_SEED = 0x4B1D57ADL
private const val SPREAD_FACTOR = 32
private const val MAX_SAMPLES_PER_BIT = 8

private fun buildWatermarkBitmap(w: Int, h: Int, card: DeviceInfoCardModel): Bitmap {
    val payload = encodePayload(card)
    val bits = spreadBits(payload, SPREAD_FACTOR)

    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val rng = SplitMix64(PRNG_SEED)

    for (bitIdx in bits.indices) {
        for (s in 0 until MAX_SAMPLES_PER_BIT) {
            val x = abs(rng.next().toInt()) % w
            val y = abs(rng.next().toInt()) % h
            if (bits[bitIdx]) {
                bmp.setPixel(x, y, 0x00000001)
            }
        }
    }
    return bmp
}

// ── Payload ───────────────────────────────────────────────────────

private fun encodePayload(card: DeviceInfoCardModel): ByteArray {
    val text = buildIdentityString(card)
    val textBytes = text.toByteArray(Charsets.UTF_8)
    val crc = CRC32().apply { update(textBytes) }.value.toInt()
    val payload = ByteArray(4 + textBytes.size)
    payload[0] = (crc shr 24).toByte()
    payload[1] = (crc shr 16).toByte()
    payload[2] = (crc shr 8).toByte()
    payload[3] = crc.toByte()
    textBytes.copyInto(payload, 4)
    return payload
}

private fun buildIdentityString(card: DeviceInfoCardModel): String {
    val map = mutableMapOf<String, String>()
    card.sections.forEach { s -> s.rows.forEach { r -> map[r.label] = r.value } }
    val fields = listOf(
        map["Brand"], map["Model"], map["Release"], map["SDK"],
        map["Fingerprint"], map["SOC Model"],
    ).mapNotNull { it?.takeIf { v -> v != "Unavailable" }?.take(48) }
    return if (fields.isEmpty()) "DuckDetector" else fields.joinToString("|")
}

private fun spreadBits(payload: ByteArray, factor: Int): BooleanArray {
    val raw = BooleanArray(payload.size * 8) { i ->
        val b = payload[i / 8].toInt() and 0xFF
        ((b shr (7 - (i % 8))) and 1) == 1
    }
    val spread = BooleanArray(raw.size * factor)
    for (i in raw.indices) {
        val v = raw[i]
        val base = i * factor
        for (r in 0 until factor) spread[base + r] = v
    }
    return spread
}

// ── PRNG (portable SplitMix64) ────────────────────────────────────

private class SplitMix64(seed: Long) {
    private var state: ULong = seed.toULong()
    fun next(): Long {
        state += 0x9E3779B97F4A7C15uL
        var z = state
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return (z xor (z shr 31)).toLong()
    }
}
