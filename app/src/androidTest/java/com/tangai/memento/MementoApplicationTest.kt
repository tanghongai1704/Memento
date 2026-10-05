package com.tangai.memento

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MementoApplicationTest {
    @Test
    fun applicationUsesExpectedPackageAndType() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals("com.tangai.memento", context.packageName)
        assertTrue(context.applicationContext is MementoApplication)
    }
}
