package com.example.mientrega.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReminderPreferencesTest {

    private lateinit var context: Context
    private lateinit var preferencesName: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Archivo aislado para no tocar los ajustes reales de la app.
        preferencesName = "test-reminders-${UUID.randomUUID()}"
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(preferencesName)
    }

    @Test
    fun defaultValueIsFalse() {
        val preferences = SharedPreferencesReminderPreferences(context, preferencesName)

        assertFalse(preferences.enabled.value)
    }

    @Test
    fun setEnabledTrueUpdatesFlowAndPersistsAcrossInstances() {
        val preferences = SharedPreferencesReminderPreferences(context, preferencesName)

        preferences.setEnabled(true)

        assertTrue(preferences.enabled.value)
        assertTrue(
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
                .getBoolean(SharedPreferencesReminderPreferences.KEY_ENABLED, false)
        )

        val recreated = SharedPreferencesReminderPreferences(context, preferencesName)
        assertTrue(recreated.enabled.value)
    }

    @Test
    fun setEnabledFalsePersistsAcrossInstances() {
        val preferences = SharedPreferencesReminderPreferences(context, preferencesName)
        preferences.setEnabled(true)

        preferences.setEnabled(false)

        assertFalse(preferences.enabled.value)
        val recreated = SharedPreferencesReminderPreferences(context, preferencesName)
        assertFalse(recreated.enabled.value)
    }

    @Test
    fun fallbackTargetIsNullByDefault() {
        val preferences = SharedPreferencesReminderPreferences(context, preferencesName)

        assertNull(preferences.fallbackTargetMillis(42L))
    }

    @Test
    fun fallbackTargetPersistsWithTheDocumentedKey() {
        val preferences = SharedPreferencesReminderPreferences(context, preferencesName)
        val target = 1_800_000_000_000L

        preferences.setFallbackTargetMillis(42L, target)

        assertEquals(target, preferences.fallbackTargetMillis(42L))
        assertEquals(
            target,
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
                .getLong("fallback_target_42", 0L)
        )
        val recreated = SharedPreferencesReminderPreferences(context, preferencesName)
        assertEquals(target, recreated.fallbackTargetMillis(42L))
    }

    @Test
    fun clearingFallbackTargetRemovesOnlyThatAssignment() {
        val preferences = SharedPreferencesReminderPreferences(context, preferencesName)
        preferences.setFallbackTargetMillis(42L, 1_000L)
        preferences.setFallbackTargetMillis(43L, 2_000L)

        preferences.clearFallbackTarget(42L)

        assertNull(preferences.fallbackTargetMillis(42L))
        assertEquals(2_000L, preferences.fallbackTargetMillis(43L))
    }

    @Test
    fun fallbackDeliveredIsFalseByDefault() {
        val preferences = SharedPreferencesReminderPreferences(context, preferencesName)

        assertFalse(preferences.isFallbackDelivered(42L))
    }

    @Test
    fun fallbackDeliveredPersistsWithTheDocumentedKey() {
        val preferences = SharedPreferencesReminderPreferences(context, preferencesName)

        preferences.setFallbackDelivered(42L, true)

        assertTrue(preferences.isFallbackDelivered(42L))
        assertTrue(
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
                .getBoolean("fallback_delivered_42", false)
        )
        val recreated = SharedPreferencesReminderPreferences(context, preferencesName)
        assertTrue(recreated.isFallbackDelivered(42L))
    }

    @Test
    fun clearingFallbackDeliveredRemovesOnlyThatAssignment() {
        val preferences = SharedPreferencesReminderPreferences(context, preferencesName)
        preferences.setFallbackDelivered(42L, true)
        preferences.setFallbackDelivered(43L, true)

        preferences.clearFallbackDelivered(42L)

        assertFalse(preferences.isFallbackDelivered(42L))
        assertTrue(preferences.isFallbackDelivered(43L))
    }
}
