package com.ksupatcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ksupatcher.ui.KsuPatcherNavGraph
import com.ksupatcher.ui.theme.KsuPatcherTheme
import com.ksupatcher.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KsuPatcherTheme(darkTheme = isSystemInDarkTheme()) {
                val mainViewModel: MainViewModel = viewModel()
                KsuPatcherNavGraph(viewModel = mainViewModel)
            }
        }
    }
}
