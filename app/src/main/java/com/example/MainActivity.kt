package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.MainAppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HisabViewModel
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HisabViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        // STEP 2: Initialize ViewModel after App Check is wired
        viewModel = ViewModelProvider(this)[HisabViewModel::class.java]

        enableEdgeToEdge()

        // STEP 3: Initialize UI
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
