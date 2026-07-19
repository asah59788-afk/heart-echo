package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.GeminiService
import com.example.data.JournalEntry
import com.example.data.PlayerStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    private val geminiService = GeminiService()

    val playerStats: StateFlow<PlayerStats>
    val journalEntries: StateFlow<List<JournalEntry>>

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // Current screen state: "home", "journal", "write_letter", "gallery", "story", "ai_chat"
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Story state for active visual novel choices
    private val _storyNarrative = MutableStateFlow("")
    val storyNarrative: StateFlow<String> = _storyNarrative.asStateFlow()

    private val _activeChoices = MutableStateFlow<List<StoryChoice>>(emptyList())
    val activeChoices: StateFlow<List<StoryChoice>> = _activeChoices.asStateFlow()

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
        playerStats = repository.playerStats.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerStats()
        )
        journalEntries = repository.journalEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        loadChapterStory(PlayerStats().currentChapter)
    }

    fun changeScreen(screenName: String) {
        _currentScreen.value = screenName
    }

    // Stats training exercises
    fun performActivity(activityType: String) {
        viewModelScope.launch {
            when (activityType) {
                "diary" -> {
                    repository.updateStat("sincerity", 5)
                    repository.updateStat("thoughtfulness", 2)
                }
                "mirror" -> {
                    repository.updateStat("eloquence", 5)
                    repository.updateStat("courage", 2)
                }
                "origami" -> {
                    repository.updateStat("thoughtfulness", 5)
                    repository.updateStat("sincerity", 2)
                }
                "music" -> {
                    repository.updateStat("sincerity", 4)
                    repository.updateStat("eloquence", 3)
                }
                "breathe" -> {
                    repository.updateStat("courage", 6)
                    repository.updateStat("thoughtfulness", 2)
                }
            }
        }
    }

    // Submitting a hand-crafted letter or raw thought to Gemini for analysis and rewrite
    fun submitLetter(title: String, rawContent: String) {
        if (rawContent.isBlank()) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null
            try {
                val result = geminiService.analyzeExpression(rawContent)
                
                // Add points based on Gemini rating (scaling 1-10 to points)
                val courageEarned = result.courage * 2
                val sincerityEarned = result.sincerity * 2

                repository.saveJournalEntry(
                    title = title.ifBlank { "Unspoken Thought" },
                    content = rawContent,
                    courageEarned = courageEarned,
                    sincerityEarned = sincerityEarned,
                    aiFeedback = "Sincerity: ${result.sincerity}/10 • Courage: ${result.courage}/10\n\n" +
                            "✨ Inner Voice: ${result.advice}\n\n" +
                            "✉️ Redrafted Expression:\n${result.rewrittenLetter}"
                )
                
                // Try to unlock related items based on scores
                if (result.courage >= 7) {
                    repository.unlockGalleryItem("umbrella")
                }
                if (result.sincerity >= 8) {
                    repository.unlockGalleryItem("music_tape")
                }
                if (result.sincerity >= 9 && result.courage >= 8) {
                    repository.unlockGalleryItem("love_letter")
                }

                _isAnalyzing.value = false
                _currentScreen.value = "journal"
            } catch (e: Exception) {
                _analysisError.value = e.message ?: "Failed to reach your heart's advisor. Try again."
                _isAnalyzing.value = false
            }
        }
    }

    fun deleteJournal(id: Int) {
        viewModelScope.launch {
            repository.deleteJournalById(id)
        }
    }

    fun unlockGallery(itemId: String) {
        viewModelScope.launch {
            repository.unlockGalleryItem(itemId)
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            repository.resetGame()
            loadChapterStory(0)
            _currentScreen.value = "home"
        }
    }

    fun handleStoryChoice(choice: StoryChoice) {
        viewModelScope.launch {
            // Apply rewards/costs
            if (choice.courageReward != 0) repository.updateStat("courage", choice.courageReward)
            if (choice.sincerityReward != 0) repository.updateStat("sincerity", choice.sincerityReward)
            if (choice.thoughtfulnessReward != 0) repository.updateStat("thoughtfulness", choice.thoughtfulnessReward)
            if (choice.eloquenceReward != 0) repository.updateStat("eloquence", choice.eloquenceReward)

            if (choice.unlocksItem != null) {
                repository.unlockGalleryItem(choice.unlocksItem)
            }

            if (choice.advancesChapter) {
                val current = playerStats.value.currentChapter
                repository.advanceChapter()
                loadChapterStory(current + 1)
            } else {
                // Update text to resolve choice
                _storyNarrative.value = choice.resolutionText
                _activeChoices.value = listOf(
                    StoryChoice(
                        text = "Continue",
                        resolutionText = "",
                        advancesChapter = false,
                        onSelected = {
                            loadChapterStory(playerStats.value.currentChapter)
                        }
                    )
                )
            }
        }
    }

    fun loadChapterStory(chapter: Int) {
        when (chapter) {
            0 -> {
                _storyNarrative.value = "Chapter 1: The Passing Glance\n\n" +
                        "She passes by your locker every morning at exactly 8:15 AM, holding a blue notebook. " +
                        "Her name is Alice. You've loved her quiet kindness from afar for a year, but you've never spoken. " +
                        "Today, you notice a small teardrop stain on her homework sheet. She looks overwhelmed."
                _activeChoices.value = listOf(
                    StoryChoice(
                        text = "Leave a secret sticky note: 'You've got this!' on her locker",
                        resolutionText = "You slip the sticky note onto Locker 104 when nobody is looking. " +
                                "At 8:15 AM, you watch from down the hallway. She sees it, and a tiny, gentle smile graces her lips. " +
                                "Your heart beats like a wild drum! You've unlocked the Origami Star in your Gallery.",
                        courageReward = 15,
                        thoughtfulnessReward = 10,
                        unlocksItem = "origami_star"
                    ),
                    StoryChoice(
                        text = "Do nothing and look down as she passes",
                        resolutionText = "She walks by silently. The hallway feels cold. You realize that keeping feelings locked up " +
                                "makes them heavy. Writing down your feelings might help build your courage.",
                        sincerityReward = 5,
                        courageReward = -2
                    ),
                    StoryChoice(
                        text = "Gather courage to say 'Good morning!' directly (Requires 25 Courage)",
                        resolutionText = "You take a deep breath, step out, and say: 'Good morning, Alice!' " +
                                "She looks surprised, then blushes slightly, nodding with a sweet 'Morning!'. " +
                                "The universe feels complete! You are ready for the next chapter.",
                        courageReward = 20,
                        eloquenceReward = 10,
                        advancesChapter = true,
                        requiredStat = "courage",
                        requiredStatValue = 25
                    )
                )
            }
            1 -> {
                _storyNarrative.value = "Chapter 2: The Library Helper\n\n" +
                        "It is golden hour. You are both in the quiet school library. " +
                        "She is reaching for a classic book on the high shelf, but it's just out of reach. " +
                        "Her fingers brush the spine. This is your chance."
                _activeChoices.value = listOf(
                    StoryChoice(
                        text = "Politely reach up and hand it to her",
                        resolutionText = "You step in, easily reach the book, and hand it to her. " +
                                "Your fingers brush for a brief second. 'Oh, thank you so much,' she whispers. " +
                                "You've unlocked the Leaf Bookmark in your Gallery!",
                        courageReward = 15,
                        eloquenceReward = 10,
                        unlocksItem = "library_bookmark"
                    ),
                    StoryChoice(
                        text = "Recommend a poetry book by sliding it onto her desk (Requires 30 Eloquence)",
                        resolutionText = "You write a small quote inside the library slip: 'To find oneself is to find love.' " +
                                "You place it on her desk. She opens it, reads it, and smiles, looking around to see who left it. " +
                                "Her eyes meet yours, and she smiles warmly! You are ready to advance.",
                        sincerityReward = 20,
                        eloquenceReward = 15,
                        advancesChapter = true,
                        requiredStat = "eloquence",
                        requiredStatValue = 30
                    ),
                    StoryChoice(
                        text = "Walk away, feeling too shy to intervene",
                        resolutionText = "You turn away. Another student helps her. You sit in the corner, " +
                                "listening to soft music on your phone, writing secret entries in your diary to calm your thoughts.",
                        sincerityReward = 10,
                        thoughtfulnessReward = 2
                    )
                )
            }
            2 -> {
                _storyNarrative.value = "Chapter 3: The Shared Umbrella\n\n" +
                        "A sudden summer rainstorm pours down after school. " +
                        "Alice is standing under the school entrance, shivering slightly, with no umbrella. " +
                        "You have a single umbrella. Your hands are trembling."
                _activeChoices.value = listOf(
                    StoryChoice(
                        text = "Offer her your umbrella and run home in the rain",
                        resolutionText = "You thrust the umbrella into her hands: 'Take it!' and dash off into the downpour. " +
                                "You get soaked, but when you look back, she is holding the umbrella close, staring after you with " +
                                "deep concern and sweetness in her eyes. You feel incredibly warm.",
                        thoughtfulnessReward = 25,
                        sincerityReward = 15
                    ),
                    StoryChoice(
                        text = "Offer to walk together under the umbrella (Requires 50 Courage)",
                        resolutionText = "You muster every ounce of courage: 'Would you... like to walk together?' " +
                                "She looks up, smiles brightly: 'I'd love that. Thank you.' " +
                                "Under the small dome of the umbrella, the rain sounds like music. " +
                                "She notices your wet shoulder because you are holding the umbrella over her. She gently pulls you closer. " +
                                "You've unlocked the Shared Umbrella in your Gallery!",
                        courageReward = 30,
                        thoughtfulnessReward = 20,
                        unlocksItem = "umbrella",
                        advancesChapter = true,
                        requiredStat = "courage",
                        requiredStatValue = 50
                    ),
                    StoryChoice(
                        text = "Wait in the lobby silently until the rain stops",
                        resolutionText = "You wait in silence. The rain eventually tapers off. You walk home alone, " +
                                "wondering if you will ever find the strength to share her space.",
                        sincerityReward = 5,
                        courageReward = 2
                    )
                )
            }
            3 -> {
                _storyNarrative.value = "Chapter 4: Under the Stars\n\n" +
                        "The summer festival night is warm. Fireworks light up the sky in vibrant pinks and golds. " +
                        "You stand next to her at the hilltop overlook. The cool evening breeze carries the scent of summer. " +
                        "This is the final moment. Will you express your true feelings?"
                _activeChoices.value = listOf(
                    StoryChoice(
                        text = "Hand her your final handwritten letter (Requires 60 Sincerity, 60 Courage)",
                        resolutionText = "You draw the sealed, beautiful love letter from your pocket. " +
                                "With a steady hand born of your journey, you say: 'Alice, these are my true feelings. Thank you for making my year beautiful.' " +
                                "She takes the letter, reads it under the glow of the lanterns, and looks at you. " +
                                "A soft blush fills her cheeks. 'I never knew you felt this way... I want to get to know you better too, under the daylight.' " +
                                "The onesided love starts to echo back. You've completed the game!",
                        courageReward = 40,
                        sincerityReward = 40,
                        unlocksItem = "love_letter"
                    ),
                    StoryChoice(
                        text = "Simply thank her for being a wonderful friend",
                        resolutionText = "You look at the fireworks together. 'Thanks for being such a wonderful person, Alice.' " +
                                "She smiles, looking at you: 'You too.' " +
                                "Though unspoken, you have found the peace of self-expression and are no longer afraid.",
                        thoughtfulnessReward = 20,
                        eloquenceReward = 20
                    ),
                    StoryChoice(
                        text = "Reset the Story to relive the memories",
                        resolutionText = "Let's start the journey again, finding new paths to build courage.",
                        advancesChapter = false,
                        onSelected = { resetGame() }
                    )
                )
            }
        }
    }
}

data class StoryChoice(
    val text: String,
    val resolutionText: String,
    val courageReward: Int = 0,
    val sincerityReward: Int = 0,
    val thoughtfulnessReward: Int = 0,
    val eloquenceReward: Int = 0,
    val unlocksItem: String? = null,
    val advancesChapter: Boolean = false,
    val requiredStat: String? = null,
    val requiredStatValue: Int = 0,
    val onSelected: (() -> Unit)? = null
)
