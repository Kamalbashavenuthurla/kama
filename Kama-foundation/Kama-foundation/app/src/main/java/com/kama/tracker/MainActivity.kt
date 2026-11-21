package com.kama.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kama.tracker.ui.KamaRoot
import com.kama.tracker.ui.KamaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as KamaApplication).repository
        setContent {
            val vm: KamaViewModel = viewModel(
                factory = viewModelFactory { initializer { KamaViewModel(repo) } }
            )
            KamaRoot(vm)
        }
    }
}
