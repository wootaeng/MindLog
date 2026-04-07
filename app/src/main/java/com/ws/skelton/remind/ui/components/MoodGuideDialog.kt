package com.ws.skelton.remind.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun MoodGuideDialog(onDismiss: () -> Unit) {
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
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "감정 분석 기준 안내 🌿",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "AI가 작성해주신 일기를 분석하여 5가지 단계로 마음의 날씨를 기록합니다.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                MoodGuideItem(score = "5점", emoji = "😀", label = "매우 좋음", desc = "밝고 긍정적인 에너지가 가득한 하루")
                MoodGuideItem(score = "4점", emoji = "🙂", label = "좋음", desc = "만족스럽고 즐거운 하루")
                MoodGuideItem(score = "3점", emoji = "😐", label = "보통", desc = "평온하고 무난하게 보낸 하루")
                MoodGuideItem(score = "2점", emoji = "🙁", label = "안 좋음", desc = "다소 지치거나 우울함이 느껴지는 하루")
                MoodGuideItem(score = "1점", emoji = "😞", label = "매우 안 좋음", desc = "힘든 감정의 위로와 격려가 필요한 하루")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("확인", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MoodGuideItem(score: String, emoji: String, label: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = label, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = score, 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = desc, 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
