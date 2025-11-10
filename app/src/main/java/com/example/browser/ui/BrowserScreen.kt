
package com.example.browser.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.browser.model.TabInfo
import com.example.browser.viewmodel.BrowserViewModel

/**
 * ブラウザのメイン画面。1つのAndroidView(WebView)だけを描画し、タブによって差し替える。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsState()
    val selectedTabId by viewModel.selectedTabId.collectAsState()
    val selectedTab: TabInfo? = tabs.firstOrNull { it.id == selectedTabId }

    LaunchedEffect(Unit) {
        WebView.setWebContentsDebuggingEnabled(true)
    }

    var canGoBack by remember(selectedTabId) { mutableStateOf(false) }
    var canGoForward by remember(selectedTabId) { mutableStateOf(false) }
    val navigationStateCallback = rememberUpdatedState<(Boolean, Boolean) -> Unit> { back, forward ->
        canGoBack = back
        canGoForward = forward
    }
    val activeWebViewState = remember(selectedTabId) { mutableStateOf<WebView?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TabBar(
            tabs = tabs,
            selectedTabId = selectedTabId,
            onSelectTab = viewModel::selectTab,
            onCloseTab = viewModel::closeTab,
            onAddTab = { viewModel.addNewTab() },
            modifier = Modifier.fillMaxWidth()
        )

        BrowserControls(
            canGoBack = canGoBack && activeWebViewState.value != null,
            canGoForward = canGoForward && activeWebViewState.value != null,
            onBack = {
                activeWebViewState.value?.let { webView ->
                    if (webView.canGoBack()) {
                        webView.goBack()
                        webView.post {
                            navigationStateCallback.value(webView.canGoBack(), webView.canGoForward())
                        }
                    }
                }
            },
            onForward = {
                activeWebViewState.value?.let { webView ->
                    if (webView.canGoForward()) {
                        webView.goForward()
                        webView.post {
                            navigationStateCallback.value(webView.canGoBack(), webView.canGoForward())
                        }
                    }
                }
            },
            onReload = {
                activeWebViewState.value?.reload()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
        ) {
            if (selectedTab != null) {
                key(selectedTab.id) {
                    val tabId = selectedTab.id
                    val navigationCallback = navigationStateCallback
                    val webView = remember(tabId) {
                        viewModel.getOrCreateWebView(tabId) {
                            createConfiguredWebView(
                                context = context,
                                tabId = tabId,
                                viewModel = viewModel,
                                onNavigationStateChanged = { back, forward ->
                                    navigationCallback.value(back, forward)
                                }
                            )
                        }
                    }

                    LaunchedEffect(tabId, webView) {
                        configureWebView(
                            webView = webView,
                            tabId = tabId,
                            viewModel = viewModel,
                            onNavigationStateChanged = { back, forward ->
                                navigationCallback.value(back, forward)
                            }
                        )
                        navigationCallback.value(webView.canGoBack(), webView.canGoForward())
                        if (webView.url != selectedTab.url) {
                            webView.loadUrl(selectedTab.url)
                        }
                        activeWebViewState.value = webView
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView
                        },
                        update = { view ->
                            if (view.url != selectedTab.url) {
                                view.loadUrl(selectedTab.url)
                            }
                        }
                    )

                    DisposableEffect(tabId) {
                        onDispose {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            if (activeWebViewState.value === webView) {
                                activeWebViewState.value = null
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "タブがありません",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * WebViewを生成して共通設定を適用する。
 */
private fun createConfiguredWebView(
    context: Context,
    tabId: String,
    viewModel: BrowserViewModel,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit
): WebView = WebView(context).also {
    configureWebView(
        webView = it,
        tabId = tabId,
        viewModel = viewModel,
        onNavigationStateChanged = onNavigationStateChanged
    )
}

/**
 * 既存のWebViewへ共通設定・コールバックを適用する。
 */
private fun configureWebView(
    webView: WebView,
    tabId: String,
    viewModel: BrowserViewModel,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit
) {
    with(webView.settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
    }

    fun notifyNavigationState() {
        onNavigationStateChanged(webView.canGoBack(), webView.canGoForward())
    }

    webView.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (url != null) {
                viewModel.updateTabUrl(tabId, url)
            }
            notifyNavigationState()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (url != null) {
                viewModel.updateTabUrl(tabId, url)
            }
            notifyNavigationState()
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            if (!title.isNullOrBlank()) {
                viewModel.updateTabTitle(tabId, title)
            }
        }
    }

    notifyNavigationState()
}

/**
 * 進む・戻る・再読み込みのブラウザ操作ボタン。
 */
@Composable
private fun BrowserControls(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, enabled = canGoBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "戻る"
            )
        }
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "進む"
            )
        }
        IconButton(onClick = onReload) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "再読み込み"
            )
        }
    }
}


/**
 * WebViewの共通初期化処理。
 */
private fun createConfiguredWebView(
    context: android.content.Context,
    tabId: String,
    viewModel: BrowserViewModel
): WebView = WebView(context).apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.setSupportZoom(true)
    settings.builtInZoomControls = true
    settings.displayZoomControls = false

    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (url != null) {
                viewModel.updateTabUrl(tabId, url)
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (url != null) {
                viewModel.updateTabUrl(tabId, url)
            }
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            if (!title.isNullOrBlank()) {
                viewModel.updateTabTitle(tabId, title)
            }
        }
    }
}

