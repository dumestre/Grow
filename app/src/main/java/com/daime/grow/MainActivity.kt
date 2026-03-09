package com.daime.grow

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.daime.grow.ui.GrowRoot
import com.daime.grow.ui.theme.GrowTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ativa o modo Edge-to-Edge corretamente para Android 15+
        // Sem passar SystemBarStyle manual para evitar uso de APIs descontinuadas
        // O Compose e o Material3 gerenciam as cores das barras automaticamente
        enableEdgeToEdge()

        val container = (application as GrowApplication).appContainer
        setContent {
            GrowTheme {
                GrowRoot(container)
            }
        }
    }
}
