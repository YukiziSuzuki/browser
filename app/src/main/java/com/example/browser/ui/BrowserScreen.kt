package com.example.browser.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
        ) {
            if (selectedTab != null) {
                key(selectedTab.id) {
                    val tabId = selectedTab.id
                    val webView = remember(tabId) {
                        viewModel.getOrCreateWebView(tabId) {
                            createConfiguredWebView(
                                context = context,
                                tabId = tabId,
                                viewModel = viewModel
                            )
                        }
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

