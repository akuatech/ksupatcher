package org.akuatech.ksupatcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import org.akuatech.ksupatcher.ui.KsuPatcherNavGraph
import org.akuatech.ksupatcher.ui.theme.KsuPatcherTheme
import org.akuatech.ksupatcher.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KsuPatcherTheme(darkTheme = isSystemInDarkTheme()) {
                val mainViewModel: MainViewModel = viewModel()
                KsuPatcherNavGraph(viewModel = mainViewModel)
            }
        }
    }
}
