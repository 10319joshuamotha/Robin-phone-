package com.robin.assistant

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private val Night = Color(0xFF070A18)
private val Cyan = Color(0xFF78E8FF)
private val Violet = Color(0xFF9D75FF)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var ttsReady = false
    private var state by mutableStateOf("Ready")
    private var transcript by mutableStateOf("Tap the orb and speak")
    private var reply by mutableStateOf("I'm ROBIN. Tap the orb to try a voice command.")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { speech ->
                speech.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { state = "Listening"; transcript = "Listening…" }
                    override fun onBeginningOfSpeech() { state = "Listening" }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { state = "Thinking" }
                    override fun onError(error: Int) {
                        state = "Ready"
                        reply = when (error) {
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition needs a working recognition provider or network connection."
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't catch that. Tap the orb and try again."
                            else -> "Speech recognition stopped. Please try again."
                        }
                        transcript = "No speech recognized"
                    }
                    override fun onResults(results: Bundle?) {
                        val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        transcript = words.ifBlank { "No speech recognized" }
                        state = "Thinking"
                        handleCommand(words)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val words = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!words.isNullOrBlank()) transcript = words
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Cyan, secondary = Violet, background = Night, surface = Color(0xFF11152A),
                onBackground = Color(0xFFF2F4FF), onSurface = Color(0xFFF2F4FF)
            )) {
                val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) startListening() else {
                        state = "Ready"
                        reply = "Microphone permission was not granted. You can enable it in Android Settings."
                    }
                }
                RobinHome(
                    state = state,
                    transcript = transcript,
                    reply = reply,
                    onOrbTap = {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startListening()
                        } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onDial = { openDialer(null) }
                )
            }
        }
    }

    override fun onInit(statusCode: Int) {
        ttsReady = statusCode == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.getDefault()
    }

    private fun startListening() {
        if (recognizer == null) {
            state = "Ready"
            reply = "This Android device has no speech recognition service available."
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to ROBIN")
            }
            state = "Listening"
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            state = "Ready"
            reply = "I couldn't start speech recognition. Please try again."
        }
    }

    private fun say(text: String) {
        reply = text
        state = "Speaking"
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "robin-reply")
        else state = "Ready"
    }

    private fun handleCommand(raw: String) {
        val command = raw.trim().lowercase(Locale.ROOT).removePrefix("robin").trim().trim(',', '.', ' ')
        when {
            command.isBlank() -> say("I'm listening. What would you like me to do?")
            command.contains("open phone") || command.contains("open dialer") || command.contains("phone dialer") -> {
                say("Opening the phone dialer.")
                openDialer(null)
            }
            command.startsWith("call ") -> {
                val number = command.removePrefix("call ").filter { it.isDigit() || it == '+' }
                if (number.length >= 3) {
                    say("Opening the dialer with ${number}. Please review and place the call yourself.")
                    openDialer(number)
                } else say("Please say a phone number after the word call.")
            }
            command.contains("what can you do") || command == "help" -> say("For this first version, I can listen to simple commands, speak replies, and open the phone dialer. Offline AI, recording, reminders, and wake word are not implemented yet.")
            command.contains("hello") || command.contains("hi robin") -> say("Hello! I'm ROBIN. How can I help?")
            command.contains("what time") -> {
                val time = java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date())
                say("It's $time.")
            }
            else -> say("I heard: ${raw}. The offline AI engine isn't connected yet. Try saying open dialer, call followed by a number, or help.")
        }
        if (state == "Thinking") state = "Ready"
    }

    private fun openDialer(number: String?) {
        val uri = if (number.isNullOrBlank()) Uri.parse("tel:") else Uri.fromParts("tel", number, null)
        try { startActivity(Intent(Intent.ACTION_DIAL, uri)) }
        catch (_: ActivityNotFoundException) { say("No compatible dialer app was found.") }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

@Composable
private fun RobinHome(
    state: String,
    transcript: String,
    reply: String,
    onOrbTap: () -> Unit,
    onDial: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF111A38), Night, Color(0xFF0C1025)))
        )) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ROBIN", fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 7.sp, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text("YOUR PERSONAL ASSISTANT", fontSize = 10.sp, letterSpacing = 2.5.sp, color = Cyan.copy(alpha = .75f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Orb(state = state, onTap = onOrbTap)
                    Spacer(Modifier.height(28.dp))
                    Text(state.uppercase(Locale.ROOT), color = Cyan, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text(transcript, color = Color.White.copy(alpha = .72f), fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 3)
                    Spacer(Modifier.height(16.dp))
                    Surface(color = Color.White.copy(alpha = .055f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(reply, modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp), color = Color(0xFFE3E8FF), fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Tap the orb to speak", color = Color.White.copy(alpha = .42f), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onOrbTap, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202B4A))) {
                            Text("🎙  Speak")
                        }
                        Button(onClick = onDial, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202B4A))) {
                            Text("☎  Dialer")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("STAGE 1 · VOICE + LOCAL ACTIONS", color = Color.White.copy(alpha = .32f), fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
            }
        }
    }
}

@Composable
private fun Orb(state: String, onTap: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 0.90f, targetValue = if (state == "Listening") 1.12f else 1.04f,
        animationSpec = infiniteRepeatable(tween(if (state == "Listening") 650 else 1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(9000), RepeatMode.Restart), label = "spin")
    Canvas(modifier = Modifier.size(270.dp).clip(CircleShape).clickable(onClick = onTap)) {
        val center = Offset(size.width / 2, size.height / 2)
        val base = size.minDimension * .27f * pulse
        drawCircle(brush = Brush.radialGradient(listOf(Cyan.copy(alpha = .19f), Violet.copy(alpha = .09f), Color.Transparent), center, base * 2.8f), radius = base * 2.8f, center = center)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE8FFFF), Cyan.copy(alpha = .88f), Violet.copy(alpha = .76f), Color(0xFF29245D)), center, base * 1.5f), radius = base * 1.5f, center = center)
        drawCircle(color = Color.White.copy(alpha = .24f), radius = base * 1.23f, center = center, style = Stroke(width = 1.4.dp.toPx()))
        val orbit = base * 1.85f
        val angle = Math.toRadians(spin.toDouble())
        drawCircle(color = Cyan.copy(alpha = .8f), radius = 4.dp.toPx(), center = Offset(center.x + kotlin.math.cos(angle).toFloat() * orbit, center.y + kotlin.math.sin(angle).toFloat() * orbit))
        drawCircle(color = Violet.copy(alpha = .9f), radius = 3.dp.toPx(), center = Offset(center.x + kotlin.math.cos(angle + 2.6).toFloat() * orbit, center.y + kotlin.math.sin(angle + 2.6).toFloat() * orbit))
        drawCircle(color = Color.White.copy(alpha = .58f), radius = base * .27f, center = Offset(center.x - base * .43f, center.y - base * .53f))
    }
}
