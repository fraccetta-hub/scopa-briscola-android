package com.fraccetta.scopabriscola

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fraccetta.scopabriscola.ui.navigation.AppNavGraph
import com.fraccetta.scopabriscola.ui.theme.ScopaBriscolaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScopaBriscolaTheme {
                AppNavGraph()
            }
        }
    }
}
