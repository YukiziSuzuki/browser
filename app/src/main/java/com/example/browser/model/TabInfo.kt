package com.example.browser.model

import java.util.UUID

/**
 * タブの基本情報を保持するデータクラス。
 *
 * @property id タブを一意に識別するID
 * @property url 現在のURL
 * @property title WebViewから取得したタイトル
 */
data class TabInfo(
    val id: String,
    val url: String,
    val title: String
) {
    companion object {
        /**
         * 新規タブを生成するヘルパー。
         */
        fun create(initialUrl: String, initialTitle: String = ""): TabInfo {
            return TabInfo(
                id = UUID.randomUUID().toString(),
                url = initialUrl,
                title = initialTitle
            )
        }
    }
}

