package io.github.hemitcanj.zybedoo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hemitcanj.zybedoo.ui.AppViewModel
import io.github.hemitcanj.zybedoo.ui.RootScreen
import io.github.hemitcanj.zybedoo.ui.theme.ZBedooTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZBedooTheme {
                val vm: AppViewModel = viewModel()
                RootScreen(vm)
            }
        }
    }
}
