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

package com.eltavine.duckdetector.features.dashboard.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.eltavine.duckdetector.core.ui.components.DetectorAutoExpansionDirective
import com.eltavine.duckdetector.core.ui.components.LocalDetectorAutoExpansionDirective
import com.eltavine.duckdetector.core.ui.components.MetricChip
import com.eltavine.duckdetector.core.ui.components.StatusBadge
import com.eltavine.duckdetector.core.ui.components.WrapSafeText
import com.eltavine.duckdetector.core.ui.components.digitalWatermark
import com.eltavine.duckdetector.core.ui.model.MetricChipModel
import com.eltavine.duckdetector.features.bootloader.ui.card.BootloaderDetectorCard
import com.eltavine.duckdetector.features.customrom.ui.card.CustomRomDetectorCard
import com.eltavine.duckdetector.features.dashboard.ui.model.DashboardDetectorCardEntry
import com.eltavine.duckdetector.features.dashboard.ui.model.DashboardUiState
import com.eltavine.duckdetector.features.deviceinfo.ui.card.DeviceInfoCard
import com.eltavine.duckdetector.features.deviceinfo.ui.model.DeviceInfoCardModel
import com.eltavine.duckdetector.features.dangerousapps.ui.card.DangerousAppsDetectorCard
import com.eltavine.duckdetector.features.kernelcheck.ui.card.KernelCheckDetectorCard
import com.eltavine.duckdetector.features.lsposed.ui.card.LSPosedDetectorCard
import com.eltavine.duckdetector.features.memory.ui.card.MemoryDetectorCard
import com.eltavine.duckdetector.features.mount.ui.card.MountDetectorCard
import com.eltavine.duckdetector.features.nativeroot.ui.card.NativeRootDetectorCard
import com.eltavine.duckdetector.features.playintegrityfix.ui.card.PlayIntegrityFixDetectorCard
import com.eltavine.duckdetector.features.selinux.ui.card.SelinuxDetectorCard
import com.eltavine.duckdetector.features.su.ui.card.SuDetectorCard
import com.eltavine.duckdetector.features.systemproperties.ui.card.SystemPropertiesDetectorCard
import com.eltavine.duckdetector.features.tee.ui.card.TeeDetectorCard
import com.eltavine.duckdetector.features.tee.ui.model.TeeFooterActionId
import com.eltavine.duckdetector.features.virtualization.ui.card.VirtualizationDetectorCard
import com.eltavine.duckdetector.features.zygisk.ui.card.ZygiskDetectorCard
import com.eltavine.duckdetector.ui.theme.DuckDetectorTheme
import com.eltavine.duckdetector.ui.theme.ShapeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val EXPORT_JPEG_QUALITY = 90
private val EXPORT_RELATIVE_DIR = "${Environment.DIRECTORY_PICTURES}/DuckDetector"

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    showTeeDetailsDialog: Boolean,
    showTeeCertificatesDialog: Boolean,
    onTeeExpandedChange: (Boolean) -> Unit,
    onTeeFooterAction: (TeeFooterActionId) -> Unit,
    onDismissTeeDetails: () -> Unit,
    onDismissTeeCertificates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val darkTheme = isSystemInDarkTheme()
    var exporting by rememberSaveable { mutableStateOf(false) }

    DashboardScreenContent(
        uiState = uiState,
        showTeeDetailsDialog = showTeeDetailsDialog,
        showTeeCertificatesDialog = showTeeCertificatesDialog,
        onTeeExpandedChange = onTeeExpandedChange,
        onTeeFooterAction = onTeeFooterAction,
        onDismissTeeDetails = onDismissTeeDetails,
        onDismissTeeCertificates = onDismissTeeCertificates,
        modifier = modifier.fillMaxSize(),
        scrollable = true,
        includeSystemBarsPadding = true,
        includeVisibleWatermark = false,
        showExportButton = true,
        exporting = exporting,
        onExportClick = {
            if (!exporting) {
                scope.launch {
                    exporting = true
                    val result = runCatching {
                        exportDashboardLongScreenshot(
                            context = context,
                            anchorView = view,
                            uiState = uiState,
                            darkTheme = darkTheme,
                        )
                    }
                    exporting = false
                    result.onSuccess {
                        Toast.makeText(
                            context,
                            "Long screenshot saved to Pictures/DuckDetector",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }.onFailure { throwable ->
                        val message = throwable.message ?: "unknown error"
                        Toast.makeText(
                            context,
                            "Export failed: $message",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        },
    )
}

@Composable
private fun DashboardScreenContent(
    uiState: DashboardUiState,
    showTeeDetailsDialog: Boolean,
    showTeeCertificatesDialog: Boolean,
    onTeeExpandedChange: (Boolean) -> Unit,
    onTeeFooterAction: (TeeFooterActionId) -> Unit,
    onDismissTeeDetails: () -> Unit,
    onDismissTeeCertificates: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean,
    includeSystemBarsPadding: Boolean,
    includeVisibleWatermark: Boolean,
    showExportButton: Boolean,
    exporting: Boolean,
    onExportClick: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .digitalWatermark(uiState.deviceInfoCard),
    ) {
        val contentModifier = Modifier
            .align(Alignment.TopCenter)
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .then(
                if (includeSystemBarsPadding) {
                    Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .then(
                if (scrollable) {
                    Modifier.verticalScroll(scrollState)
                } else {
                    Modifier
                },
            )

        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DashboardHeaderCard(
                isLoading = uiState.isLoading,
                showExportButton = showExportButton,
                exporting = exporting,
                onExportClick = onExportClick,
            )
            DashboardOverviewCard(uiState = uiState)
            DashboardTopFindingsCard(uiState = uiState)
            DashboardDetectorCardsCard(
                uiState = uiState,
                showTeeDetailsDialog = showTeeDetailsDialog,
                showTeeCertificatesDialog = showTeeCertificatesDialog,
                onTeeExpandedChange = onTeeExpandedChange,
                onTeeFooterAction = onTeeFooterAction,
                onDismissTeeDetails = onDismissTeeDetails,
                onDismissTeeCertificates = onDismissTeeCertificates,
            )
            DashboardDeviceInfoCard(uiState.deviceInfoCard)
            if (scrollable) {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        if (includeVisibleWatermark) {
            ExportDeviceInfoWatermarkOverlay(
                deviceInfoCard = uiState.deviceInfoCard,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DashboardHeaderCard(
    isLoading: Boolean,
    showExportButton: Boolean,
    exporting: Boolean,
    onExportClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.CornerExtraLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WrapSafeText(
                text = "Detection results",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            WrapSafeText(
                text = if (isLoading) {
                    "Detector cards are still collecting local evidence. Export captures the current state as a long screenshot."
                } else {
                    "Export the current dashboard as a long screenshot with a visible device watermark and embedded device fingerprint."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (showExportButton) {
                FilledTonalButton(
                    onClick = onExportClick,
                    enabled = !exporting,
                ) {
                    if (exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    WrapSafeText(
                        text = if (exporting) "Exporting long screenshot..." else "Export long screenshot",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardOverviewCard(
    uiState: DashboardUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.CornerExtraLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = ShapeTokens.CornerLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                StatusBadge(status = uiState.overview.status)
            }

            WrapSafeText(
                text = uiState.overview.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            WrapSafeText(
                text = uiState.overview.headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            WrapSafeText(
                text = uiState.overview.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.overview.metrics.forEach { metric ->
                    MetricChip(
                        chip = MetricChipModel(
                            label = metric.label,
                            value = metric.value,
                            status = metric.status,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTopFindingsCard(
    uiState: DashboardUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.CornerExtraLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ReportProblem,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            WrapSafeText(
                text = "Top findings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            uiState.topFindings.forEachIndexed { index, finding ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(status = finding.status)
                    WrapSafeText(
                        text = finding.detectorTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    WrapSafeText(
                        text = finding.headline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    WrapSafeText(
                        text = finding.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardDetectorCardsCard(
    uiState: DashboardUiState,
    showTeeDetailsDialog: Boolean,
    showTeeCertificatesDialog: Boolean,
    onTeeExpandedChange: (Boolean) -> Unit,
    onTeeFooterAction: (TeeFooterActionId) -> Unit,
    onDismissTeeDetails: () -> Unit,
    onDismissTeeCertificates: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeTokens.CornerExtraLargeIncreased,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                WrapSafeText(
                    text = "Detector cards",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                WrapSafeText(
                    text = "The long screenshot export automatically expands detector cards so the image keeps the detailed evidence, not only the collapsed overview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        uiState.detectorCards.forEach { entry ->
            DashboardDetectorCard(
                entry = entry,
                showTeeDetailsDialog = showTeeDetailsDialog,
                showTeeCertificatesDialog = showTeeCertificatesDialog,
                onTeeExpandedChange = onTeeExpandedChange,
                onTeeFooterAction = onTeeFooterAction,
                onDismissTeeDetails = onDismissTeeDetails,
                onDismissTeeCertificates = onDismissTeeCertificates,
            )
        }
    }
}

@Composable
private fun DashboardDeviceInfoCard(
    deviceInfoCard: DeviceInfoCardModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeTokens.CornerExtraLargeIncreased,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                WrapSafeText(
                    text = "Device information",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                WrapSafeText(
                    text = "The export adds a transparent visible watermark derived from this section and keeps the existing invisible device watermark in the bitmap data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DeviceInfoCard(model = deviceInfoCard)
    }
}

@Composable
private fun DashboardDetectorCard(
    entry: DashboardDetectorCardEntry,
    showTeeDetailsDialog: Boolean,
    showTeeCertificatesDialog: Boolean,
    onTeeExpandedChange: (Boolean) -> Unit,
    onTeeFooterAction: (TeeFooterActionId) -> Unit,
    onDismissTeeDetails: () -> Unit,
    onDismissTeeCertificates: () -> Unit,
) {
    when (entry) {
        is DashboardDetectorCardEntry.Bootloader -> BootloaderDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.CustomRom -> CustomRomDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.DangerousApps -> DangerousAppsDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.KernelCheck -> KernelCheckDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.LSPosed -> LSPosedDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.Memory -> MemoryDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.Mount -> MountDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.NativeRoot -> NativeRootDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.PlayIntegrityFix -> PlayIntegrityFixDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.Selinux -> SelinuxDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.Su -> SuDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.SystemProperties -> {
            SystemPropertiesDetectorCard(model = entry.model)
        }

        is DashboardDetectorCardEntry.Tee -> {
            TeeDetectorCard(
                model = entry.model,
                showDetailsDialog = showTeeDetailsDialog,
                showCertificatesDialog = showTeeCertificatesDialog,
                onExpandedChange = onTeeExpandedChange,
                onFooterAction = onTeeFooterAction,
                onDismissDetails = onDismissTeeDetails,
                onDismissCertificates = onDismissTeeCertificates,
            )
        }

        is DashboardDetectorCardEntry.Virtualization -> VirtualizationDetectorCard(model = entry.model)
        is DashboardDetectorCardEntry.Zygisk -> ZygiskDetectorCard(model = entry.model)
    }
}

@Composable
private fun ExportDeviceInfoWatermarkOverlay(
    deviceInfoCard: DeviceInfoCardModel,
    modifier: Modifier = Modifier,
    alpha: Float = 0.085f,
    textSizeSp: Float = 12f,
    spacingDp: Float = 220f,
    rotationDegrees: Float = -25f,
) {
    val density = LocalDensity.current
    val isDarkTheme = isSystemInDarkTheme()
    val textSizePx = with(density) { textSizeSp.sp.toPx() }
    val spacingPx = with(density) { spacingDp.dp.toPx() }
    val watermarkLines = remember(deviceInfoCard) {
        buildVisibleWatermarkLines(deviceInfoCard)
    }

    Canvas(modifier = modifier) {
        val colorValue = if (isDarkTheme) 255 else 0
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (alpha * 255f).roundToInt(),
                colorValue,
                colorValue,
                colorValue,
            )
            textSize = textSizePx
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val lineHeight = paint.fontSpacing
        val widestLine = watermarkLines.maxOfOrNull(paint::measureText) ?: 0f
        val safeHSpacing = maxOf(spacingPx, widestLine * 1.12f)
        val safeVSpacing = maxOf(spacingPx * 0.48f, lineHeight * (watermarkLines.size + 1.2f))
        val diagonal = sqrt(size.width * size.width + size.height * size.height)
        val startX = -diagonal / 2f
        val endX = size.width + diagonal / 2f
        val endY = size.height + diagonal / 2f

        rotate(
            degrees = rotationDegrees,
            pivot = Offset(size.width / 2f, size.height / 2f),
        ) {
            var y = -diagonal / 2f + lineHeight
            while (y < endY) {
                var x = startX
                while (x < endX) {
                    watermarkLines.forEachIndexed { index, line ->
                        drawContext.canvas.nativeCanvas.drawText(
                            line,
                            x,
                            y + index * lineHeight,
                            paint,
                        )
                    }
                    x += safeHSpacing
                }
                y += safeVSpacing
            }
        }
    }
}

private fun buildVisibleWatermarkLines(
    deviceInfoCard: DeviceInfoCardModel,
): List<String> {
    val facts = mutableMapOf<String, String>()
    deviceInfoCard.headerFacts.forEach { fact ->
        facts[fact.label] = fact.value
    }
    deviceInfoCard.sections.forEach { section ->
        section.rows.forEach { row ->
            facts[row.label] = row.value
        }
    }

    val brand = facts["Brand"].orFallback()
    val model = facts["Model"].orFallback()
    val device = facts["Device"].orFallback()
    val release = facts["Release"].orFallback()
    val sdk = facts["SDK"].orFallback()
    val securityPatch = facts["Security patch"].orFallback()
    val fingerprint = truncateMiddle(facts["Fingerprint"].orFallback(), 44)

    return listOf(
        "Duck Detector export",
        "Brand $brand | Model $model | Device $device",
        "Android $release | SDK $sdk | Patch $securityPatch",
        "Fingerprint $fingerprint",
    )
}

private fun String?.orFallback(): String {
    return this?.takeIf { it.isNotBlank() && it != "Unavailable" } ?: "Unknown"
}

private fun truncateMiddle(
    value: String,
    maxLength: Int,
): String {
    if (value.length <= maxLength) {
        return value
    }
    val edge = ((maxLength - 1) / 2).coerceAtLeast(4)
    return "${value.take(edge)}…${value.takeLast(edge)}"
}

private suspend fun exportDashboardLongScreenshot(
    context: Context,
    anchorView: View,
    uiState: DashboardUiState,
    darkTheme: Boolean,
): Uri {
    return withContext(Dispatchers.Main) {
        val host = anchorView.rootView as? ViewGroup
            ?: error("Unable to access root view for export")
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.coerceAtLeast(1)
        val density = displayMetrics.density
        val contentMaxWidthPx = (720.dp.value * density).roundToInt()
        val width = minOf(screenWidth, contentMaxWidthPx).coerceAtLeast(1)

        val container = FrameLayout(context).apply {
            alpha = 0f
            layoutParams = ViewGroup.LayoutParams(
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        val composeView = ComposeView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                DuckDetectorTheme(
                    darkTheme = darkTheme,
                    dynamicColor = false,
                ) {
                    CompositionLocalProvider(
                        LocalDetectorAutoExpansionDirective provides DetectorAutoExpansionDirective(
                            titles = uiState.detectorCards.mapTo(mutableSetOf()) { entry ->
                                when (entry) {
                                    is DashboardDetectorCardEntry.Bootloader -> entry.model.title
                                    is DashboardDetectorCardEntry.CustomRom -> entry.model.title
                                    is DashboardDetectorCardEntry.DangerousApps -> entry.model.title
                                    is DashboardDetectorCardEntry.KernelCheck -> entry.model.title
                                    is DashboardDetectorCardEntry.LSPosed -> entry.model.title
                                    is DashboardDetectorCardEntry.Memory -> entry.model.title
                                    is DashboardDetectorCardEntry.Mount -> entry.model.title
                                    is DashboardDetectorCardEntry.NativeRoot -> entry.model.title
                                    is DashboardDetectorCardEntry.PlayIntegrityFix -> entry.model.title
                                    is DashboardDetectorCardEntry.Selinux -> entry.model.title
                                    is DashboardDetectorCardEntry.Su -> entry.model.title
                                    is DashboardDetectorCardEntry.SystemProperties -> entry.model.title
                                    is DashboardDetectorCardEntry.Tee -> entry.model.title
                                    is DashboardDetectorCardEntry.Virtualization -> entry.model.title
                                    is DashboardDetectorCardEntry.Zygisk -> entry.model.title
                                }
                            },
                            disableAnimation = true,
                        ),
                    ) {
                        DashboardScreenContent(
                            uiState = uiState,
                            showTeeDetailsDialog = false,
                            showTeeCertificatesDialog = false,
                            onTeeExpandedChange = {},
                            onTeeFooterAction = {},
                            onDismissTeeDetails = {},
                            onDismissTeeCertificates = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            scrollable = false,
                            includeSystemBarsPadding = false,
                            includeVisibleWatermark = true,
                            showExportButton = false,
                            exporting = false,
                            onExportClick = {},
                        )
                    }
                }
            }
        }

        container.addView(composeView)
        host.addView(container)
        val bitmap = try {
            delay(80L)
            val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            composeView.measure(widthSpec, heightSpec)
            val measuredHeight = composeView.measuredHeight.coerceAtLeast(1)
            composeView.layout(0, 0, width, measuredHeight)
            renderViewToBitmap(
                view = composeView,
                width = width,
                height = measuredHeight,
            )
        } finally {
            host.removeView(container)
        }

        val uri = saveBitmapToGallery(
            context = context,
            bitmap = bitmap,
        )
        bitmap.recycle()
        uri
    }
}

private fun renderViewToBitmap(
    view: View,
    width: Int,
    height: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    Log.d(
        "DuckExport",
        "rendered bitmap: ${bitmap.width}x${bitmap.height}, " +
            "byteCount=${bitmap.byteCount}, " +
            "allocationByteCount=${bitmap.allocationByteCount}, " +
            "config=${bitmap.config}",
    )
    return bitmap
}

private suspend fun saveBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
): Uri = withContext(Dispatchers.IO) {
    val fileName = "duckdetector-scan-${
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    }.jpg"
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, EXPORT_RELATIVE_DIR)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Failed to create MediaStore record")

    try {
        resolver.openOutputStream(uri)?.use { output ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, EXPORT_JPEG_QUALITY, output)
            Log.d(
                "DuckExport",
                "compress JPEG q$EXPORT_JPEG_QUALITY: result=$compressed, " +
                    "src=${bitmap.width}x${bitmap.height} (${bitmap.byteCount}B), " +
                    "isRecycled=${bitmap.isRecycled}",
            )
            if (!compressed) {
                error("JPEG compression failed")
            }
        } ?: error("Failed to open output stream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publishValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, publishValues, null, null)
        }
        uri
    } catch (throwable: Throwable) {
        resolver.delete(uri, null, null)
        throw throwable
    }
}
