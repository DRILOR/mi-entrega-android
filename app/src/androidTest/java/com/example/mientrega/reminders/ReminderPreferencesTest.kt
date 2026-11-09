package com.example.mientrega.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
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
}
