package com.ws.skelton.remind.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex

// 듣기 팝업 UI
@Composable
fun ListeningDialog(
    currentText: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text("당신의 이야기를 듣고 있어요... 🎧", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                
                Spacer(modifier = Modifier.height(40.dp))

                // 리플(물결) 애니메이션 아이콘
                Box(contentAlignment = Alignment.Center) {
                    RippleEffect()
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening",
                        tint = Color.White,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF0288D1), CircleShape)
                            .padding(12.dp)
                            .zIndex(1f) // 맨 위에 표시
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 실시간 텍스트 표시
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentText.isBlank()) {
                        Text("말씀해 주세요...", color = Color.Gray)
                    } else {
                        Text(currentText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 조작 버튼 (취소, 다시, 완료/저장)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 취소
                    DialogActionButton(
                        onClick = onDismiss,
                        icon = Icons.Default.Close,
                        label = "취소",
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color.Red
                    )
                    // 다시
                    DialogActionButton(
                        onClick = onRetry,
                        icon = Icons.Default.Refresh,
                        label = "다시",
                        containerColor = Color(0xFFFFF3E0),
                        contentColor = Color(0xFFFF9800)
                    )
                    // 완료 (저장)
                    DialogActionButton(
                        onClick = onSave,
                        icon = Icons.Default.Done,
                        label = "완료",
                        containerColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun DialogActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .background(containerColor, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = contentColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = contentColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RippleEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    
    // 3개의 원이 시간차를 두고 커지면서 사라짐
    val ripples = listOf(0, 300, 600)
    
    ripples.forEach { initialDelay ->
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, delayMillis = initialDelay, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scale"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, delayMillis = initialDelay, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )

        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .background(Color(0xFF0288D1), CircleShape)
        )
    }
}
