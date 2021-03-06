/*
 * Copyright 2018 Codepunk, LLC
 * Author(s): Scott Slater
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

package com.codepunk.doofenschmirtz.util

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.os.Build.VERSION_CODES.*
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * Compatibility class that allows for immersive-style modes on devices running Kit Kat or later.
 * For these modes (i.e. SYSTEM_UI_FLAG_IMMERSIVE, SYSTEM_UI_FLAG_IMMERSIVE_STICKY etc.),
 * earlier devices will simply enter SYSTEM_UI_FLAG_LOW_PROFILE mode.
 */
object ActivityCompat {

    // region Properties

    /**
     * Class that implements Activity compatibility methods.
     */
    private val IMPL: ActivityCompatImpl = when (Build.VERSION.SDK_INT) {
        in BASE until JELLY_BEAN -> BaseActivityCompatImpl()
        in JELLY_BEAN until KITKAT -> JellyBeanActivityCompatImpl()
        else -> KitKatActivityCompatImpl()
    }

    // endregion Properties

    // region Methods

    /**
     * Hides system UI elements based on the supplied [mode].
     */
    fun setFullScreenMode(activity: Activity, mode: FullScreenMode) {
        IMPL.setFullScreenMode(activity, mode)
    }

    fun ensureFullScreenMode(activity: Activity, visibility: Int, mode: FullScreenMode) {
        IMPL.ensureFullScreenMode(activity, visibility, mode)
    }

    // endregion Methods

    // region Nested/inner classes

    /**
     * An enum class that describes a fullscreen mode.
     */
    enum class FullScreenMode {

        /**
         * Do not implement a fullscreen mode.
         */
        NONE,

        /**
         * Implement a "lean back" full screen mode.
         */
        LEAN_BACK,

        /**
         * Implement an "immersive" full screen mode.
         */
        IMMERSIVE,

        /**
         * Implement a "sticky immersive" full screen mode.
         */
        STICKY_IMMERSIVE

    }

    /**
     * An interface that allows for api-independent fullscreen mode handling.
     */
    private interface ActivityCompatImpl {

        fun setFullScreenMode(activity: Activity, mode: FullScreenMode)

        fun ensureFullScreenMode(activity: Activity, visibility: Int, mode: FullScreenMode)
    }

    /**
     * A base implementation for hiding system UI elements.
     */
    private class BaseActivityCompatImpl : ActivityCompatImpl {

        /**
         * Hides system UI elements based on the supplied [mode].
         */
        override fun setFullScreenMode(activity: Activity, mode: FullScreenMode) {
            val show = (mode == FullScreenMode.NONE)
            showStatusBar(activity, show)
            showNavigation(activity, show)
            showActionBar(activity, show)
        }

        override fun ensureFullScreenMode(
            activity: Activity,
            visibility: Int,
            mode: FullScreenMode
        ) {
            // No op
        }

        private fun showStatusBar(activity: Activity, show: Boolean) {
            when (show) {
                true -> activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                false -> activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
        }

        /**
         * "Hides" the navigation bar. Although we CAN hide the navigation bar in Android
         * API prior to 19, it re-appears with any user interaction, which makes for a less-
         * than-stellar experience. To overcome that, we'll treat [View.SYSTEM_UI_FLAG_LOW_PROFILE]
         * as the default "hidden" state rather than [View.SYSTEM_UI_FLAG_HIDE_NAVIGATION].
         */
        private fun showNavigation(
            activity: Activity,
            show: Boolean,
            flagWhenHidden: Int = View.SYSTEM_UI_FLAG_LOW_PROFILE
        ) {
            activity.window.decorView.systemUiVisibility =
                when (show) {
                    true -> 0
                    false -> flagWhenHidden
                }
        }

        private fun showActionBar(activity: Activity, show: Boolean) {
            when (activity) {
                is AppCompatActivity -> when (show) {
                    true -> activity.supportActionBar?.show()
                    false -> activity.supportActionBar?.hide()
                }
                else -> when (show) {
                    true -> activity.actionBar?.show()
                    false -> activity.actionBar?.hide()
                }
            }
        }

    }

    @TargetApi(JELLY_BEAN)
    private class JellyBeanActivityCompatImpl : ActivityCompatImpl {

        /**
         * Since we aren't actually hiding the navigation bar (see below), we don't want to set
         * the SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION flag because some content would be
         * permanently hidden beneath the navigation bar.
         */
        private val contentUnderSystemBarsFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN /* or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION */

        /**
         * A set of flags to "hide" the system (status and navigation) bars. Although we CAN hide
         * the navigation bar in Android API prior to 19, it re-appears with any user interaction,
         * which makes for a less-than-stellar experience. To overcome that, we'll treat
         * [View.SYSTEM_UI_FLAG_LOW_PROFILE] as the default "hidden" state rather than
         * [View.SYSTEM_UI_FLAG_HIDE_NAVIGATION].
         */
        private val hideSystemBarsFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN

        /**
         * Hides system UI elements based on the supplied [mode].
         */
        override fun setFullScreenMode(activity: Activity, mode: FullScreenMode) {
            val flags = when (mode) {
                FullScreenMode.NONE -> contentUnderSystemBarsFlags
                FullScreenMode.LEAN_BACK -> contentUnderSystemBarsFlags or
                        hideSystemBarsFlags
                FullScreenMode.IMMERSIVE -> contentUnderSystemBarsFlags or
                        hideSystemBarsFlags
                FullScreenMode.STICKY_IMMERSIVE -> contentUnderSystemBarsFlags or
                        hideSystemBarsFlags
            }
            activity.window.decorView.systemUiVisibility = flags
        }

        override fun ensureFullScreenMode(
            activity: Activity,
            visibility: Int,
            mode: FullScreenMode
        ) {
            // No op
        }

    }

    /**
     * A KitKat implementation for hiding system UI elements.
     */
    @TargetApi(KITKAT)
    private class KitKatActivityCompatImpl : ActivityCompatImpl {

        private val contentUnderSystemBarsFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        private val hideSystemBarsFlags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN

        /**
         * Hides system UI elements based on the supplied [mode].
         */
        override fun setFullScreenMode(activity: Activity, mode: FullScreenMode) {
            val flags = when (mode) {
                FullScreenMode.NONE -> contentUnderSystemBarsFlags
                FullScreenMode.LEAN_BACK -> contentUnderSystemBarsFlags or
                        hideSystemBarsFlags
                FullScreenMode.IMMERSIVE -> contentUnderSystemBarsFlags or
                        hideSystemBarsFlags or
                        View.SYSTEM_UI_FLAG_IMMERSIVE
                FullScreenMode.STICKY_IMMERSIVE -> contentUnderSystemBarsFlags or
                        hideSystemBarsFlags or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            activity.window.decorView.systemUiVisibility = flags
        }

        /**
         * A workaround method for instances when visibility flags are cleared and not re-set after
         * configuration change.
         */
        override fun ensureFullScreenMode(
            activity: Activity,
            visibility: Int,
            mode: FullScreenMode
        ) {
            // This is a workaround because for some reason, sometimes after configuration change
            // the visibility flags get cleared and not re-set properly. This will force another
            // call to setFullScreenMode in that case.
            if (visibility == 0) {
                setFullScreenMode(activity, FullScreenMode.STICKY_IMMERSIVE)
            }
        }

    }

    // endregion Nested/inner classes

}
