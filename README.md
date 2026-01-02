# SmileScore

MediaPipe Face Landmarkerを使用して、リアルタイムで笑顔スコアを分析するAndroidアプリです。

## 機能

- フロントカメラでリアルタイム顔検出
- 笑顔スコア（0〜100）の表示
- 468点の顔ランドマーク描画
- 信号機カラーによるスコアの視覚化
  - 緑: 67〜100（笑顔）
  - 黄: 34〜66（中間）
  - 赤: 0〜33（無表情）

## スクリーンショット

```
┌─────────────────────────────────────────┐
│              Smile Score                │
│        MediaPipe Face Landmarker        │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │                                   │  │
│  │         カメラ映像                │  │
│  │      （顔ランドマーク描画）        │  │
│  │                                   │  │
│  └───────────────────────────────────┘  │
│                                         │
│             笑顔スコア                  │
│  ┌───────────────────────────────────┐  │
│  │███████████████░░░░░░░░░░░░░░░░░░░│  │
│  └───────────────────────────────────┘  │
│                 75                      │
│                                         │
│    [カメラを開始]    [カメラを停止]      │
└─────────────────────────────────────────┘
```

## 必要要件

- Android 7.0 (API 24) 以上
- フロントカメラ搭載デバイス

## ビルド方法

```bash
# デバッグビルド
./gradlew assembleDebug

# 実機にインストール
./gradlew installDebug
```

## 技術スタック

| カテゴリ | 技術 |
|---------|------|
| 言語 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material Design 3 |
| カメラ | CameraX 1.3.4 |
| 顔検出 | MediaPipe Tasks Vision 0.10.14 |
| パーミッション | Accompanist Permissions 0.34.0 |

## アーキテクチャ

```
jp.gesource.sample.smilescore/
├── MainActivity.kt              # エントリーポイント
├── camera/
│   └── CameraManager.kt         # CameraX管理
├── ml/
│   ├── FaceLandmarkerHelper.kt  # MediaPipeラッパー
│   └── SmileScoreCalculator.kt  # スコア計算
└── ui/
    ├── component/               # UIコンポーネント
    ├── screen/                  # 画面
    └── theme/                   # テーマ
```

## ライセンス

This project is licensed under the MIT License.
