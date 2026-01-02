package jp.gesource.sample.smilescore.ml

import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * ============================================================================
 * MediaPipe Face Landmarkerの結果から笑顔スコアを計算するユーティリティ
 * ============================================================================
 *
 * 【このクラスの役割】
 * Face Landmarkerが出力するBlend Shapes（表情係数）から
 * 笑顔の度合いを0〜100のスコアとして算出します。
 *
 * 【Face Blendshapesとは】
 * Apple ARKit互換の52種類の表情パラメータです。
 * 各パラメータは0.0（なし）〜1.0（最大）の値を持ちます。
 *
 * 【笑顔検出の原理】
 * 笑顔の特徴は「口角が上がる」ことです。
 * MediaPipeでは以下のBlendshapesがこれに対応します：
 *   - mouthSmileLeft: 左の口角の上がり具合
 *   - mouthSmileRight: 右の口角の上がり具合
 *
 * これらの平均値を取ることで、左右対称の笑顔スコアを算出します。
 *
 * 【全52種類のBlendshapes一覧】
 * https://developers.google.com/mediapipe/solutions/vision/face_landmarker#face_blendshapes
 *
 * 主要なBlendshapes:
 * - 目: eyeBlinkLeft/Right, eyeLookUpLeft/Right, eyeSquintLeft/Right
 * - 眉: browDownLeft/Right, browInnerUp, browOuterUpLeft/Right
 * - 口: mouthSmileLeft/Right, mouthFrownLeft/Right, jawOpen
 * - 頬: cheekPuff, cheekSquintLeft/Right
 * - 鼻: noseSneerLeft/Right
 */
object SmileScoreCalculator {

    /**
     * 【Blend Shapeのインデックス定義】
     *
     * MediaPipeのBlendshapesはリストとして返されます。
     * 各Blendshapeには固定のインデックス番号が割り当てられています。
     *
     * 笑顔検出に使用するインデックス:
     * - 44: mouthSmileLeft（左口角の笑顔）
     * - 45: mouthSmileRight（右口角の笑顔）
     *
     * 注意: 現在の実装ではインデックスではなく名前（categoryName）で
     * 検索しているため、これらの定数は参考情報として残しています。
     * 名前検索の方が可読性が高く、将来のAPI変更にも強いためです。
     */
    private const val MOUTH_SMILE_LEFT_INDEX = 44
    private const val MOUTH_SMILE_RIGHT_INDEX = 45

