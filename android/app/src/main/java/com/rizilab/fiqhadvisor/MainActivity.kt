package com.rizilab.fiqhadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import com.rizilab.fiqhadvisor.navigation.FiqhAdvisorNavigation
import com.rizilab.fiqhadvisor.ui.theme.FiqhColors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FiqhAdvisorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FiqhColors.Background
                ) {
                    FiqhAdvisorNavigation()
                }
            }
        }
    }
}

@Composable
fun FiqhAdvisorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = FiqhColors.Primary,
            secondary = FiqhColors.Secondary,
            background = FiqhColors.Background,
            surface = FiqhColors.Surface,
            onPrimary = FiqhColors.OnPrimary,
            onSecondary = FiqhColors.OnSecondary,
            onBackground = FiqhColors.OnBackground,
            onSurface = FiqhColors.OnSurface,
            error = FiqhColors.Error,
            onError = FiqhColors.OnError
        ),
        content = content
    )
}