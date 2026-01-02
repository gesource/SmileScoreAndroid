package jp.gesource.sample.smilescore.ui.screen

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import jp.gesource.sample.smilescore.camera.CameraManager
import jp.gesource.sample.smilescore.ml.FaceLandmarkerHelper
import jp.gesource.sample.smilescore.ml.SmileScoreCalculator
import jp.gesource.sample.smilescore.ui.component.CameraPreview
import jp.gesource.sample.smilescore.ui.component.FaceLandmarkOverlay
import jp.gesource.sample.smilescore.ui.component.SmileScoreOverlay

/**
 * メイン画面: カメラプレビュー、顔ランドマーク描画、笑顔スコア表示
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SmileScoreScreen(modifier: Modifier = Modifier) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            SmileScoreContent(modifier = modifier)
        }
        cameraPermissionState.status.shouldShowRationale -> {
            PermissionRationaleScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }
        else -> {
            PermissionDeniedScreen()
        }
    }
}

@Composable
private fun SmileScoreContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var smileScore by remember { mutableIntStateOf(0) }
    var faceDetected by remember { mutableStateOf(false) }
    var faceLandmarkerResult by remember { mutableStateOf<FaceLandmarkerResult?>(null) }
    var isCameraRunning by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }

    val faceLandmarkerHelper = remember {
        FaceLandmarkerHelper(
            context = context,
            listener = object : FaceLandmarkerHelper.FaceLandmarkerListener {
                override fun onResults(result: FaceLandmarkerResult, inferenceTime: Long) {
                    faceLandmarkerResult = result
                    faceDetected = result.faceLandmarks().isNotEmpty()
                    smileScore = if (faceDetected) {
                        SmileScoreCalculator.calculateSmileScore(result)
                    } else {
                        0
                    }
                }

                override fun onError(error: String) {
                    faceDetected = false
                    smileScore = 0
                    faceLandmarkerResult = null
                }
            }
        )
    }

    // Face Landmarkerの初期化
    DisposableEffect(Unit) {
        faceLandmarkerHelper.setupFaceLandmarker()
        onDispose {
            faceLandmarkerHelper.close()
            cameraManager?.shutdown()
        }
    }

    // PreviewViewが設定されたらCameraManagerを初期化し、カメラを開始
    LaunchedEffect(previewView, isCameraRunning) {
        previewView?.let { view ->
            if (cameraManager == null) {
                cameraManager = CameraManager(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = view,
                    onFrameAnalyzed = { bitmap, timestamp ->
                        faceLandmarkerHelper.detectAsync(bitmap, timestamp)
                    }
                )
            }
            // カメラ開始フラグが立っていたらカメラを開始
            if (isCameraRunning) {
                cameraManager?.startCamera()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // タイトル
        Text(
            text = "Smile Score",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "MediaPipe Face Landmarker",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // カメラプレビュー + ランドマークオーバーレイ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            if (isCameraRunning) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onPreviewViewCreated = { previewView = it }
                )

                // 顔ランドマークオーバーレイ
                FaceLandmarkOverlay(
                    result = faceLandmarkerResult,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // カメラ停止中のプレースホルダー
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "カメラを開始してください",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 笑顔スコアオーバーレイ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            SmileScoreOverlay(
                smileScore = smileScore,
                faceDetected = faceDetected && isCameraRunning
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // カメラ制御ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (!isCameraRunning) {
                        isCameraRunning = true
                        // カメラ開始はLaunchedEffectで処理
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isCameraRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("カメラを開始")
            }

            Button(
                onClick = {
                    if (isCameraRunning) {
                        isCameraRunning = false
                        cameraManager?.stopCamera()
                        faceDetected = false
                        smileScore = 0
                        faceLandmarkerResult = null
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = isCameraRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("カメラを停止")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ステータスメッセージ
        Text(
            text = when {
                !isCameraRunning -> "カメラを開始してください"
                !faceDetected -> "顔を検出中..."
                else -> getSmileMessage(smileScore)
            },
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionRationaleScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "カメラ権限が必要です",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "笑顔スコアを測定するにはカメラへのアクセスが必要です。",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("権限を許可する")
        }
    }
}

@Composable
private fun PermissionDeniedScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "カメラ権限が拒否されました",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "設定からカメラ権限を有効にしてください。",
            textAlign = TextAlign.Center
        )
    }
}

private fun getSmileMessage(score: Int): String {
    return when {
        score >= 80 -> "素晴らしい笑顔!"
        score >= 67 -> "良い笑顔です!"
        score >= 50 -> "もう少し笑ってみて!"
        score >= 34 -> "笑顔を見せて!"
        else -> "笑ってみましょう!"
    }
}
