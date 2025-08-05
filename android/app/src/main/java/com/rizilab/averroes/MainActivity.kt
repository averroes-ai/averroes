package com.rizilab.averroes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rizilab.averroes.presentation.App
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

class MainActivity : ComponentActivity() {
    // Activity result sender for Mobile Wallet Adapter
    private val activityResultSender = ActivityResultSender(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(activityResultSender = activityResultSender)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
