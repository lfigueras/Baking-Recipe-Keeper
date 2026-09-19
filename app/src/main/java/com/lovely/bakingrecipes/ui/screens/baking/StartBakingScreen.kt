package com.lovely.bakingrecipes.ui.screens.baking

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.data.Step
import com.lovely.bakingrecipes.ui.components.brandedTopAppBarColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartBakingScreen(
    pastry: PastryWithIngredients?,
    onExit: () -> Unit
) {
    // Keep the screen awake while baking.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? ComponentActivity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val steps = remember(pastry?.pastry?.id) {
        pastry?.steps?.sortedBy { it.position } ?: emptyList()
    }
    var index by remember(pastry?.pastry?.id) { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = brandedTopAppBarColors(),
                title = { Text(pastry?.pastry?.name ?: "Start Baking") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit baking mode")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (pastry == null || steps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("This recipe has no steps to bake.")
            }
            return@Scaffold
        }

        val current = steps[index]
        val progress = (index + 1).toFloat() / steps.size

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Step ${index + 1} of ${steps.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = current.instruction,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                StepTimer(step = current)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { if (index > 0) index-- },
                    enabled = index > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Previous")
                }
                if (index < steps.size - 1) {
                    Button(
                        onClick = { index++ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = onExit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

// Detects a duration like "15 minutes" or "30 sec" in the step text and offers a countdown.
@Composable
private fun StepTimer(step: Step) {
    val suggestedSeconds = remember(step.id) { extractDurationSeconds(step.instruction) }
    if (suggestedSeconds == null) return

    var remaining by remember(step.id) { mutableIntStateOf(suggestedSeconds) }
    var running by remember(step.id) { mutableStateOf(false) }

    LaunchedEffect(running, step.id) {
        while (running && remaining > 0) {
            delay(1000)
            remaining--
        }
        if (remaining == 0) running = false
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatTime(remaining),
            style = MaterialTheme.typography.displaySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = { running = !running }) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Text(if (running) "Pause" else "Start")
            }
            OutlinedButton(onClick = {
                running = false
                remaining = suggestedSeconds
            }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text("Reset")
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

// Parses the first "<number> min/hour/sec" mention into seconds, if present.
internal fun extractDurationSeconds(text: String): Int? {
    val match = Regex(
        "(\\d+)\\s*(hours?|hrs?|minutes?|mins?|seconds?|secs?)",
        RegexOption.IGNORE_CASE
    ).find(text) ?: return null
    val value = match.groupValues[1].toIntOrNull() ?: return null
    val unit = match.groupValues[2].lowercase()
    return when {
        unit.startsWith("h") -> value * 3600
        unit.startsWith("m") -> value * 60
        else -> value
    }
}
