package com.privatecall.browser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabRepositoryTest {
    @Test
    fun privateTabLifecycle() {
        val repository = TabRepository()
        repository.openTab(private = true)
        assertEquals(1, repository.privateTabsCount())
        assertTrue(repository.activeTab?.private == true)

        repository.setPrivateSessionEnabled(false)
        assertEquals(0, repository.privateTabsCount())
    }

    @Test
    fun closingLastTabCreatesReplacement() {
        val repository = TabRepository()
        val privateClosed = repository.closeActive()
        assertEquals(1, repository.tabs.value.size)
        assertEquals(false, privateClosed)
    }
}
