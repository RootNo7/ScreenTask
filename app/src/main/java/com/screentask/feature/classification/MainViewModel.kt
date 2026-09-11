package com.screentask.feature.classification

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screentask.core.classifier.model.ClassificationCategory
import com.screentask.core.data.local.AppDatabase
import com.screentask.core.data.local.entity.ScreenshotEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val searchQuery: String = "",
    val selectedCategory: ClassificationCategory? = null,
    val screenshots: List<ScreenshotEntity> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).screenshotDao()
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<ClassificationCategory?>(null)

    private val screenshotListStream = searchQuery.flatMapLatest { query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            dao.getAllScreenshots()
        } else {
            val formattedQuery = "$trimmed*"
            dao.searchScreenshots(formattedQuery)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        screenshotListStream,
        searchQuery,
        selectedCategory
    ) { screenshots, query, category ->
        val filtered = if (category == null) {
            screenshots
        } else {
            screenshots.filter { it.category == category }
        }
        HomeUiState(
            searchQuery = query,
            selectedCategory = category,
            screenshots = filtered,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(category: ClassificationCategory?) {
        selectedCategory.value = category
    }

    fun deleteScreenshot(entity: ScreenshotEntity) {
        viewModelScope.launch {
            dao.deleteById(entity.id)
            runCatching {
                getApplication<Application>().contentResolver.delete(
                    Uri.parse(entity.uri), null, null
                )
            }
        }
    }
}
