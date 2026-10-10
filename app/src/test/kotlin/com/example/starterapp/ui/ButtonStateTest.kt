package com.example.starterapp.ui

import com.example.starterapp.ui.components.isButtonClickable
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonStateTest {
    @Test
    fun clickable_whenEnabledAndIdle() {
        assertTrue(isButtonClickable(enabled = true, loading = false))
    }

    @Test
    fun notClickable_whileLoading() {
        assertFalse(isButtonClickable(enabled = true, loading = true))
    }

    @Test
    fun notClickable_whenDisabled() {
        assertFalse(isButtonClickable(enabled = false, loading = false))
        assertFalse(isButtonClickable(enabled = false, loading = true))
    }
}
