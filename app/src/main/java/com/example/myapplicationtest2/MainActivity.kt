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
import kotlin.random.Random

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
            // Tässä kutsutaan pääsovellusta
            Surface(modifier = Modifier.fillMaxSize()) {
                MatiasTabataApp()
            }
        }
    }
}

@Composable
fun MatiasTabataApp() {
    // TILAT
    var secondsLeft by remember { mutableIntStateOf(5) } // Aloitetaan 5s valmistautumisella
    var isRunning by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("PREP") } // PREP, WORK, REST
    var round by remember { mutableIntStateOf(1) }
    var isFinished by remember { mutableStateOf(false) }

    // Motivaatioteksti
    var motivationText by remember { mutableStateOf("") }
    val motivations = listOf("DON'T GIVE UP!", "KEEP GOING!", "PUSH IT!", "ALMOST THERE!", "STAY STRONG!")

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    // 1. Varmista alustus (lisää .apply { isLooping = true })
    val context = LocalContext.current
    val bgMusic = remember {
        MediaPlayer.create(context, R.raw.final_final_treenimusa).apply {
            isLooping = true
            setVolume(1.0f, 1.0f) // Laita täysille testin ajaksi!
        }
    }

// 2. Tehostettu käynnistyslogiikka
    LaunchedEffect(isRunning) {
        if (isRunning) {
            // Kun painat START, musa alkaa
            bgMusic.start()
        }
        // POISTETTU: else { bgMusic.pause() }
        // Näin musa jatkuu, vaikka isRunning muuttuisi falseksi (treeni loppuu)
    }

// 3. Pysäytetään musiikki VASTA kun poistutaan näkymästä tai painetaan menunappia
    DisposableEffect(Unit) {
        onDispose {
            bgMusic.stop()
            bgMusic.release()
        }
    }

    // Lasketaan edistymispalkin osuus (0.0 - 1.0)
    val totalTimeInPhase = when(phase) {
        "PREP" -> 5f
        "WORK" -> 20f
        else -> 10f
    }
    val progress by animateFloatAsState(targetValue = secondsLeft / totalTimeInPhase)

    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && !isFinished) {
            if (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--

                // Piippaus viimeisellä kolmella sekunnilla
                if (secondsLeft < 3 && secondsLeft > 0) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
            } else {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 300)

                when (phase) {
                    "PREP" -> {
                        phase = "WORK"
                        secondsLeft = 20
                    }
                    "WORK" -> {
                        if (round == 8) {
                            isFinished = true
                            isRunning = false
                        } else {
                            phase = "REST"
                            secondsLeft = 10
                            // Arvotaan tsemppiteksti kierroksen 3 jälkeen
                            motivationText = if (round >= 3 && Random.nextFloat() > 0.5f) motivations.random() else ""
                        }
                    }
                    "REST" -> {
                        phase = "WORK"
                        secondsLeft = 20
                        round++
                        motivationText = ""
                    }
                }
            }
        }
    }

    // TAUSTAVÄRI
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFinished -> Color(0xFF4527A0)
            !isRunning && round == 1 && phase == "PREP" -> Color(0xFF263238)
            phase == "PREP" -> Color(0xFFFBC02D) // Keltainen valmistautuminen
            phase == "WORK" -> Color(0xFF2E7D32) // Vihreä työ
            else -> Color(0xFFC62828) // Punainen lepo
        }
    )

    Box(
        modifier = Modifier.fillMaxSize().background(backgroundColor).padding(WindowInsets.systemBars.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        ConfettiRain(visible = isFinished)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight().padding(vertical = 50.dp)
        ) {
            // YLÄOSA
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isFinished) {
                    Text("FINISHED", fontSize = 60.sp, color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 110.dp))
                    Text("🏆", fontSize = 90.sp, color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 30.dp))

                } else {
                    Text(
                        text = when(phase) {
                            "PREP" -> "GET READY ⚡"
                            "WORK" -> "WORK 🔥"
                            else -> "REST 😴"
                        },
                        fontSize = 40.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text("ROUND $round / 8", fontSize = 24.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }

            // KESKIOSA: Kello ja Edistymispalkki
            if (!isFinished) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(280.dp),
                        color = Color.White,
                        strokeWidth = 12.dp,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d", secondsLeft),
                            fontSize = 120.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        if (motivationText.isNotEmpty()) {
                            Text(
                                text = motivationText,
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // ALAOSA: Napit
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isFinished) {

                    // VAIN YKSI NAPPI LOPUSSA
                    Button(

                        onClick = {
                            if (bgMusic.isPlaying)
                            {
                                bgMusic.stop()
                                bgMusic.prepare()
                            }
                            // TÄYSI NOLLAUS
                            isFinished = false
                            isRunning = false
                            secondsLeft = 5
                            phase = "PREP"
                            round = 1
                            motivationText = ""
                        },

                        modifier = Modifier.fillMaxWidth().height(90.dp), // Tehdään tästäkin kunnon lätkä
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {

                        Text("DONE - BACK TO START", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                else {
                    // ALOITA / PAUSE
                    Button(
                        onClick = { isRunning = !isRunning },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (isRunning) "PAUSE" else "START",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // RESET NÄKYY VAIN TREENIN AIKANA (EI LOPUSSA)
                    if (!isRunning || round > 1 || phase != "PREP") {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                if (bgMusic.isPlaying)
                                {
                                    bgMusic.stop()
                                    bgMusic.prepare()
                                }
                                isRunning = false
                                isFinished = false
                                secondsLeft = 5
                                phase = "PREP"
                                round = 1
                                motivationText = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RESET", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
                        }
                    }

                    // Se kuuluisa Debug-nappi (Poista tämä kun olet kokeillut DONE-napin toimivuuden!)

//                Spacer(modifier = Modifier.height(8.dp))
//                 TextButton(onClick = { secondsLeft = 1; round = 8; phase = "WORK" }) {
//                    Text("Debug: Skip to End", color = Color.White.copy(alpha = 0.2f))
//                  }

                }

            }
        }
    }

}
@Composable
fun ConfettiRain(visible: Boolean) {
    if (!visible) return

    val pieces = remember {
        List(100) { // 70 suikaletta on sopiva määrä
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
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ), label = "drop"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val currentY = (piece.y + elapsed * piece.speed / 5) % size.height
            // Piirretään pieniä suorakaiteita (paperisuikaleita)
            drawRect(
                color = piece.color,
                topLeft = androidx.compose.ui.geometry.Offset(piece.x % size.width, currentY),
                size = androidx.compose.ui.geometry.Size(25f, 12f),
                alpha = if (currentY > size.height - 100) 0f else 1f
            )
        }
    }
}
