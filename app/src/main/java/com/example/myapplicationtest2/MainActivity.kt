package com.example.myapplicationtest2

import android.os.Bundle
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import kotlin.random.Random

/**
 * Konfettihiukkasen mallinnus loppuanimaatiota varten.
 */
data class ConfettiPiece(
    var x: Float,
    var y: Float,
    var color: Color,
    var speed: Float,
    var rotation: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                MatiasTabataApp()
            }
        }
    }
}

@Composable
fun MatiasTabataApp() {
    // --- ASETUKSET JA TILA ---
    var workTimeInput by remember { mutableIntStateOf(20) } // Treeniaika (oletus 20s)
    var restTimeInput by remember { mutableIntStateOf(10) } // Lepoaika (oletus 10s)
    var showSettings by remember { mutableStateOf(true) }  // Hallitsee asetusikkunan näkyvyyttä
    var totalRoundsInput by remember { mutableIntStateOf(8) } // Oletus 8 kierrosta

    var secondsLeft by remember { mutableIntStateOf(5) }
    var isRunning by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("PREP") }
    var round by remember { mutableIntStateOf(1) }
    var isFinished by remember { mutableStateOf(false) }
    var motivationText by remember { mutableStateOf("") }
    val motivations = listOf("DON'T GIVE UP!", "KEEP GOING!", "PUSH IT!", "ALMOST THERE!", "STAY STRONG!")

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60) }
    val context = LocalContext.current
    val bgMusic = remember {
        MediaPlayer.create(context, R.raw.final_final_treenimusa).apply {
            isLooping = true
            setVolume(1.0f, 1.0f)
        }
    }

    // --- LOGIIKKA ---

    // Musiikin hallinta
    LaunchedEffect(isRunning) {
        if (isRunning) { bgMusic.start() }
        // Musiikki jätetään soimaan, jotta treeni ei hiljene pausella (tyylivalinta)
    }

    DisposableEffect(Unit) {
        onDispose {
            bgMusic.stop()
            bgMusic.release()
        }
    }

    // Edistymispalkin laskenta dynaamisesti
    val totalTimeInPhase = when(phase) {
        "PREP" -> 5f
        "WORK" -> workTimeInput.toFloat()
        else -> restTimeInput.toFloat()
    }
    val progress by animateFloatAsState(targetValue = secondsLeft / totalTimeInPhase)

    // Ajastimen päälooppi
    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && !isFinished) {
            if (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
                if (secondsLeft < 3 && secondsLeft > 0) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
            } else {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                when (phase) {
                    "PREP" -> {
                        phase = "WORK"
                        secondsLeft = workTimeInput
                    }
                    "WORK" -> {
                        if (round == totalRoundsInput) {
                            isFinished = true
                            isRunning = false
                        } else {
                            phase = "REST"
                            secondsLeft = restTimeInput
                            motivationText = if (round >= 3 && Random.nextFloat() > 0.5f) motivations.random() else ""
                        }
                    }
                    "REST" -> {
                        phase = "WORK"
                        secondsLeft = workTimeInput
                        round++
                        motivationText = ""
                    }
                }
            }
        }
    }

    // Taustavärin vaihtelu
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFinished -> Color(0xFF4527A0)
            phase == "PREP" -> Color(0xFF5D5B5B)
            phase == "WORK" -> Color(0xFF2E7D32)
            else -> Color(0xFFC62828)
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(WindowInsets.systemBars.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        // --- UI KERROKSET ---

        // 1. Konfetit (alin kerros)
        ConfettiRain(visible = isFinished)

        // 2. Pääsisältö (sumennetaan jos asetukset on auki)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 50.dp)
                .blur(if (showSettings) 10.dp else 0.dp)
        ) {
            // Yläosa
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isFinished) {
                    Text("FINISHED", fontSize = 60.sp, color = Color.White, fontWeight = FontWeight.Black)
                    Text("🏆", fontSize = 90.sp)
                } else {
                    Text(
                        text = when(phase) {
                            "PREP" -> "GET READY ⚡"
                            "WORK" -> "WORK 🔥"
                            else -> "REST 😴"
                        },
                        fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                    Text("ROUND $round / $totalRoundsInput", fontSize = 24.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }

            // Keskiosa (Ajastin)
            if (!isFinished) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(280.dp),
                        color = Color.White,
                        strokeWidth = 12.dp,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d", secondsLeft),
                            fontSize = 120.sp, color = Color.White, fontWeight = FontWeight.Black
                        )
                        if (motivationText.isNotEmpty()) {
                            Text(text = motivationText, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Alaosa (Napit)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isFinished) {
                    Button(
                        onClick = {
                            if (bgMusic.isPlaying) { bgMusic.stop(); bgMusic.prepare() }
                            isFinished = false; isRunning = false; secondsLeft = 5; phase = "PREP"; round = 1; showSettings = true
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(70.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("BACK TO START", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { isRunning = !isRunning; showSettings = false },
                        modifier = Modifier.fillMaxWidth(0.8f).height(70.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text(text = if (isRunning) "PAUSE" else "START", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = {
                        if (bgMusic.isPlaying) { bgMusic.stop(); bgMusic.prepare() }
                        isRunning = false; secondsLeft = 5; phase = "PREP"; round = 1; showSettings = true
                    }) {
                        Text("RESET & SETTINGS ⚙\uFE0F", color = Color.White.copy(alpha = 1.5f))
                    }
                }
            }
        }

        // 3. Asetusnappi (Oikea yläkulma)
//        if (!isRunning && !isFinished) {
//            Box(modifier = Modifier.fillMaxSize().padding(15.dp), // Reunan etäisyys
//                contentAlignment = Alignment.TopEnd) { // Sijainti
//                IconButton(onClick = { showSettings = !showSettings }) {
//                    Text("⚙️", fontSize = 32.sp)
//                }
//            }
//        }

        // 4. Asetusvalikko (Ylin kerros)
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIMER SETTINGS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Rounds: $totalRoundsInput", color = Color.White)
                    Slider(
                        value = totalRoundsInput.toFloat(),
                        onValueChange = { totalRoundsInput = it.toInt() },
                        valueRange = 1f..15f, // Käyttäjä voi valita 1-15 kierrosta
                        steps = 13, // Tekee sliderista "pykälittäisen", jolloin on helpompi valita tasan lukuja
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Work Time: $workTimeInput s", color = Color.White)
                    Slider(
                        value = workTimeInput.toFloat(),
                        onValueChange = { workTimeInput = it.toInt() },
                        valueRange = 5f..60f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Rest Time: $restTimeInput s", color = Color.White)
                    Slider(
                        value = restTimeInput.toFloat(),
                        onValueChange = { restTimeInput = it.toInt() },
                        valueRange = 5f..30f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showSettings = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("SAVE & CLOSE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Konfettianimaatio Canvasilla.
 */
@Composable
fun ConfettiRain(visible: Boolean) {
    if (!visible) return
    val pieces = remember {
        List(100) {
            ConfettiPiece(
                x = Random.nextFloat() * 1500f,
                y = -(Random.nextFloat() * 2000f),
                color = listOf(Color.Yellow, Color.Cyan, Color.Magenta, Color.White, Color.Green).random(),
                speed = 5f + Random.nextFloat() * 10f,
                rotation = Random.nextFloat() * 360f
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val elapsed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3000f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing)),
        label = "drop"
    )
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val currentY = (piece.y + elapsed * piece.speed / 5) % size.height
            drawRect(
                color = piece.color,
                topLeft = androidx.compose.ui.geometry.Offset(piece.x % size.width, currentY),
                size = androidx.compose.ui.geometry.Size(25f, 12f)
            )
        }
    }
}
