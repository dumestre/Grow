package com.daime.grow.ui.util

import android.os.Build

object DeviceUtils {
    /**
     * O Android 15 (API 35+) introduziu mudanças no pipeline de renderização
     * que causam instabilidade (SIGSEGV) em versões alpha da biblioteca Haze.
     */
    val supportsBlurEffects: Boolean
        get() = Build.VERSION.SDK_INT < 35 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
