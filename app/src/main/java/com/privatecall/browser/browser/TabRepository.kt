package com.privatecall.browser.browser

import com.privatecall.browser.browser.model.BrowserTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TabRepository {
    private var nextId = 1L
    private val normalTab = BrowserTab(id = nextId++, url = "https://www.example.com")
    private val privateTab = BrowserTab(id = nextId++, url = "https://www.example.com", private = true)
    private val _tabs = MutableStateFlow(listOf(normalTab))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()
    private val _activeTabId = MutableStateFlow(normalTab.id)
    val activeTabId: StateFlow<Long> = _activeTabId.asStateFlow()

    val activeTab: BrowserTab?
        get() = _tabs.value.firstOrNull { it.id == _activeTabId.value }

    fun openTab(private: Boolean = false): BrowserTab {
        val tab = BrowserTab(id = nextId++, private = private)
        _tabs.update { it + tab }
        _activeTabId.value = tab.id
        return tab
    }

    fun closeActive(): Boolean {
        val tabs = _tabs.value
        val activeId = _activeTabId.value
        val active = tabs.firstOrNull { it.id == activeId } ?: return false
        if (tabs.size == 1) {
            val replacement = BrowserTab(id = active.id, private = active.private)
            _tabs.value = listOf(replacement)
            _activeTabId.value = replacement.id
            return active.private
        }
        val index = tabs.indexOfFirst { it.id == activeId }
        val remaining = tabs.filterNot { it.id == activeId }
        _tabs.value = remaining
        _activeTabId.value = remaining[maxOf(0, index - 1)].id
        return active.private
    }

    fun selectTab(id: Long) {
        if (_tabs.value.any { it.id == id }) _activeTabId.value = id
    }

    fun updateActive(update: (BrowserTab) -> BrowserTab) {
        val id = _activeTabId.value
        _tabs.update { all -> all.map { tab -> if (tab.id == id) update(tab) else tab } }
    }

    fun clearAll() {
        val fresh = BrowserTab(id = nextId++, private = false)
        _tabs.value = listOf(fresh)
        _activeTabId.value = fresh.id
    }

    fun setPrivateSessionEnabled(enabled: Boolean) {
        val tabs = _tabs.value
        if (enabled) {
            val existing = tabs.firstOrNull { it.private }
            if (existing != null) {
                _activeTabId.value = existing.id
            } else {
                openTab(private = true)
            }
        } else {
            val privateTabs = tabs.filter { it.private }
            if (privateTabs.isNotEmpty()) {
                _tabs.update { all -> all.filterNot { it.private }.ifEmpty {
                    listOf(BrowserTab(id = nextId++, private = false))
                } }
                _activeTabId.value = _tabs.value.first().id
            }
        }
    }

    fun privateTabsCount(): Int = _tabs.value.count { it.private }
}
