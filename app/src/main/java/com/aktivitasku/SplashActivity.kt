package com.aktivitasku

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.edit
import com.aktivitasku.presentation.onboarding.OnboardingScreen
import com.aktivitasku.presentation.settings.PrefKeys
import com.aktivitasku.presentation.settings.dataStore
import com.aktivitasku.presentation.theme.*
import com.aktivitasku.util.PermissionRequestScreen
import com.aktivitasku.util.checkAppPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class SplashState {
    LOADING, PERMISSIONS, ONBOARDING, DONE
}

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AktivitasKuTheme {
                var isFirst  by remember { mutableStateOf(true) }
                var state    by remember { mutableStateOf(SplashState.LOADING) }
                val scope    = rememberCoroutineScope()

                // Initial check on launch
                LaunchedEffect(Unit) {
                    delay(700)
                    val prefs      = dataStore.data.first()
                    isFirst        = prefs[PrefKeys.FIRST_LAUNCH] != false
                    val perms      = checkAppPermissions()
                    val allGranted = perms.hasNotification && perms.hasExactAlarm

                    state = when {
                        !allGranted -> SplashState.PERMISSIONS
                        isFirst     -> SplashState.ONBOARDING
                        else        -> SplashState.DONE
                    }
                }

                when (state) {
                    SplashState.LOADING -> SplashContent()

                    SplashState.PERMISSIONS -> PermissionRequestScreen(
                        onAllGranted = {
                            // After permissions granted, check if still needs onboarding
                            state = if (isFirst) SplashState.ONBOARDING else SplashState.DONE
                        }
                    )

                    SplashState.ONBOARDING -> OnboardingScreen(
                        onFinish = {
                            scope.launch {
                                dataStore.edit { it[PrefKeys.FIRST_LAUNCH] = false }
                                goToMain()
                            }
                        }
                    )

                    SplashState.DONE -> {
                        LaunchedEffect(Unit) { goToMain() }
                        SplashContent()
                    }
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

@Composable
private fun SplashContent() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Blue900, Blue700)))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(White.copy(alpha = 0.15f))
            ) {
                Text("⏰", fontSize = 40.sp)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "AktivitasKu",
                fontSize   = 30.sp,
                fontWeight = FontWeight.Bold,
                color      = White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Kelola kegiatanmu dengan mudah",
                fontSize = 14.sp,
                color    = Teal400
            )
        }
    }
}
