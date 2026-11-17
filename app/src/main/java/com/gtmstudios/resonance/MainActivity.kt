
package com.gtmstudios.resonance

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.roundToInt
import java.util.concurrent.atomic.AtomicBoolean

private val Purple = Color(0xFF7868D9)
private val Ink = Color(0xFF22243A)
private val Muted = Color(0xFF898B9A)
private val Pale = Color(0xFFF3F1FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ResonanceApp() }
    }
}

class AudioEngine {
    private val sampleRate = 44100
    private val running = AtomicBoolean(false)

    @Volatile private var frequency = 432f
    @Volatile private var volume = 0.12f
    @Volatile private var audioTrack: AudioTrack? = null

    fun setFrequency(value: Float) {
        frequency = value.coerceIn(20f, 2000f)
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 0.25f)
    }

    @Synchronized
    fun play() {
        if (!running.compareAndSet(false, true)) return

        val minimum = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minimum <= 0) {
            running.set(false)
            return
        }

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minimum, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            running.set(false)
            return
        }

        audioTrack = track

        try {
            track.play()
        } catch (_: Exception) {
            running.set(false)
            track.release()
            audioTrack = null
            return
        }

        Thread {
            val buffer = ShortArray(1024)
            var phase = 0.0

            try {
                while (running.get()) {
                    val hz = frequency
                    val amplitude = volume
                    val increment = 2.0 * PI * hz / sampleRate

                    for (i in buffer.indices) {
                        buffer[i] = (
                            sin(phase) * amplitude * Short.MAX_VALUE
                        ).roundToInt().toShort()

                        phase += increment
                        if (phase >= 2.0 * PI) phase -= 2.0 * PI
                    }

                    val written = track.write(
                        buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING
                    )
                    if (written < 0) break
                }
            } catch (_: Exception) {
                // Audio output is no longer available.
            } finally {
                running.set(false)
                try {
                    track.stop()
                } catch (_: Exception) {
                }
                track.release()
                synchronized(this) {
                    if (audioTrack === track) audioTrack = null
                }
            }
        }.apply {
            name = "ResonanceAudio"
            start()
        }
    }

    fun stop() {
        running.set(false)
        try {
            audioTrack?.stop()
        } catch (_: Exception) {
        }
    }
}

@Composable
fun ResonanceApp() {
    val engine = remember { AudioEngine() }

    var frequency by remember { mutableFloatStateOf(432f) }
    var volume by remember { mutableFloatStateOf(0.12f) }
    var playing by remember { mutableStateOf(false) }

    var showFrequencyDialog by remember { mutableStateOf(false) }
    var customFrequency by remember { mutableStateOf("432") }
    var frequencyError by remember { mutableStateOf(false) }

    val presets = listOf(174, 285, 396, 432, 528, 639, 741, 852, 963)

    LaunchedEffect(frequency, volume) {
        engine.setFrequency(frequency)
        engine.setVolume(volume)
    }

    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple,
            background = Color.White,
            surface = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 22.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "RESONANCE",
                        color = Ink,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Your personal meditation space",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Pale, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◉", color = Purple, fontSize = 24.sp)
                }
            }

            Spacer(Modifier.height(42.dp))

            Text(
                "YOUR FREQUENCY",
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.clickable {
                    customFrequency = frequency.roundToInt().toString()
                    frequencyError = false
                    showFrequencyDialog = true
                }
            ) {
                Text(
                    frequency.roundToInt().toString(),
                    fontSize = 68.sp,
                    color = Ink,
                    fontWeight = FontWeight.Light
                )

                Text(
                    " Hz",
                    fontSize = 21.sp,
                    color = Purple,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text(
                "Tap the frequency number to edit",
                color = Purple,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(25.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val path = Path()
                val center = size.height / 2

                for (i in 0..300) {
                    val x = size.width * i / 300f
                    val angle = i / 300f * 4f * PI
                    val amplitude = if (playing) 30f else 12f
                    val y = center + (sin(angle) * amplitude).toFloat()

                    if (i == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = Purple,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            Slider(
                value = frequency,
                onValueChange = { frequency = it },
                valueRange = 20f..2000f,
                colors = SliderDefaults.colors(
                    thumbColor = Purple,
                    activeTrackColor = Purple
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("20 Hz", color = Muted, fontSize = 11.sp)
                Text("2000 Hz", color = Muted, fontSize = 11.sp)
            }

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (!playing) {
                            engine.play()
                            playing = true
                        }
                    },
                    enabled = !playing,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text("▶  Play")
                }

                OutlinedButton(
                    onClick = {
                        engine.stop()
                        playing = false
                    },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text("Stop", color = Ink)
                }
            }

            Spacer(Modifier.height(30.dp))

            Text(
                "EXPLORE FREQUENCIES",
                modifier = Modifier.fillMaxWidth(),
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(14.dp))

            presets.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { hz ->
                        val selected = frequency.roundToInt() == hz

                        Surface(
                            onClick = { frequency = hz.toFloat() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) Pale else Color(0xFFF8F8FA)
                        ) {
                            Column(
                                modifier = Modifier.padding(13.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$hz",
                                    color = if (selected) Purple else Ink,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Hz", color = Muted, fontSize = 11.sp)
                            }
                        }
                    }

                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(9.dp))
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "VOLUME",
                modifier = Modifier.fillMaxWidth(),
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("−", color = Muted)

                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..0.25f,
                    modifier = Modifier.weight(1f)
                )

                Text("+", color = Muted)
            }

            Spacer(Modifier.weight(1f))

            Text(
                "RESONANCE  •  GTM STUDIOS",
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp
            )
        }

        if (showFrequencyDialog) {
            AlertDialog(
                onDismissRequest = {
                    showFrequencyDialog = false
                },
                title = {
                    Text(
                        "Custom Frequency",
                        color = Ink,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "Enter a frequency between 20 and 2000 Hz.",
                            color = Muted,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = customFrequency,
                            onValueChange = {
                                customFrequency = it.filter { char ->
                                    char.isDigit()
                                }
                                frequencyError = false
                            },
                            singleLine = true,
                            label = { Text("Frequency (Hz)") },
                            placeholder = { Text("Example: 777") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            isError = frequencyError,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (frequencyError) {
                            Text(
                                "Enter a whole number from 20 to 2000.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val value = customFrequency.toIntOrNull()

                            if (value != null && value in 20..2000) {
                                frequency = value.toFloat()
                                showFrequencyDialog = false
                                frequencyError = false
                            } else {
                                frequencyError = true
                            }
                        }
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showFrequencyDialog = false
                        }
                    ) {
                        Text("Cancel", color = Ink)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }
    }
}
