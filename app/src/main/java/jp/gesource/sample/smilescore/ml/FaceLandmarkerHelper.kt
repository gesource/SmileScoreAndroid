package jp.gesource.sample.smilescore.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * ============================================================================
 * MediaPipe Face Landmarkerのラッパークラス
 * ============================================================================
 *
 * 【MediaPipe Face Landmarkerとは】
 * Google製の機械学習ライブラリ「MediaPipe」の顔検出ソリューションです。
 * 1つの顔から478個の3Dランドマーク（特徴点）をリアルタイムで検出できます。
 *
 * 【このクラスの役割】
 * - Face Landmarkerモデルの初期化と設定
 * - カメラフレームからの顔検出実行
 * - 検出結果のコールバック通知
 * - リソースのライフサイクル管理
 *
 * 【Face Landmarkerが出力する主要データ】
 * 1. Face Landmarks: 顔の478個の3D座標点（目、鼻、口、輪郭など）
 * 2. Face Blendshapes: 52種類の表情係数（笑顔、眉の動き、瞬きなど）
 * 3. Facial Transformation Matrix: 顔の3D姿勢情報（オプション）
 *
 * 参考: https://developers.google.com/mediapipe/solutions/vision/face_landmarker
 */
class FaceLandmarkerHelper(
    private val context: Context,
    private val listener: FaceLandmarkerListener? = null
) {
    /**
     * MediaPipe Face Landmarkerのインスタンス
     * nullableなのは、初期化前や解放後の状態を表現するため
     */
    private var faceLandmarker: FaceLandmarker? = null

    companion object {
        private const val TAG = "FaceLandmarkerHelper"

        /**
         * 【モデルファイル】
         * app/src/main/assets/に配置されたMediaPipeモデルファイル
         * このファイルには顔検出用の機械学習モデルが含まれています（約3.7MB）
         *
         * モデルは以下からダウンロード可能:
         * https://developers.google.com/mediapipe/solutions/vision/face_landmarker#models
         */
        private const val MODEL_FILE = "face_landmarker.task"

        /**
         * 【顔検出の信頼度しきい値】
         * 値の範囲: 0.0〜1.0
         *
         * MIN_FACE_DETECTION_CONFIDENCE:
         *   - 顔を最初に検出する際の信頼度しきい値
         *   - 低くすると検出されやすくなるが、誤検出も増える
         *   - 推奨値: 0.5
         *
         * MIN_FACE_PRESENCE_CONFIDENCE:
         *   - 検出された顔が「本当に顔である」と判断する信頼度
         *   - トラッキング中に顔が存在し続けているかの判定に使用
         *
         * MIN_TRACKING_CONFIDENCE:
         *   - フレーム間で同じ顔を追跡する際の信頼度しきい値
         *   - 高くするとトラッキングが途切れやすくなるが精度向上
         */
        private const val MIN_FACE_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_FACE_PRESENCE_CONFIDENCE = 0.5f
        private const val MIN_TRACKING_CONFIDENCE = 0.5f

        /**
         * 【検出する顔の最大数】
         * このサンプルでは1人の顔のみを対象とするため1に設定
         * 複数人の顔を検出したい場合は値を増やす
         */
        private const val NUM_FACES = 1
    }

    /**
     * 【リスナーインターフェース】
     * 顔検出結果やエラーを呼び出し元に通知するためのコールバック
     *
     * onResults: 検出成功時に呼ばれる。結果と推論時間を受け取る
     * onError: エラー発生時に呼ばれる。エラーメッセージを受け取る
     */
    interface FaceLandmarkerListener {
        fun onResults(result: FaceLandmarkerResult, inferenceTime: Long)
        fun onError(error: String)
    }

    /**
     * ============================================================================
     * Face Landmarkerの初期化
     * ============================================================================
     *
     * 【初期化の流れ】
     * 1. BaseOptions: モデルファイルのパスなど基本設定
     * 2. FaceLandmarkerOptions: 検出パラメータやコールバックを設定
     * 3. createFromOptions: 設定を元にFaceLandmarkerインスタンスを生成
     *
     * 【注意点】
     * - 初期化は重い処理のため、メインスレッドで実行するとUIがブロックされる
     * - 通常はバックグラウンドスレッドで実行することを推奨
     */
    fun setupFaceLandmarker() {
        try {
            /**
             * 【BaseOptions】
             * MediaPipeタスク共通の基本設定
             * - setModelAssetPath: assetsフォルダ内のモデルファイルパス
             * - setDelegate: 推論に使用するハードウェア（CPU/GPU）を指定可能
             */
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_FILE)
                .build()

            /**
             * 【FaceLandmarkerOptions】
             * Face Landmarker固有の設定
             */
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)

                /**
                 * 【RunningMode - 実行モード】
                 *
                 * LIVE_STREAM（本アプリで使用）:
                 *   - カメラからのリアルタイム映像用
                 *   - 非同期処理（detectAsync）を使用
                 *   - 結果はコールバックで受け取る
                 *   - フレームは時系列順に処理される必要がある
                 *
                 * IMAGE:
                 *   - 単一画像の処理用
                 *   - 同期処理（detect）を使用
                 *
                 * VIDEO:
                 *   - 録画済み動画の処理用
                 *   - 同期処理（detectForVideo）を使用
                 *   - タイムスタンプを指定して処理
                 */
                .setRunningMode(RunningMode.LIVE_STREAM)

                // 検出する顔の最大数
                .setNumFaces(NUM_FACES)

                // 信頼度しきい値の設定
                .setMinFaceDetectionConfidence(MIN_FACE_DETECTION_CONFIDENCE)
                .setMinFacePresenceConfidence(MIN_FACE_PRESENCE_CONFIDENCE)
                .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)

                /**
                 * 【★重要★ Blend Shapesの有効化】
                 *
                 * Face Blendshapesとは:
                 *   - ARKit互換の52種類の表情係数
                 *   - 各係数は0.0（無し）〜1.0（最大）の値
                 *
                 * 笑顔検出に使用する主なBlendshapes:
                 *   - mouthSmileLeft: 左口角の上がり具合
                 *   - mouthSmileRight: 右口角の上がり具合
                 *
                 * その他の例:
                 *   - eyeBlinkLeft/Right: 瞬き
                 *   - browDownLeft/Right: 眉を下げる
                 *   - jawOpen: 口を開ける
                 *
                 * 全52種類のBlendshapes一覧:
                 * https://developers.google.com/mediapipe/solutions/vision/face_landmarker#face_blendshapes
                 */
                .setOutputFaceBlendshapes(true)

                /**
                 * 【結果コールバック】
                 * LIVE_STREAMモードでは必須
                 * 非同期で検出結果を受け取る
                 */
                .setResultListener { result, _ ->
                    listener?.onResults(result, System.currentTimeMillis())
                }

                /**
                 * 【エラーコールバック】
                 * 検出中にエラーが発生した場合に呼ばれる
                 */
                .setErrorListener { error ->
                    Log.e(TAG, "Face Landmarker error: ${error.message}")
                    listener?.onError(error.message ?: "Unknown error")
                }
                .build()

            /**
             * 【FaceLandmarkerインスタンスの生成】
             * 設定を元にFaceLandmarkerを生成
             * この時点でモデルファイルが読み込まれ、推論の準備が完了する
             */
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.d(TAG, "Face Landmarker initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Face Landmarker: ${e.message}")
            listener?.onError("Failed to initialize: ${e.message}")
        }
    }

    /**
     * ============================================================================
     * 非同期で顔検出を実行
     * ============================================================================
     *
     * 【処理フロー】
     * 1. BitmapをMediaPipe用のMPImageに変換
     * 2. detectAsyncで非同期検出を開始
     * 3. 結果はsetResultListenerで設定したコールバックに通知される
     *
     * 【frameTimeについて】
     * - LIVE_STREAMモードではフレームの時系列順を保証するためにタイムスタンプが必要
     * - 前のフレームより小さいタイムスタンプを渡すとエラーになる
     * - 通常はSystem.currentTimeMillis()などを使用
     *
     * @param bitmap 分析対象のBitmap画像（カメラフレーム）
     * @param frameTime フレームのタイムスタンプ（ミリ秒）
     */
    fun detectAsync(bitmap: Bitmap, frameTime: Long) {
        if (faceLandmarker == null) {
            Log.w(TAG, "Face Landmarker not initialized")
            return
        }

        try {
            /**
             * 【BitmapからMPImageへの変換】
             * MediaPipeは独自のMPImage形式を使用する
             * BitmapImageBuilderを使ってAndroidのBitmapから変換
             */
            val mpImage = BitmapImageBuilder(bitmap).build()

            /**
             * 【非同期検出の実行】
             * detectAsyncはすぐに返り、結果は後でコールバックに通知される
             * これにより、メインスレッドをブロックせずに検出を実行できる
             */
            faceLandmarker?.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e(TAG, "Detection error: ${e.message}")
            listener?.onError("Detection error: ${e.message}")
        }
    }

    /**
     * ============================================================================
     * リソースを解放
     * ============================================================================
     *
     * 【重要】
     * - Face Landmarkerはネイティブリソースを使用するため、
     *   使用後は必ずclose()を呼んでリソースを解放する
     * - ActivityやFragmentのonDestroyなどで呼び出すこと
     * - 解放しないとメモリリークの原因になる
     */
    fun close() {
        try {
            faceLandmarker?.close()
            faceLandmarker = null
            Log.d(TAG, "Face Landmarker closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Face Landmarker: ${e.message}")
        }
    }
}
