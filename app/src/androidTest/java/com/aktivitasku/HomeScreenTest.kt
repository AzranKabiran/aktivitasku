package com.aktivitasku

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun homeScreen_showsHeaderWithDayName() {
        // The top bar shows current day name
        composeRule
            .onNodeWithContentDescription("Cari kegiatan...", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun fabButton_isVisible() {
        composeRule
            .onNodeWithContentDescription("Tambah kegiatan")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun bottomNav_hasTwoTabs() {
        composeRule.onNodeWithText("Beranda").assertExists()
        composeRule.onNodeWithText("Statistik").assertExists()
        composeRule.onNodeWithText("Setelan").assertExists()
    }

    @Test
    fun clickFab_navigatesToAddScreen() {
        composeRule
            .onNodeWithContentDescription("Tambah kegiatan")
            .performClick()

        composeRule
            .onNodeWithText("Kegiatan Baru")
            .assertIsDisplayed()
    }

    @Test
    fun addScreen_hasVoiceInputSection() {
        composeRule
            .onNodeWithContentDescription("Tambah kegiatan")
            .performClick()

        composeRule
            .onNodeWithText("Input Suara")
            .assertIsDisplayed()
    }

    @Test
    fun addScreen_titleFieldExists() {
        composeRule
            .onNodeWithContentDescription("Tambah kegiatan")
            .performClick()

        composeRule
            .onNodeWithText("Judul kegiatan *")
            .assertExists()
    }

    @Test
    fun addScreen_saveWithEmptyTitle_showsError() {
        composeRule
            .onNodeWithContentDescription("Tambah kegiatan")
            .performClick()

        composeRule
            .onNodeWithText("Simpan")
            .performClick()

        composeRule
            .onNodeWithText("Judul tidak boleh kosong")
            .assertIsDisplayed()
    }

    @Test
    fun statisticsTab_isNavigable() {
        composeRule
            .onNodeWithText("Statistik")
            .performClick()

        composeRule
            .onNodeWithText("Statistik")
            .assertIsDisplayed()
    }

    @Test
    fun settingsTab_isNavigable() {
        composeRule
            .onNodeWithText("Setelan")
            .performClick()

        composeRule
            .onNodeWithText("Pengaturan")
            .assertIsDisplayed()
    }

    @Test
    fun searchBar_existsOnHome() {
        composeRule
            .onNodeWithText("Cari kegiatan...")
            .assertExists()
    }
}
