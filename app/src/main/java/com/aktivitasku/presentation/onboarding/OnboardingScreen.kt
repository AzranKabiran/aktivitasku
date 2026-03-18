package com.aktivitasku.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aktivitasku.presentation.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val title: String,
    val description: String,
    val gradientColors: List<Color>
)

val onboardingPages = listOf(
    OnboardingPage(
        icon            = Icons.Rounded.EventNote,
        iconBg          = Blue50,
        iconTint        = Blue700,
        title           = "Catat Semua Kegiatanmu",
        description     = "Simpan jadwal harian, mingguan, atau bulanan dengan mudah. Tambah kategori, prioritas, dan deskripsi untuk setiap kegiatan.",
        gradientColors  = listOf(Blue50, Color.White)
    ),
    OnboardingPage(
        icon            = Icons.Rounded.Mic,
        iconBg          = Teal50,
        iconTint        = Teal600,
        title           = "Input Suara Bahasa Indonesia",
        description     = "Cukup ucapkan \"Besok jam 9 pagi meeting dengan klien\" — aplikasi langsung mengisi form secara otomatis. Cepat dan praktis!",
        gradientColors  = listOf(Teal50, Color.White)
    ),
    OnboardingPage(
        icon            = Icons.Rounded.NotificationsActive,
        iconBg          = Blue50,
        iconTint        = Blue700,
        title           = "Alarm Tepat Waktu",
        description     = "Dapatkan notifikasi 5 menit, 15 menit, hingga 1 jam sebelum kegiatan dimulai — bahkan saat layar HP mati.",
        gradientColors  = listOf(Blue50, Color.White)
    ),
    OnboardingPage(
        icon            = Icons.Rounded.BarChart,
        iconBg          = Teal50,
        iconTint        = Teal600,
        title           = "Pantau Produktivitasmu",
        description     = "Lihat grafik mingguan, streak hari berturut-turut, dan breakdown kegiatan per kategori untuk memahami polamu.",
        gradientColors  = listOf(Teal50, Color.White)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { onboardingPages.size }
    val scope      = rememberCoroutineScope()
    val isLast     = pagerState.currentPage == onboardingPages.lastIndex

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPage(page = onboardingPages[page])
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(onboardingPages.size) { i ->
                    val selected = i == pagerState.currentPage
                    val width by animateDpAsState(
                        targetValue = if (selected) 28.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) Blue700 else Blue700.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip
                AnimatedVisibility(visible = !isLast) {
                    TextButton(onClick = onFinish) {
                        Text(
                            "Lewati",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isLast) Spacer(Modifier.weight(1f))

                // Next / Mulai
                Button(
                    onClick = {
                        if (isLast) onFinish()
                        else scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLast) Teal400 else Blue700
                    ),
                    modifier = if (isLast) Modifier.fillMaxWidth() else Modifier
                ) {
                    Text(
                        text  = if (isLast) "Mulai Sekarang" else "Lanjut",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (!isLast) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors  = page.gradientColors,
                    endY    = 800f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .padding(bottom = 180.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated icon container
            val infiniteTransition = rememberInfiniteTransition(label = "iconFloat")
            val offsetY by infiniteTransition.animateFloat(
                initialValue  = -6f,
                targetValue   = 6f,
                animationSpec = infiniteRepeatable(
                    tween(2000, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "float"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(page.iconBg)
                    .border(
                        width = 1.dp,
                        color = page.iconTint.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Icon(
                    page.icon,
                    contentDescription = null,
                    tint     = page.iconTint,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text      = page.title,
                style     = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                ),
                color     = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text      = page.description,
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
    }
}
