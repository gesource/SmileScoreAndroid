package jp.gesource.sample.smilescore.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * 顔のランドマーク（468点）をCanvas上に描画するオーバーレイ
 */
@Composable
fun FaceLandmarkOverlay(
    result: FaceLandmarkerResult?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        result?.faceLandmarks()?.forEach { landmarks ->
            // 468点のランドマークを描画
            landmarks.forEach { landmark ->
                // 正規化座標（0-1）をCanvas座標に変換
                // フロントカメラで既にミラーリングされているので、そのまま使用
                val x = landmark.x() * size.width
                val y = landmark.y() * size.height

                // ランドマークポイントを描画
                drawCircle(
                    color = LandmarkColor,
                    radius = LandmarkRadius,
                    center = Offset(x, y)
                )
            }

            // 顔の輪郭線を描画（オプション）
            drawFaceContours(landmarks)
        }
    }
}

/**
 * 顔の主要な輪郭線を描画
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFaceContours(
    landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>
) {
    // 顔の輪郭（Face Oval）のインデックス
    val faceOvalIndices = listOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
        397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
        172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109, 10
    )

    // 左眉のインデックス
    val leftEyebrowIndices = listOf(276, 283, 282, 295, 285, 300, 293, 334, 296, 336)

    // 右眉のインデックス
    val rightEyebrowIndices = listOf(46, 53, 52, 65, 55, 70, 63, 105, 66, 107)

    // 左目のインデックス
    val leftEyeIndices = listOf(362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398, 362)

    // 右目のインデックス
    val rightEyeIndices = listOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246, 33)

    // 口の外側輪郭のインデックス
    val lipsOuterIndices = listOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409, 270, 269, 267, 0, 37, 39, 40, 185, 61)

    // 輪郭を描画
    drawContour(landmarks, faceOvalIndices, ContourColor)
    drawContour(landmarks, leftEyebrowIndices, ContourColor)
    drawContour(landmarks, rightEyebrowIndices, ContourColor)
    drawContour(landmarks, leftEyeIndices, ContourColor)
    drawContour(landmarks, rightEyeIndices, ContourColor)
    drawContour(landmarks, lipsOuterIndices, LipsColor)
}

/**
 * ランドマークのインデックスリストから輪郭線を描画
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawContour(
    landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    indices: List<Int>,
    color: Color
) {
    for (i in 0 until indices.size - 1) {
        val startIndex = indices[i]
        val endIndex = indices[i + 1]

        if (startIndex < landmarks.size && endIndex < landmarks.size) {
            val start = landmarks[startIndex]
            val end = landmarks[endIndex]

            drawLine(
                color = color,
                start = Offset(start.x() * size.width, start.y() * size.height),
                end = Offset(end.x() * size.width, end.y() * size.height),
                strokeWidth = ContourStrokeWidth
            )
        }
    }
}

// スタイル定数
private val LandmarkColor = Color(0xFF00FF00) // 緑色
private val ContourColor = Color(0xFF00FFFF)  // シアン
private val LipsColor = Color(0xFFFF6B6B)     // 赤みがかったピンク
private const val LandmarkRadius = 2f
private const val ContourStrokeWidth = 1.5f
