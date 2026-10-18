package online.malilab.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import online.malilab.app.data.Auth
import online.malilab.app.ui.AuthFlow
import online.malilab.app.ui.HomeShell
import online.malilab.app.ui.OnboardingScreen
import online.malilab.app.ui.theme.MaliLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        val prefs = getSharedPreferences("malilab", MODE_PRIVATE)
        setContent {
            MaliLabTheme {
                var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
                if (onboarded) {
                    if (Auth.signedIn) {
                        LaunchedEffect(Unit) { Auth.refresh() }
                        HomeShell()
                    } else {
                        AuthFlow()
                    }
                } else {
                    OnboardingScreen(onFinished = {
                        prefs.edit().putBoolean("onboarded", true).apply()
                        onboarded = true
                    })
                }
            }
        }
    }
}
