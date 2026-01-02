package jp.gesource.sample.smilescore.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.gesource.sample.smilescore.ml.SmileScoreCalculator

/**
 * 笑顔スコアを表示するオーバーレイコンポーネント
 * 線形プログレスバーと信号機カラーで視覚化
 */
@Composable
fun SmileScoreOverlay(
    smileScore: Int,
    faceDetected: Boolean,
    modifier: Modifier = Modifier
) {
    // アニメーション付きスコア
    val animatedScore by animateFloatAsState(
        targetValue = smileScore / 100f,
        animationSpec = tween(durationMillis = 300),
        label = "smileScore"
    )

    // スコアレベルに応じた色
    val scoreLevel = SmileScoreCalculator.getScoreLevel(smileScore)
    val scoreColor by animateColorAsState(
        targetValue = when (scoreLevel) {
            SmileScoreCalculator.ScoreLevel.GREEN -> GreenColor
            SmileScoreCalculator.ScoreLevel.YELLOW -> YellowColor
            SmileScoreCalculator.ScoreLevel.RED -> RedColor
        },
        animationSpec = tween(durationMillis = 300),
        label = "scoreColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ラベル
        Text(
            text = "笑顔スコア",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // プログレスバー
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            LinearProgressIndicator(
                progress = { if (faceDetected) animatedScore else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = if (faceDetected) scoreColor else Color.Gray,
                trackColor = Color.Transparent
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // スコア数値
        Text(
            text = if (faceDetected) "$smileScore" else "--",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (faceDetected) scoreColor else Color.Gray
        )
    }
}

// 信号機カラー
private val GreenColor = Color(0xFF4CAF50)   // 緑: 67-100（笑顔）
private val YellowColor = Color(0xFFFFC107)  // 黄: 34-66（中間）
private val RedColor = Color(0xFFF44336)     // 赤: 0-33（無表情）
