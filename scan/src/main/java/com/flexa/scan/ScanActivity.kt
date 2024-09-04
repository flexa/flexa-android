package com.flexa.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.flexa.core.theme.FlexaTheme

class ScanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlexaTheme {
                ScanScreen(
                    modifier = Modifier.fillMaxSize(),
                    close = { finish() }
                )
            }
        }
    }
}
