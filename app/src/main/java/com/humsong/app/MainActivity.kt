package com.humsong.app

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.humsong.app.ui.FitnessApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = remember { getSharedPreferences("fitness_settings", MODE_PRIVATE) }
            var themeKey by remember {
                mutableStateOf(preferences.getString("theme_key", "teal") ?: "teal")
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            val colorScheme = fitnessColorScheme(themeKey)
            val typography = Typography().let {
                it.copy(
                    headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    titleMedium = it.titleMedium.copy(fontWeight = FontWeight.Bold),
                    titleSmall = it.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
                    FitnessApp(
                        themeKey = themeKey,
                        onThemeChange = { key ->
                            themeKey = key
                            preferences.edit().putString("theme_key", key).apply()
                        }
                    )
                }
            }
        }
    }
}

private fun fitnessColorScheme(themeKey: String) = when (themeKey) {
    "gundam" -> lightColorScheme(
        primary = Color(0xFF1E4FD7),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDDE6FF),
        onPrimaryContainer = Color(0xFF061B52),
        secondary = Color(0xFFE51F2F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDAD8),
        onSecondaryContainer = Color(0xFF410006),
        tertiary = Color(0xFFF3B51B),
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE8ECF5),
        onSurface = Color(0xFF111827),
        onSurfaceVariant = Color(0xFF4B5563),
        outline = Color(0xFF8A94A6)
    )
    "violet" -> darkColorScheme(
        primary = Color(0xFFC4B5FD),
        onPrimary = Color(0xFF21113F),
        primaryContainer = Color(0xFF3B236F),
        onPrimaryContainer = Color(0xFFF1EAFF),
        secondary = Color(0xFF7DD3FC),
        onSecondary = Color(0xFF052235),
        secondaryContainer = Color(0xFF0B3B55),
        tertiary = Color(0xFFFFB4AB),
        background = Color(0xFF101017),
        surface = Color(0xFF1A1822),
        surfaceVariant = Color(0xFF292536),
        onSurface = Color(0xFFF7F3FF),
        onSurfaceVariant = Color(0xFFCBC4D8),
        outline = Color(0xFF797184)
    )
    "blue" -> darkColorScheme(
        primary = Color(0xFF93C5FD),
        onPrimary = Color(0xFF061A33),
        primaryContainer = Color(0xFF123A63),
        onPrimaryContainer = Color(0xFFD8EAFF),
        secondary = Color(0xFFF9A8D4),
        onSecondary = Color(0xFF341025),
        secondaryContainer = Color(0xFF5C2440),
        tertiary = Color(0xFF86EFAC),
        background = Color(0xFF0B111B),
        surface = Color(0xFF151C28),
        surfaceVariant = Color(0xFF202A3A),
        onSurface = Color(0xFFF2F6FB),
        onSurfaceVariant = Color(0xFFC3CDDC),
        outline = Color(0xFF6F7B8D)
    )
    "rose" -> darkColorScheme(
        primary = Color(0xFFFFA6C1),
        onPrimary = Color(0xFF3A0618),
        primaryContainer = Color(0xFF6F1D3B),
        onPrimaryContainer = Color(0xFFFFD9E5),
        secondary = Color(0xFF67E8F9),
        onSecondary = Color(0xFF062A30),
        secondaryContainer = Color(0xFF164F57),
        tertiary = Color(0xFFFDE68A),
        background = Color(0xFF140D12),
        surface = Color(0xFF211820),
        surfaceVariant = Color(0xFF332633),
        onSurface = Color(0xFFFFF4F8),
        onSurfaceVariant = Color(0xFFD6C2CC),
        outline = Color(0xFF8A7380)
    )
    "amber" -> darkColorScheme(
        primary = Color(0xFFFCD34D),
        onPrimary = Color(0xFF2E2100),
        primaryContainer = Color(0xFF5A4305),
        onPrimaryContainer = Color(0xFFFFF0B3),
        secondary = Color(0xFF5EEAD4),
        onSecondary = Color(0xFF06201D),
        secondaryContainer = Color(0xFF164640),
        tertiary = Color(0xFFA5B4FC),
        background = Color(0xFF12100B),
        surface = Color(0xFF1F1A12),
        surfaceVariant = Color(0xFF302819),
        onSurface = Color(0xFFFFF8E8),
        onSurfaceVariant = Color(0xFFD5C8AD),
        outline = Color(0xFF827760)
    )
    else -> darkColorScheme(
        primary = Color(0xFF5EEAD4),
        onPrimary = Color(0xFF06201D),
        primaryContainer = Color(0xFF123D3A),
        onPrimaryContainer = Color(0xFFC8FFF5),
        secondary = Color(0xFFFFB86B),
        onSecondary = Color(0xFF2A1600),
        secondaryContainer = Color(0xFF4A2A08),
        tertiary = Color(0xFFA7C7FF),
        background = Color(0xFF0E1117),
        surface = Color(0xFF171B24),
        surfaceVariant = Color(0xFF232938),
        onSurface = Color(0xFFF3F7FB),
        onSurfaceVariant = Color(0xFFC2CBD8),
        outline = Color(0xFF6D7687)
    )
}
