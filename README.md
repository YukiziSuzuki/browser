# Browser App

Android向けのマルチタブブラウザアプリケーション。Jetpack ComposeとMVVMアーキテクチャを使用して実装されています。

## 概要

このアプリは、複数のタブでWebページを閲覧できるAndroidブラウザです。各タブは独立したWebViewインスタンスで動作し、Chrome風のタブUIを提供します。

## 主な機能

- **マルチタブ対応**: 複数のタブを同時に開いて、Webページを閲覧できます
- **タブ管理**: タブの追加・削除・切り替えが可能です
- **WebViewキャッシュ**: 最大4つのWebViewインスタンスをLRU（Least Recently Used）方式でキャッシュし、メモリ効率を最適化します
- **ナビゲーション**: 戻る・進む・再読み込みボタンでWebページを操作できます

## アーキテクチャ

このアプリは**MVVM（Model-View-ViewModel）アーキテクチャ**に基づいて設計されています。

### ディレクトリ構造

```
app/src/main/java/com/example/browser/
├── model/
│   └── TabInfo.kt              # タブ情報を保持するデータクラス
├── viewmodel/
│   └── BrowserViewModel.kt     # タブ管理とWebViewキャッシュのロジック
├── ui/
│   ├── BrowserScreen.kt        # メイン画面コンポーネント
│   └── TabBar.kt               # タブバーUIコンポーネント
└── MainActivity.kt             # アプリのエントリーポイント
```

### 各コンポーネントの役割

#### Model (`TabInfo.kt`)
- タブの基本情報（ID、URL、タイトル）を保持するデータクラス
- 各タブは一意のUUIDで識別されます

#### ViewModel (`BrowserViewModel.kt`)
- タブの一覧と選択状態を`StateFlow`で管理
- WebViewインスタンスを最大4つまでLRUキャッシュで保持
- タブの追加・削除・選択・更新ロジックを実装
- WebViewのライフサイクル管理（生成・破棄）

#### View (`BrowserScreen.kt`, `TabBar.kt`)
- **BrowserScreen**: メイン画面。1つの`AndroidView`でWebViewを表示し、タブ切り替え時に差し替えます
- **TabBar**: Chrome風のタブバーUI。タブ一覧と新規タブ追加ボタンを表示します
- **BrowserControls**: 戻る・進む・再読み込みボタンを提供します

## 技術スタック

- **言語**: Kotlin
- **UIフレームワーク**: Jetpack Compose
- **アーキテクチャ**: MVVM
- **非同期処理**: Kotlin Coroutines + Flow
- **Web表示**: Android WebView

## 主な実装の特徴

### WebViewキャッシュ管理

`BrowserViewModel`は`LinkedHashMap`を使用してLRUキャッシュを実装しています：

- 最大4つのWebViewインスタンスを保持
- 最も古く使用されていないWebViewが自動的に破棄される
- タブ切り替え時にキャッシュから即座に復元可能

### 単一AndroidViewアプローチ

画面には1つの`AndroidView`のみを表示し、タブ切り替え時にWebViewインスタンスを差し替えます：

- メモリ効率の向上
- UIの一貫性の維持
- `remember`と`DisposableEffect`による適切なライフサイクル管理

### 状態管理

- `StateFlow`を使用してタブ一覧と選択状態を管理
- Composeの`collectAsState()`でリアクティブにUIを更新
- WebViewのナビゲーション状態（戻る/進む可能か）を追跡

## 使用方法

1. **新規タブを開く**: タブバー右端の「＋」ボタンをタップ
2. **タブを切り替える**: タブバーのタブをタップ
3. **タブを閉じる**: タブ右上の「×」ボタンをタップ
4. **戻る**: ブラウザコントロールの戻るボタンをタップ
5. **進む**: ブラウザコントロールの進むボタンをタップ
6. **再読み込み**: ブラウザコントロールの更新ボタンをタップ

## デフォルト設定

- **初期URL**: `https://www.google.com/`
- **WebViewキャッシュサイズ**: 4インスタンス
- **JavaScript**: 有効
- **DOM Storage**: 有効
- **ズーム**: サポート（ビルトインコントロール非表示）

## 開発環境

- Android Studio
- Kotlin
- Jetpack Compose
- Material Design 3

## ライセンス

