package com.screentask.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.screentask.core.classifier.model.ClassificationCategory
import com.screentask.core.classifier.model.SuggestedAction
import com.screentask.core.data.local.dao.ScreenshotDao
import com.screentask.core.data.local.entity.ScreenshotEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ScreenshotDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.screenshotDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndFetchScreenshotByUri() = runBlocking {
        val entity = ScreenshotEntity(
            uri = "content://media/external/images/media/1001",
            category = ClassificationCategory.RECEIPT_OR_FINANCIAL,
            suggestedAction = SuggestedAction.KEEP,
            summary = "Coffee shop receipt",
            extractedText = "Total: $5.50",
            reminderTimestamp = null,
            confidenceScore = 0.98f
        )

        dao.insertScreenshot(entity)

        val fetched = dao.getScreenshotByUri(entity.uri)
        assertNotNull(fetched)
        assertEquals(entity.category, fetched?.category)
        assertEquals(entity.summary, fetched?.summary)
    }

    @Test
    fun fullTextSearchReturnsMatchingQuery() = runBlocking {
        val entity1 = ScreenshotEntity(
            uri = "content://media/external/images/media/1002",
            category = ClassificationCategory.SCHEDULE_OR_CALENDAR,
            suggestedAction = SuggestedAction.REMIND,
            summary = "Team sync calendar event",
            extractedText = "Meeting at 3 PM on Friday",
            reminderTimestamp = 1780000000000L,
            confidenceScore = 0.9f
        )

        val entity2 = ScreenshotEntity(
            uri = "content://media/external/images/media/1003",
            category = ClassificationCategory.MEDIA_OR_MEME,
            suggestedAction = SuggestedAction.DISCARD,
            summary = "Funny cat picture",
            extractedText = "When code compiles first try",
            reminderTimestamp = null,
            confidenceScore = 0.8f
        )

        dao.insertScreenshot(entity1)
        dao.insertScreenshot(entity2)

        val searchResults = dao.searchScreenshots("calendar*").first()

        assertEquals(1, searchResults.size)
        assertEquals(entity1.uri, searchResults[0].uri)
    }

    @Test
    fun deleteByUriRemovesEntityFromDatabase() = runBlocking {
        val entity = ScreenshotEntity(
            uri = "content://media/external/images/media/1004",
            category = ClassificationCategory.TEXT_DOCUMENT_OR_NOTE,
            suggestedAction = SuggestedAction.KEEP,
            summary = "Shopping list note",
            extractedText = "Milk, Eggs, Bread",
            reminderTimestamp = null,
            confidenceScore = 0.85f
        )

        dao.insertScreenshot(entity)
        dao.deleteByUri(entity.uri)

        val fetched = dao.getScreenshotByUri(entity.uri)
        assertEquals(null, fetched)
    }
}