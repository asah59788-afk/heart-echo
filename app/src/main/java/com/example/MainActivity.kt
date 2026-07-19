package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.JournalEntry
import com.example.data.PlayerStats
import com.example.ui.GameViewModel
import com.example.ui.StoryChoice
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainGameScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGameScreen(viewModel: GameViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()
    val journalEntries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    val storyNarrative by viewModel.storyNarrative.collectAsStateWithLifecycle()
    val activeChoices by viewModel.activeChoices.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart",
                            tint = Color(0xFFF0718F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Heart's Echo",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 1.sp,
                            color = Color(0xFFFFB5C5)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Sparkles",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Game",
                            tint = Color(0xFF8B80A5)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F0B1E).copy(alpha = 0.95f),
                    titleContentColor = Color(0xFFFFB5C5)
                )
            )
        },
        bottomBar = {
            BottomNavigationBarDock(
                currentScreen = currentScreen,
                onScreenSelected = { viewModel.changeScreen(it) }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F0B1E), Color(0xFF1B1435))
                    )
                )
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "story" -> StoryTabScreen(
                    playerStats = playerStats,
                    narrative = storyNarrative,
                    choices = activeChoices,
                    onChoiceSelected = { viewModel.handleStoryChoice(it) }
                )
                "training" -> TrainingTabScreen(
                    playerStats = playerStats,
                    onPerformActivity = { viewModel.performActivity(it) }
                )
                "ai_advisor" -> AiAdvisorTabScreen(
                    isAnalyzing = isAnalyzing,
                    analysisError = analysisError,
                    onSubmitLetter = { title, content -> viewModel.submitLetter(title, content) }
                )
                "notebook" -> NotebookTabScreen(
                    entries = journalEntries,
                    onDeleteEntry = { viewModel.deleteJournal(it) }
                )
                "gallery" -> GalleryTabScreen(
                    playerStats = playerStats
                )
                else -> StoryTabScreen(
                    playerStats = playerStats,
                    narrative = storyNarrative,
                    choices = activeChoices,
                    onChoiceSelected = { viewModel.handleStoryChoice(it) }
                )
            }

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Restart Journey?", color = Color(0xFFFFB5C5)) },
                    text = { Text("This will erase all your courage stats, unlocked expressions, and written letters. Are you sure you want to start over?", color = Color(0xFFF5EEF0)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.resetGame()
                                showResetDialog = false
                            },
                            modifier = Modifier.testTag("confirm_reset_button")
                        ) {
                            Text("Yes, Restart", color = Color(0xFFF0718F))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Cancel", color = Color(0xFF8B80A5))
                        }
                    },
                    containerColor = Color(0xFF1B1435),
                    tonalElevation = 6.dp
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBarDock(
    currentScreen: String,
    onScreenSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1435).copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                NavigationTabItem("story", "Story", Icons.Default.Book, "story_tab"),
                NavigationTabItem("training", "Train", Icons.Default.Bolt, "train_tab"),
                NavigationTabItem("ai_advisor", "Heart Voice", Icons.Default.AutoAwesome, "ai_advisor_tab"),
                NavigationTabItem("notebook", "Notebook", Icons.Default.EditNote, "notebook_tab"),
                NavigationTabItem("gallery", "Gallery", Icons.Default.Star, "gallery_tab")
            )

            items.forEach { item ->
                val isSelected = currentScreen == item.id
                val tintColor = if (isSelected) Color(0xFFFFB5C5) else Color(0xFF8B80A5)
                val scale = if (isSelected) 1.15f else 1.0f

                Column(
                    modifier = Modifier
                        .testTag(item.testTag)
                        .scale(scale)
                        .clickable { onScreenSelected(item.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tintColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = tintColor,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

data class NavigationTabItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

// --- SCREEN 1: THE STORY PATH ---
@Composable
fun StoryTabScreen(
    playerStats: PlayerStats,
    narrative: String,
    choices: List<StoryChoice>,
    onChoiceSelected: (StoryChoice) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFFFB5C5).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hearts_echo_banner_1784442363233),
                    contentDescription = "Emotional Sunset school",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF0F0B1E).copy(alpha = 0.85f))
                            )
                        )
                )
                Text(
                    text = when (playerStats.currentChapter) {
                        0 -> "Chapter 1: The Passing Glance"
                        1 -> "Chapter 2: The Library Helper"
                        2 -> "Chapter 3: The Shared Umbrella"
                        3 -> "Chapter 4: Under the Stars"
                        else -> "Endless Memories"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB5C5),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
        }

        // Stats Display Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1435)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF8B80A5).copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Attributes",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B80A5),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatLabel(name = "Courage", value = playerStats.courage, color = Color(0xFFF0718F))
                        StatLabel(name = "Sincerity", value = playerStats.sincerity, color = Color(0xFFFFB5C5))
                        StatLabel(name = "Thoughtfulness", value = playerStats.thoughtfulness, color = Color(0xFFFFD700))
                        StatLabel(name = "Eloquence", value = playerStats.eloquence, color = Color(0xFF4AC29A))
                    }
                }
            }
        }

        // Narrative Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1435).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFFFB5C5).copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = narrative,
                        color = Color(0xFFF5EEF0),
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }

        // Available choices
        items(choices) { choice ->
            val isLocked = choice.requiredStat != null && when (choice.requiredStat) {
                "courage" -> playerStats.courage < choice.requiredStatValue
                "sincerity" -> playerStats.sincerity < choice.requiredStatValue
                "thoughtfulness" -> playerStats.thoughtfulness < choice.requiredStatValue
                "eloquence" -> playerStats.eloquence < choice.requiredStatValue
                else -> false
            }

            Button(
                onClick = { if (!isLocked) onChoiceSelected(choice) },
                enabled = !isLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("choice_${choice.text.take(10).replace(" ", "_")}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocked) Color(0xFF1B1435) else Color(0xFFF0718F),
                    contentColor = if (isLocked) Color(0xFF8B80A5) else Color.White,
                    disabledContainerColor = Color(0xFF1B1435).copy(alpha = 0.5f),
                    disabledContentColor = Color(0xFF8B80A5)
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isLocked) BorderStroke(1.dp, Color(0xFF8B80A5).copy(alpha = 0.3f)) else null
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${choice.text} (Requires ${choice.requiredStatValue} ${choice.requiredStat?.capitalize()})",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart option",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = choice.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Lock guidance message
        if (choices.any { it.requiredStat != null }) {
            item {
                Text(
                    text = "💡 Shyness limits your options. Head to the 'Train' deck to build up your stats!",
                    color = Color(0xFF8B80A5),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun StatLabel(name: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, fontSize = 11.sp, color = Color(0xFF8B80A5))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$value",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}


// --- SCREEN 2: COURAGE TRAINING ACTIVITIES ---
@Composable
fun TrainingTabScreen(
    playerStats: PlayerStats,
    onPerformActivity: (String) -> Unit
) {
    var isBreathing by remember { mutableStateOf(false) }
    var breathingCycles by remember { mutableStateOf(0) }
    var breathingText by remember { mutableStateOf("Ready to breathe?") }
    
    val breathingScope = rememberCoroutineScope()

    // Breathing pulse scale animation
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isBreathing) 1.5f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Train Your Courage",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB5C5)
            )
            Text(
                text = "Engage in positive mental exercises to build up your emotional strength and open up new options with Alice.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8B80A5),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        // Custom Breathing Session (Animated Component)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1435)),
                border = BorderStroke(1.dp, Color(0xFFFFB5C5).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Deep Breathing sanctuary",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB5C5),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Inhale courage, exhale fear. Calms the nerves and gives (+6 Courage, +2 Thoughtfulness).",
                        fontSize = 12.sp,
                        color = Color(0xFF8B80A5),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Breath Circle Animated Indicator
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFF0718F).copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(2.dp, Color(0xFFF0718F).copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Breathing heart",
                            tint = Color(0xFFF0718F),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = breathingText,
                        color = Color(0xFFF5EEF0),
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (!isBreathing) {
                                isBreathing = true
                                breathingCycles = 0
                                breathingScope.launch {
                                    onPerformActivity("breathe")
                                    breathingText = "Breathe in deeply..."
                                    delay(4000)
                                    breathingText = "Hold..."
                                    delay(2000)
                                    breathingText = "Slowly breathe out..."
                                    delay(4000)
                                    breathingText = "Beautiful. You feel a surge of inner courage!"
                                    isBreathing = false
                                }
                            }
                        },
                        enabled = !isBreathing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0718F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("breath_exercise_button")
                    ) {
                        Text(if (isBreathing) "Breathing..." else "Begin Breathing Session")
                    }
                }
            }
        }

        item {
            Text(
                text = "Other Expressive Exercises",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB5C5),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Training Activities Grid
        val activities = listOf(
            TrainingActivity(
                id = "diary",
                title = "Write in Secret Diary",
                desc = "Write down unsaid thoughts. Reflecting build deep honesty.",
                reward = "+5 Sincerity, +2 Thoughtfulness",
                icon = Icons.Default.MenuBook,
                iconColor = Color(0xFFFFB5C5)
            ),
            TrainingActivity(
                id = "mirror",
                title = "Mirror Practice",
                desc = "Practice saying compliments aloud to build your composure.",
                reward = "+5 Eloquence, +2 Courage",
                icon = Icons.Default.RecordVoiceOver,
                iconColor = Color(0xFF4AC29A)
            ),
            TrainingActivity(
                id = "origami",
                title = "Fold Origami Stars",
                desc = "Fold small paper stars to focus your care and dedication.",
                reward = "+5 Thoughtfulness, +5 Sincerity",
                icon = Icons.Default.AutoAwesome,
                iconColor = Color(0xFFFFD700)
            ),
            TrainingActivity(
                id = "music",
                title = "Listen to Lo-Fi",
                desc = "Find lyrical inspiration and rhythmic serenity in sweet acoustic songs.",
                reward = "+4 Sincerity, +3 Eloquence",
                icon = Icons.Default.MusicNote,
                iconColor = Color(0xFF90CAF9)
            )
        )

        items(activities) { act ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_card_${act.id}"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1435)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(act.iconColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = act.icon,
                            contentDescription = act.title,
                            tint = act.iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = act.title,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5EEF0),
                            fontSize = 14.sp
                        )
                        Text(
                            text = act.desc,
                            color = Color(0xFF8B80A5),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = act.reward,
                            color = act.iconColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { onPerformActivity(act.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0B1E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .testTag("train_${act.id}_button")
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Train", fontSize = 12.sp, color = Color(0xFFFFB5C5))
                    }
                }
            }
        }
    }
}

