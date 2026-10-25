package com.mishr.habittracker.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mishr.habittracker.domain.result.isSuccess
import com.mishr.habittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(manifest=Config.NONE)
class UserPreferencesDataStoreTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var userPreferencesDataStore: UserPreferencesDataStore
    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { testContext.preferencesDataStoreFile("test_user_preferences") }
        )
        userPreferencesDataStore = UserPreferencesDataStore(dataStore)
    }

    @After
    fun teardown() {
        runBlocking {
            // Clear all data after test
            dataStore.edit { it.clear() }
            File(testContext.filesDir, "datastore/test_user_preferences.preferences_pb").delete()
        }
    }

    @Test
    fun defaultThemeModeIsSystemWhenNotSet() {
        runBlocking {
            val theme = userPreferencesDataStore.themeMode.first()
            assertEquals(ThemeMode.SYSTEM, theme)
        }
    }

    @Test
    fun defaultLanguageIsEnWhenNotSet() {
        runBlocking {
            val lang = userPreferencesDataStore.languageCode.first()
            assertEquals("en", lang)
        }
    }

    @Test
    fun canSetAndReadThemeMode() {
        runBlocking {
            val result = userPreferencesDataStore.setThemeMode(ThemeMode.DARK)
            assertTrue(result.isSuccess())

            val theme = userPreferencesDataStore.themeMode.first()
            assertEquals(ThemeMode.DARK, theme)
        }
    }

    @Test
    fun canSetAndReadLanguageCode() {
        runBlocking {
            val result = userPreferencesDataStore.setLanguageCode("fr")
            assertTrue(result.isSuccess())

            val lang = userPreferencesDataStore.languageCode.first()
            assertEquals("fr", lang)
        }
    }
}
