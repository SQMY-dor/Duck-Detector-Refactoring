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

package com.eltavine.duckdetector.features.dashboard.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner

class DashboardScreenshotCapture(
    private val context: Context,
    private vararg val lifecycleViews: View,
) {

    fun capture(
        content: @Composable () -> Unit,
        callback: (Bitmap?) -> Unit,
    ) {
        val parent = FrameLayout(context)
        val composeView = ComposeView(context)

        transferLifecycle(composeView)
        parent.addView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        composeView.setContent {
            content()
        }

        captureWhenReady(parent, retries = 15, callback)
    }

    private fun transferLifecycle(composeView: ComposeView) {
        for (view in lifecycleViews) {
            val lifecycleOwner = view.findViewTreeLifecycleOwner()
            if (lifecycleOwner != null) {
                ViewTreeLifecycleOwner.set(composeView, lifecycleOwner)
                break
            }
        }
        for (view in lifecycleViews) {
            val registryOwner = view.findViewTreeSavedStateRegistryOwner()
            if (registryOwner != null) {
                ViewTreeSavedStateRegistryOwner.set(composeView, registryOwner)
                break
            }
        }
    }

    private fun captureWhenReady(
        parent: FrameLayout,
        retries: Int,
        callback: (Bitmap?) -> Unit,
    ) {
        if (retries <= 0) {
            callback(null)
            return
        }

        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            context.resources.displayMetrics.widthPixels,
            View.MeasureSpec.EXACTLY,
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        parent.measure(widthSpec, heightSpec)

        val measuredWidth = parent.measuredWidth
        val measuredHeight = parent.measuredHeight

        if (measuredWidth > 0 && measuredHeight > 0) {
            parent.layout(0, 0, measuredWidth, measuredHeight)
            val bitmap = Bitmap.createBitmap(
                measuredWidth,
                measuredHeight,
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            parent.draw(canvas)
            callback(bitmap)
        } else {
            parent.postDelayed({
                captureWhenReady(parent, retries - 1, callback)
            }, 100)
        }
    }
}
