package com.example.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.browser.ui.BrowserScreen
import com.example.browser.ui.theme.BrowserTheme

/**
 * メインアクティビティ
 * 
 * ブラウザアプリのエントリーポイント。
 * BrowserScreenコンポーネントを表示する。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrowserTheme {
                BrowserScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}