data class TrainingActivity(
    val id: String,
    val title: String,
    val desc: String,
    val reward: String,
    val icon: ImageVector,
    val iconColor: Color
)


// --- SCREEN 3: HEART VOICE SANCTUARY (GEMINI ADVISOR) ---
@Composable
fun AiAdvisorTabScreen(
    isAnalyzing: Boolean,
    analysisError: String?,
    onSubmitLetter: (String, String) -> Unit
) {
    var rawTitle by remember { mutableStateOf("") }
    var rawContent by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Heart Voice Sanctuary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB5C5)
            )
            Text(
                text = "Write your unpolished raw thoughts, fears, or letter ideas here. Your inner courage AI companion will analyze your sincerity, give tender advice, and rewrite it into a beautiful poetic confession.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8B80A5),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        // Input Deck
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1435)),
                border = BorderStroke(1.dp, Color(0xFFFFB5C5).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = rawTitle,
                        onValueChange = { rawTitle = it },
                        label = { Text("Title of Feeling (e.g. Rainy Day Locker)", color = Color(0xFF8B80A5)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB5C5),
                            unfocusedBorderColor = Color(0xFF8B80A5).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFFFB5C5),
                            focusedTextColor = Color(0xFFF5EEF0),
                            unfocusedTextColor = Color(0xFFF5EEF0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("letter_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rawContent,
                        onValueChange = { rawContent = it },
                        label = { Text("Describe your raw feelings, desires, or shyness...", color = Color(0xFF8B80A5)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB5C5),
                            unfocusedBorderColor = Color(0xFF8B80A5).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFFFB5C5),
                            focusedTextColor = Color(0xFFF5EEF0),
                            unfocusedTextColor = Color(0xFFF5EEF0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("letter_content_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAnalyzing) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFB5C5))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Whispering to your heart's advisor...",
                                color = Color(0xFFFFB5C5),
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (rawContent.isNotBlank()) {
                                    keyboardController?.hide()
                                    onSubmitLetter(rawTitle, rawContent)
                                    // Reset input values as it saves to database
                                    rawTitle = ""
                                    rawContent = ""
                                }
                            },
                            enabled = rawContent.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF0718F),
                                disabledContainerColor = Color(0xFF8B80A5).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_letter_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Sparkle",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Listen to Your Heart", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    analysisError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            color = Color(0xFFF0718F),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0B1E)),
                border = BorderStroke(1.dp, Color(0xFF8B80A5).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tips",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your entries will be automatically rated, and custom redrafted letters will be saved to your Notebook!",
                        color = Color(0xFF8B80A5),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}


// --- SCREEN 4: NOTEBOOK / DIARY ARCHIVE ---
@Composable
fun NotebookTabScreen(
    entries: List<JournalEntry>,
    onDeleteEntry: (Int) -> Unit
) {
    var expandedEntryId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Unspoken Notebook",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB5C5)
            )
            Text(
                text = "Every secret letter, raw feeling, and inner-voice response saved on your path to Alice.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8B80A5),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        if (entries.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Empty notes",
                        tint = Color(0xFF8B80A5).copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The pages are blank.",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5EEF0)
                    )
                    Text(
                        text = "Go write down your first secret feeling in the 'Heart Voice' sanctuary to begin your collection.",
                        color = Color(0xFF8B80A5),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp)
                    )
                }
            }
        }

        items(entries) { entry ->
            val isExpanded = expandedEntryId == entry.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_card_${entry.id}")
                    .clickable { expandedEntryId = if (isExpanded) null else entry.id },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1435)),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isExpanded) Color(0xFFFFB5C5).copy(alpha = 0.5f) else Color(0xFF8B80A5).copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB5C5),
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Earned: +${entry.courageEarned} Courage • +${entry.sincerityEarned} Sincerity",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = Color(0xFF8B80A5)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Divider(color = Color(0xFF8B80A5).copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Your Raw Feelings:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B80A5),
                                fontSize = 12.sp
                            )
                            Text(
                                text = entry.content,
                                color = Color(0xFFF5EEF0),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            Text(
                                text = "Inner Mirror Response & Redraft:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB5C5),
                                fontSize = 12.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .background(Color(0xFF0F0B1E), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = entry.aiFeedback,
                                    color = Color(0xFFF5EEF0),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = FontFamily.Serif
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { onDeleteEntry(entry.id) },
                                    modifier = Modifier.testTag("delete_entry_${entry.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFF0718F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Erase Thought", color = Color(0xFFF0718F), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- SCREEN 5: COLLECTED TOKENS OF EXPRESSION GALLERY ---
@Composable
fun GalleryTabScreen(
    playerStats: PlayerStats
) {
    val items = listOf(
        GalleryItem(
            id = "cherry_petal",
            title = "Cherry Blossom Petal",
            desc = "A tiny petal from the hallway cherry tree. You watched her walk by through the morning drift.",
            icon = "🌸",
            unlockedTip = "Unlocked by default"
        ),
        GalleryItem(
            id = "origami_star",
            title = "Origami Star",
            desc = "A small paper star of protection. You left it silently on Locker 104 to cheer her up.",
            icon = "⭐",
            unlockedTip = "Unlocked in Chapter 1"
        ),
        GalleryItem(
            id = "library_bookmark",
            title = "Leaf Bookmark",
            desc = "A dried leaf bookmark slipped inside a recommended poetry journal in the library.",
            icon = "🔖",
            unlockedTip = "Unlocked in Chapter 2"
        ),
        GalleryItem(
            id = "umbrella",
            title = "Shared Umbrella",
            desc = "A small haven of dry sky in the pouring summer storm. You stood so incredibly close.",
            icon = "☔",
            unlockedTip = "Unlocked in Chapter 3"
        ),
        GalleryItem(
            id = "music_tape",
            title = "Cassette Tape Playlist",
            desc = "A custom acoustic soundtrack sharing songs that put your silent voice into words.",
            icon = "📼",
            unlockedTip = "Unlocked with 8+ Sincerity AI response"
        ),
        GalleryItem(
            id = "love_letter",
            title = "Sealed Love Letter",
            desc = "The final testament of your heart. Poured out on cream-colored paper and handed under fireworks.",
            icon = "✉️",
            unlockedTip = "Unlocked with 9+ Sincerity & 8+ Courage AI letter"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Unspoken Gallery",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB5C5)
            )
            Text(
                text = "Tokens of your courage and thoughtfulness. Build your stats and write beautiful letters to unlock them all.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8B80A5),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1435), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val unlockedCount = items.count { playerStats.hasUnlocked(it.id) }
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = "Collection progress",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlocked: $unlockedCount / ${items.size} Tokens",
                    color = Color(0xFFF5EEF0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        gridItems(items, 2) { item ->
            val isUnlocked = playerStats.hasUnlocked(item.id)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .border(
                        width = 1.dp,
                        color = if (isUnlocked) Color(0xFFFFB5C5).copy(alpha = 0.4f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("gallery_item_${item.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) Color(0xFF1B1435) else Color(0xFF1B1435).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isUnlocked) item.icon else "🔒",
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = if (isUnlocked) item.title else "Unspoken Token",
                        color = if (isUnlocked) Color(0xFFFFB5C5) else Color(0xFF8B80A5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isUnlocked) item.desc else "Keep training and expressing to unlock: ${item.unlockedTip}",
                        color = Color(0xFF8B80A5),
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

data class GalleryItem(
    val id: String,
    val title: String,
    val desc: String,
    val icon: String,
    val unlockedTip: String
)

// Helper extension for list grids in Compose LazyColumn
fun <T> LazyListScope.gridItems(
    items: List<T>,
    columnCount: Int,
    itemContent: @Composable (T) -> Unit
) {
    val rows = items.chunked(columnCount)
    items(rows) { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (item in row) {
                Box(modifier = Modifier.weight(1f)) {
                    itemContent(item)
                }
            }
            if (row.size < columnCount) {
                repeat(columnCount - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
