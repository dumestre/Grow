package com.daime.grow.ui.screen.lock

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.daime.grow.ui.theme.GrowTheme
import com.daime.grow.ui.viewmodel.LockUiState

@Preview(showBackground = true)
@Composable
private fun LockPreview() {
    GrowTheme {
        LockScreen(
            state = LockUiState(showLockScreen = true, pinEnabled = true, biometricEnabled = true),
            onPinChange = {},
            onUnlockWithPin = {},
            onTryBiometric = {}
        )
    }
}

