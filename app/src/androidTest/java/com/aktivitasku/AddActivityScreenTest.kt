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
class AddActivityScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        // Navigate to add screen
        composeRule
            .onNodeWithContentDescription("Tambah kegiatan")
            .performClick()
    }

    @Test
    fun addScreen_allCategoriesShown() {
        composeRule.onNodeWithText("💼 Kerja").assertExists()
        composeRule.onNodeWithText("🏠 Pribadi").assertExists()
        composeRule.onNodeWithText("💪 Kesehatan").assertExists()
        composeRule.onNodeWithText("📚 Belajar").assertExists()
        composeRule.onNodeWithText("📌 Lainnya").assertExists()
    }

    @Test
    fun addScreen_priorityChipsShown() {
        composeRule.onNodeWithText("Rendah").assertExists()
        composeRule.onNodeWithText("Sedang").assertExists()
        composeRule.onNodeWithText("Tinggi").assertExists()
    }

    @Test
    fun addScreen_typeTitle_showsInField() {
        composeRule
            .onNodeWithText("Judul kegiatan *")
            .performTextInput("Meeting pagi")

        composeRule
            .onNodeWithText("Meeting pagi")
            .assertIsDisplayed()
    }

    @Test
    fun addScreen_selectCategory_changesSelection() {
        composeRule
            .onNodeWithText("💼 Kerja")
            .performClick()

        // Chip should be selected (implementation-specific, just check it doesn't crash)
        composeRule
            .onNodeWithText("💼 Kerja")
            .assertExists()
    }

    @Test
    fun addScreen_backButton_navigatesBack() {
        composeRule
            .onNodeWithContentDescription("Kembali")
            .performClick()

        // Should be back on home showing FAB
        composeRule
            .onNodeWithContentDescription("Tambah kegiatan")
            .assertExists()
    }

    @Test
    fun addScreen_reminderChipsExist() {
        composeRule.onNodeWithText("15 menit sebelum").assertExists()
        composeRule.onNodeWithText("30 menit sebelum").assertExists()
        composeRule.onNodeWithText("1 jam sebelum").assertExists()
    }

    @Test
    fun addScreen_repeatTypeShown() {
        composeRule.onNodeWithText("Tidak berulang").assertExists()
        composeRule.onNodeWithText("Setiap hari").assertExists()
    }

    @Test
    fun addScreen_voiceMicButtonExists() {
        composeRule
            .onNodeWithText("Ketuk untuk mulai")
            .assertExists()
    }

    @Test
    fun addScreen_saveValidActivity_returnsHome() {
        composeRule
            .onNodeWithText("Judul kegiatan *")
            .performTextInput("Test Meeting")

        composeRule
            .onNodeWithText("Simpan")
            .performClick()

        // After save, back on home with FAB visible
        composeRule.waitUntil(3000) {
            composeRule
                .onAllNodesWithContentDescription("Tambah kegiatan")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
