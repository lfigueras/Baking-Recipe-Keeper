package com.lovely.bakingrecipes.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lovely.bakingrecipes.data.Ingredient
import com.lovely.bakingrecipes.data.IngredientUnit
import com.lovely.bakingrecipes.data.ImageStorage
import com.lovely.bakingrecipes.data.MediaItem
import com.lovely.bakingrecipes.data.MediaType
import com.lovely.bakingrecipes.data.Pastry
import com.lovely.bakingrecipes.data.Step
import com.lovely.bakingrecipes.data.formatAmount
import com.lovely.bakingrecipes.repository.PastryRepository
import com.lovely.bakingrecipes.util.Analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// One editable ingredient row in the form; amount is kept as text while typing.
data class IngredientDraft(
    val key: Long,
    val name: String = "",
    val amount: String = "",
    val unit: IngredientUnit = IngredientUnit.GRAMS,
    val amountError: Boolean = false
)

// One editable instruction row in the form.
data class StepDraft(
    val key: Long,
    val text: String = ""
)

// One photo or video attached to the recipe; existing items already live in app storage.
data class MediaDraft(
    val key: Long,
    val uri: Uri,
    val type: MediaType,
    val isExisting: Boolean
)

class AddPastryViewModel(
    application: Application,
    private val repository: PastryRepository,
    private val pastryId: Int? = null
) : AndroidViewModel(application) {

    val isEditMode: Boolean = pastryId != null

    var pastryName by mutableStateOf("")
        private set

    var category by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var servings by mutableStateOf("")
        private set

    var prepMinutes by mutableStateOf("")
        private set

    var cookMinutes by mutableStateOf("")
        private set

    var difficulty by mutableStateOf("")
        private set

    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    var nameError by mutableStateOf<String?>(null)
        private set

    // Turns true once a pastry is persisted, signalling the screen to navigate back.
    var saveComplete by mutableStateOf(false)
        private set

    private var nextIngredientKey = 0L
    val ingredients = mutableStateListOf<IngredientDraft>()

    private var nextStepKey = 0L
    val steps = mutableStateListOf<StepDraft>()

    val tags = mutableStateListOf<String>()

    // Increments only on user-initiated row adds, so the form scrolls to reveal a new row.
    var addRowTick by mutableStateOf(0)
        private set

    private var nextMediaKey = 0L
    val mediaItems = mutableStateListOf<MediaDraft>()

    // The already-stored image path, so we don't re-copy an unchanged photo when editing.
    private var existingImageUri: String? = null

    // Preserved so an edit keeps the original creation time.
    private var existingCreatedAt: Long = System.currentTimeMillis()

    // Preserved so an edit keeps the favorite flag.
    private var existingIsFavorite: Boolean = false

    // Snapshot of the initial form, used to detect unsaved changes.
    private var initialName = ""
    private var initialCategory = ""
    private var initialDescription = ""
    private var initialServings = ""
    private var initialPrep = ""
    private var initialCook = ""
    private var initialDifficulty = ""
    private var initialImage: String? = null
    private var initialIngredients: List<Triple<String, String, IngredientUnit>> = emptyList()
    private var initialSteps: List<String> = emptyList()
    private var initialTags: List<String> = emptyList()
    private var initialMedia: List<String> = emptyList()

    init {
        if (pastryId != null) {
            loadPastry(pastryId)
        }
    }

    private fun loadPastry(id: Int) {
        viewModelScope.launch {
            val data = repository.getPastryById(id).first() ?: return@launch
            pastryName = data.pastry.name
            category = data.pastry.category
            description = data.pastry.description
            existingImageUri = data.pastry.imageUri
            existingCreatedAt = data.pastry.createdAt
            existingIsFavorite = data.pastry.isFavorite
            selectedImageUri = data.pastry.imageUri?.let { Uri.parse(it) }
            ingredients.clear()
            data.ingredients.forEach { ingredient ->
                ingredients.add(
                    IngredientDraft(
                        key = nextIngredientKey++,
                        name = ingredient.name,
                        amount = formatAmount(ingredient.amount),
                        unit = ingredient.unit
                    )
                )
            }
            steps.clear()
            data.steps.sortedBy { it.position }.forEach { step ->
                steps.add(StepDraft(key = nextStepKey++, text = step.instruction))
            }

            tags.clear()
            data.tags.forEach { tags.add(it.name) }

            mediaItems.clear()
            data.media.sortedBy { it.position }.forEach { m ->
                mediaItems.add(
                    MediaDraft(
                        key = nextMediaKey++,
                        uri = Uri.parse(m.uri),
                        type = m.type,
                        isExisting = true
                    )
                )
            }

            servings = data.pastry.servings.takeIf { it > 0 }?.toString() ?: ""
            prepMinutes = data.pastry.prepMinutes.takeIf { it > 0 }?.toString() ?: ""
            cookMinutes = data.pastry.cookMinutes.takeIf { it > 0 }?.toString() ?: ""
            difficulty = data.pastry.difficulty

            captureInitialSnapshot()
        }
    }

    private fun captureInitialSnapshot() {
        initialName = pastryName
        initialCategory = category
        initialDescription = description
        initialServings = servings
        initialPrep = prepMinutes
        initialCook = cookMinutes
        initialDifficulty = difficulty
        initialImage = selectedImageUri?.toString()
        initialIngredients = ingredients
            .filter { it.name.isNotBlank() || it.amount.isNotBlank() }
            .map { Triple(it.name, it.amount, it.unit) }
        initialSteps = steps.map { it.text }.filter { it.isNotBlank() }
        initialTags = tags.toList()
        initialMedia = mediaItems.map { it.uri.toString() }
    }

    fun hasUnsavedChanges(): Boolean {
        if (pastryName != initialName) return true
        if (category != initialCategory) return true
        if (description != initialDescription) return true
        if (servings != initialServings) return true
        if (prepMinutes != initialPrep) return true
        if (cookMinutes != initialCook) return true
        if (difficulty != initialDifficulty) return true
        if (selectedImageUri?.toString() != initialImage) return true
        val currentIngredients = ingredients
            .filter { it.name.isNotBlank() || it.amount.isNotBlank() }
            .map { Triple(it.name, it.amount, it.unit) }
        if (currentIngredients != initialIngredients) return true
        val currentSteps = steps.map { it.text }.filter { it.isNotBlank() }
        if (currentSteps != initialSteps) return true
        if (tags.toList() != initialTags) return true
        if (mediaItems.map { it.uri.toString() } != initialMedia) return true
        return false
    }

    fun addPhotos(uris: List<Uri>) {
        uris.forEach { uri ->
            mediaItems.add(
                MediaDraft(key = nextMediaKey++, uri = uri, type = MediaType.PHOTO, isExisting = false)
            )
        }
    }

    fun addVideo(uri: Uri) {
        mediaItems.add(
            MediaDraft(key = nextMediaKey++, uri = uri, type = MediaType.VIDEO, isExisting = false)
        )
    }

    fun removeMedia(key: Long) {
        mediaItems.removeAll { it.key == key }
    }

    fun addTag(raw: String) {
        val name = raw.trim()
        if (name.isNotEmpty() && tags.none { it.equals(name, ignoreCase = true) }) {
            tags.add(name)
        }
    }

    fun removeTag(name: String) {
        tags.removeAll { it.equals(name, ignoreCase = true) }
    }

    fun onPastryNameChange(newName: String) {
        pastryName = newName
        if (nameError != null && newName.isNotBlank()) {
            nameError = null
        }
    }

    fun onCategoryChange(newCategory: String) {
        category = newCategory
    }

    fun onDescriptionChange(newDescription: String) {
        description = newDescription
    }

    fun onServingsChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) servings = value
    }

    fun onPrepMinutesChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) prepMinutes = value
    }

    fun onCookMinutesChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) cookMinutes = value
    }

    fun onDifficultyChange(value: String) {
        difficulty = value
    }

    fun onImageSelected(uri: Uri?) {
        selectedImageUri = uri
    }

    fun addIngredientRow() {
        ingredients.add(IngredientDraft(key = nextIngredientKey++))
        addRowTick++
    }

    fun removeIngredientRow(key: Long) {
        ingredients.removeAll { it.key == key }
    }

    fun onIngredientNameChange(key: Long, newName: String) {
        updateIngredient(key) { it.copy(name = newName) }
    }

    fun onIngredientAmountChange(key: Long, newAmount: String) {
        // Allow only digits and a single decimal point.
        if (newAmount.isNotEmpty() && !newAmount.matches(Regex("^\\d*\\.?\\d*$"))) return
        updateIngredient(key) { draft ->
            val parsed = newAmount.toDoubleOrNull()
            draft.copy(
                amount = newAmount,
                amountError = if (parsed != null && parsed > 0.0) false else draft.amountError
            )
        }
    }

    fun onIngredientUnitChange(key: Long, newUnit: IngredientUnit) {
        updateIngredient(key) { it.copy(unit = newUnit) }
    }

    private inline fun updateIngredient(key: Long, transform: (IngredientDraft) -> IngredientDraft) {
        val index = ingredients.indexOfFirst { it.key == key }
        if (index != -1) {
            ingredients[index] = transform(ingredients[index])
        }
    }

    fun addStepRow() {
        steps.add(StepDraft(key = nextStepKey++))
        addRowTick++
    }

    fun removeStepRow(key: Long) {
        steps.removeAll { it.key == key }
    }

    fun moveStepUp(key: Long) {
        val i = steps.indexOfFirst { it.key == key }
        if (i > 0) steps.add(i - 1, steps.removeAt(i))
    }

    fun moveStepDown(key: Long) {
        val i = steps.indexOfFirst { it.key == key }
        if (i in 0 until steps.size - 1) steps.add(i + 1, steps.removeAt(i))
    }

    fun onStepChange(key: Long, newText: String) {
        val index = steps.indexOfFirst { it.key == key }
        if (index != -1) {
            steps[index] = steps[index].copy(text = newText)
        }
    }

    fun savePastry() {

        if (pastryName.isBlank()) {
            nameError = "Please enter a recipe name"
            return
        }

        // A named ingredient must have an amount greater than 0.
        val validIngredients = mutableListOf<Ingredient>()
        var hasIngredientError = false
        ingredients.forEachIndexed { index, draft ->
            if (draft.name.isBlank()) {
                if (draft.amountError) ingredients[index] = draft.copy(amountError = false)
                return@forEachIndexed
            }
            val amount = draft.amount.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                ingredients[index] = draft.copy(amountError = true)
                hasIngredientError = true
            } else {
                if (draft.amountError) ingredients[index] = draft.copy(amountError = false)
                validIngredients.add(
                    Ingredient(
                        pastryId = 0,
                        name = draft.name.trim(),
                        amount = amount,
                        unit = draft.unit
                    )
                )
            }
        }

        if (hasIngredientError) return

        val validSteps = steps
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
            .map { Step(pastryId = 0, position = 0, instruction = it) }

        viewModelScope.launch {
            val imageUriString = resolveImageUri()
            val resolvedMedia = resolveMedia()

            if (pastryId == null) {
                val pastry = Pastry(
                    name = pastryName.trim(),
                    category = category.trim(),
                    description = description.trim(),
                    imageUri = imageUriString,
                    servings = servings.toIntOrNull() ?: 0,
                    prepMinutes = prepMinutes.toIntOrNull() ?: 0,
                    cookMinutes = cookMinutes.toIntOrNull() ?: 0,
                    difficulty = difficulty
                )
                repository.insertPastryWithIngredients(
                    pastry, validIngredients, validSteps, tags.toList(), resolvedMedia
                )
                Analytics.recipeCreated(pastry.category, validIngredients.size, validSteps.size)
            } else {
                val pastry = Pastry(
                    id = pastryId,
                    name = pastryName.trim(),
                    category = category.trim(),
                    description = description.trim(),
                    imageUri = imageUriString,
                    servings = servings.toIntOrNull() ?: 0,
                    prepMinutes = prepMinutes.toIntOrNull() ?: 0,
                    cookMinutes = cookMinutes.toIntOrNull() ?: 0,
                    difficulty = difficulty,
                    isFavorite = existingIsFavorite,
                    createdAt = existingCreatedAt,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updatePastryWithIngredients(
                    pastry, validIngredients, validSteps, tags.toList(), resolvedMedia
                )
                Analytics.recipeEdited()

                // Remove the previous photo if it was swapped out or cleared.
                if (existingImageUri != null && existingImageUri != imageUriString) {
                    ImageStorage.deleteIfLocal(getApplication(), existingImageUri)
                }

                // Remove media files the user dropped during this edit.
                val keptUris = resolvedMedia.map { it.uri }.toSet()
                initialMedia.filter { it !in keptUris }.forEach {
                    ImageStorage.deleteIfLocal(getApplication(), it)
                }
            }

            saveComplete = true
        }
    }

    // Copies newly picked media into app storage; keeps existing items as-is.
    private suspend fun resolveMedia(): List<MediaItem> =
        mediaItems.mapNotNull { draft ->
            val uriString = if (draft.isExisting) {
                draft.uri.toString()
            } else {
                copyMediaToInternalStorage(draft.uri, draft.type)
            }
            uriString?.let {
                MediaItem(pastryId = 0, uri = it, type = draft.type, position = 0)
            }
        }

    private suspend fun copyMediaToInternalStorage(uri: Uri, type: MediaType): String? =
        withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val ext = if (type == MediaType.VIDEO) "mp4" else "jpg"
                val prefix = if (type == MediaType.VIDEO) "video" else "photo"
                val file = File(context.filesDir, "${prefix}_${System.currentTimeMillis()}_${nextMediaKey++}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                Uri.fromFile(file).toString()
            } catch (e: Exception) {
                null
            }
        }

    // Reuse the stored file when the photo is unchanged; copy only newly picked images.
    private suspend fun resolveImageUri(): String? {
        val current = selectedImageUri ?: return null
        return if (current.toString() == existingImageUri) {
            existingImageUri
        } else {
            copyImageToInternalStorage(current)
        }
    }

    // The photo picker grants only temporary access, so copy the bytes into app storage.
    private suspend fun copyImageToInternalStorage(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val file = File(context.filesDir, "pastry_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(file).toString()
            } catch (e: Exception) {
                null
            }
        }
}