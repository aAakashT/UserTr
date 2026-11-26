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
        val closedTabId = repository.activeTabId.value
        val privateClosed = repository.closeActive()
        assertEquals(1, repository.tabs.value.size)
        assertEquals(false, privateClosed)
        assertTrue(repository.activeTabId.value != closedTabId)
    }

    @Test
    fun openingNewTabSelectsBlankTab() {
        val repository = TabRepository()
        val originalId = repository.activeTabId.value

        val newTab = repository.openTab()

        assertEquals("about:blank", newTab.url)
        assertEquals(newTab.id, repository.activeTabId.value)
        assertTrue(repository.activeTabId.value != originalId)
    }

    @Test
    fun switchingTabsRestoresEachTabsCurrentUrl() {
        val repository = TabRepository()
        val firstTabId = repository.activeTabId.value
        repository.updateTab(firstTabId) { it.copy(url = "https://example.org/first") }
        val secondTab = repository.openTab()
        repository.updateTab(secondTab.id) { it.copy(url = "https://example.org/second") }

        repository.selectTab(firstTabId)

        assertEquals("https://example.org/first", repository.activeTab?.url)
        repository.selectTab(secondTab.id)
        assertEquals("https://example.org/second", repository.activeTab?.url)
    }
}
