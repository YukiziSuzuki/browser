package com.example.browser.viewmodel

import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.model.TabInfo
import java.util.LinkedHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ブラウザアプリの状態を管理するViewModel。
 *
 * - タブの一覧と選択中のタブIDをStateFlowで公開
 * - WebViewの生成/キャッシュ/破棄を一元管理
 */
class BrowserViewModel : ViewModel() {

    companion object {
        private const val MAX_CACHE_SIZE = 4
        private const val DEFAULT_URL = "https://www.google.com/"
    }

    private val _tabs = MutableStateFlow<List<TabInfo>>(emptyList())
    val tabs: StateFlow<List<TabInfo>> = _tabs.asStateFlow()

    private val _selectedTabId = MutableStateFlow<String?>(null)
    val selectedTabId: StateFlow<String?> = _selectedTabId.asStateFlow()

    // WebViewをLRU方式で最大4件まで保持するキャッシュ
    private val webViewCache = object : LinkedHashMap<String, WebView>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WebView>?): Boolean {
            return if (size > MAX_CACHE_SIZE) {
                eldest?.value?.let { releaseWebView(it) }
                true
            } else {
                false
            }
        }
    }

    init {
        // 起動時は1つ目のタブを開く
        addNewTab()
    }

    /**
     * 新規タブを追加し、選択状態にする。
     */
    fun addNewTab(initialUrl: String = DEFAULT_URL) {
        viewModelScope.launch {
            val newTab = TabInfo.create(initialUrl)
            _tabs.value = _tabs.value + newTab
            _selectedTabId.value = newTab.id
        }
    }

    /**
     * 指定したタブを削除する。
     */
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            val currentTabs = _tabs.value
            if (currentTabs.size <= 1) {
                // 最後の1枚を削除した場合は初期タブを再度作成
                replaceWithSingleDefaultTab()
                return@launch
            }

            val closingIndex = currentTabs.indexOfFirst { it.id == tabId }
            if (closingIndex == -1) return@launch

            val updatedTabs = currentTabs.filterNot { it.id == tabId }
            _tabs.value = updatedTabs

            if (_selectedTabId.value == tabId) {
                val newIndex = closingIndex.coerceAtMost(updatedTabs.lastIndex)
                _selectedTabId.value = updatedTabs.getOrNull(newIndex)?.id
            }

            webViewCache.remove(tabId)?.let { releaseWebView(it) }
        }
    }

    /**
     * 指定したタブを選択状態にする。
     */
    fun selectTab(tabId: String) {
        _selectedTabId.value = tabId
    }

    /**
     * 指定したタブのURLを更新する。
     * WebViewのコールバックから呼び出される想定。
     */
    fun updateTabUrl(tabId: String, url: String) {
        viewModelScope.launch {
            _tabs.value = _tabs.value.map { tab ->
                if (tab.id == tabId && tab.url != url) tab.copy(url = url) else tab
            }
        }
    }

    /**
     * 指定したタブのタイトルを更新する。
     */
    fun updateTabTitle(tabId: String, title: String) {
        viewModelScope.launch {
            _tabs.value = _tabs.value.map { tab ->
                if (tab.id == tabId && tab.title != title) tab.copy(title = title) else tab
            }
        }
    }

    /**
     * 選択されたタブに対応するWebViewを取得する。
     * キャッシュに存在しない場合はfactoryを用いて新規作成する。
     */
    fun getOrCreateWebView(tabId: String, factory: () -> WebView): WebView {
        return synchronized(webViewCache) {
            webViewCache[tabId] ?: factory().also { webView ->
                webViewCache[tabId] = webView
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        synchronized(webViewCache) {
            webViewCache.values.forEach { releaseWebView(it) }
            webViewCache.clear()
        }
    }

    private fun replaceWithSingleDefaultTab() {
        // 既存のタブとWebViewをすべて破棄
        webViewCache.values.forEach { releaseWebView(it) }
        webViewCache.clear()

        val defaultTab = TabInfo.create(DEFAULT_URL)
        _tabs.value = listOf(defaultTab)
        _selectedTabId.value = defaultTab.id
    }

    private fun releaseWebView(webView: WebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }
}