    /**
     * ============================================================================
     * FaceLandmarkerResultから笑顔スコアを計算
     * ============================================================================
     *
     * 【計算アルゴリズム】
     * 1. FaceLandmarkerResultからBlendshapesリストを取得
     * 2. mouthSmileLeftとmouthSmileRightの値を抽出（各0.0〜1.0）
     * 3. 左右の平均を計算: (left + right) / 2
     * 4. 0〜100の整数に変換: average * 100
     *
     * 【なぜ左右平均を使うのか】
     * - 片側だけ口角が上がる「にやり笑い」も検出可能
     * - 左右対称の自然な笑顔は高スコアになる
     * - 顔の向きによる左右差を緩和できる
     *
     * 【戻り値の解釈】
     * - 0〜33: 無表情または不機嫌
     * - 34〜66: 軽い笑顔または中間表情
     * - 67〜100: はっきりとした笑顔
     *
     * @param result MediaPipe Face Landmarkerの検出結果
     * @return 0〜100の整数スコア（0=無表情、100=最大の笑顔）
     */
    fun calculateSmileScore(result: FaceLandmarkerResult): Int {
        /**
         * 【Blendshapesの存在チェック】
         *
         * faceBlendshapes()はOptionalを返すため、以下を確認:
         * 1. isEmpty: Optionalが空でないか
         * 2. get().isEmpty(): 検出された顔のリストが空でないか
         *
         * 顔が検出されなかった場合は0を返す
         */
        if (result.faceBlendshapes().isEmpty || result.faceBlendshapes().get().isEmpty()) {
            return 0
        }

        /**
         * 【最初の顔のBlendshapesを取得】
         *
         * faceBlendshapes().get()[0]の意味:
         * - get(): OptionalからListを取り出す
         * - [0]: 最初に検出された顔のBlendshapesを取得
         *
         * 複数の顔を検出している場合、ここでは最初の顔のみを使用
         * NUM_FACES=1に設定しているため、通常は1つだけ
         */
        val blendshapes = result.faceBlendshapes().get()[0]

        /**
         * 【笑顔のBlendshape値を取得】
         *
         * blendshapes.find { ... }:
         *   リストから条件に合う要素を検索
         *
         * categoryName():
         *   Blendshapeの名前（"mouthSmileLeft"など）
         *
         * score():
         *   そのBlendshapeの値（0.0〜1.0）
         *
         * ?: 0f:
         *   見つからなかった場合は0.0fをデフォルト値として使用
         */
        val smileLeft = blendshapes.find { it.categoryName() == "mouthSmileLeft" }?.score() ?: 0f
        val smileRight = blendshapes.find { it.categoryName() == "mouthSmileRight" }?.score() ?: 0f

        /**
         * 【スコアの計算と変換】
         *
         * 1. 左右平均: (smileLeft + smileRight) / 2f
         *    - 左右のバランスを取り、対称的な笑顔を評価
         *
         * 2. 100倍: averageScore * 100
         *    - 0.0〜1.0 を 0〜100 のスケールに変換
         *    - 人間が直感的に理解しやすい形式に
         *
         * 3. coerceIn(0, 100):
         *    - 値を0〜100の範囲内に収める（安全対策）
         *    - 浮動小数点の誤差で範囲外になるのを防止
         */
        val averageScore = (smileLeft + smileRight) / 2f
        return (averageScore * 100).toInt().coerceIn(0, 100)
    }

    /**
     * ============================================================================
     * スコアに基づいてレベル（色）を決定
     * ============================================================================
     *
     * 【信号機カラーシステム】
     * 笑顔スコアを直感的に理解できるよう、信号機の色で表現します。
     *
     * GREEN（緑）: 67〜100
     *   - はっきりとした笑顔
     *   - 口角がしっかり上がっている状態
     *
     * YELLOW（黄）: 34〜66
     *   - 中間の表情
     *   - 軽い微笑みや曖昧な表情
     *
     * RED（赤）: 0〜33
     *   - 無表情または不機嫌
     *   - 口角が上がっていない状態
     *
     * 【しきい値の設定理由】
     * - 3等分（0-33, 34-66, 67-100）でバランス良く分類
     * - 実際の使用感を元に調整可能
     * - 67以上を「笑顔」とすることで、明確な笑顔のみを緑に
     *
     * @param score 0〜100の笑顔スコア
     * @return ScoreLevel（GREEN, YELLOW, RED のいずれか）
     */
    fun getScoreLevel(score: Int): ScoreLevel {
        return when {
            score >= 67 -> ScoreLevel.GREEN   // 笑顔（口角がしっかり上がっている）
            score >= 34 -> ScoreLevel.YELLOW  // 中間（軽い笑顔や曖昧な表情）
            else -> ScoreLevel.RED            // 無表情（口角が上がっていない）
        }
    }

    /**
     * 【スコアレベルの列挙型】
     *
     * enumを使用する理由:
     * - 型安全: 想定外の値が入らない
     * - 可読性: 色の意味が明確
     * - 拡張性: 必要に応じてプロパティを追加可能
     *
     * UI側ではこのenumに基づいて実際の色を決定します。
     * 例: GREEN -> Color.Green, YELLOW -> Color.Yellow, RED -> Color.Red
     */
    enum class ScoreLevel {
        GREEN,  // 67-100: 笑顔（良好）
        YELLOW, // 34-66: 中間（普通）
        RED     // 0-33: 無表情（要改善）
    }
}
