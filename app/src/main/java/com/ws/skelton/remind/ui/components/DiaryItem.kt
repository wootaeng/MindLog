package com.ws.skelton.remind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ws.skelton.remind.data.DiaryEntity
import com.ws.skelton.remind.ui.theme.SoftGray
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.ws.skelton.remind.R


@Composable
fun DiaryItem(
    diary: DiaryEntity,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit,
    onRetry: () -> Unit,
    onShare: (String) -> Unit // 추가: 공유 콜백
) {
    val dateFormat = SimpleDateFormat("a h:mm", Locale.KOREA)
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 감정 점수에 따른 아이콘 및 색상
    val (moodIcon, moodColor) = when (diary.moodScore) {
        1 -> Icons.Rounded.SentimentVeryDissatisfied to Color(0xFFE57373)
        2 -> Icons.Rounded.SentimentDissatisfied to Color(0xFFFFB74D)
        3 -> Icons.Rounded.SentimentNeutral to Color(0xFFFFD54F) // Default
        4 -> Icons.Rounded.SentimentSatisfied to Color(0xFFAED581)
        5 -> Icons.Rounded.SentimentVerySatisfied to Color(0xFF81C784)
        else -> Icons.Rounded.SentimentNeutral to Color(0xFFFFD54F)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 날짜와 감정 헤더
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 감정 아이콘 (분석 완료 시에만 색상 적용)
                val iconTint = if (diary.analysisStatus == "DONE") moodColor else Color.LightGray
                Icon(
                    moodIcon, 
                    contentDescription = null, 
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // 날짜
                Text(
                    text = dateFormat.format(Date(diary.date)),
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftGray
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 분석 상태 표시 정교화
                when (diary.analysisStatus) {
                    "ANALYZING" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = SoftGray
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.status_analyzing), style = MaterialTheme.typography.labelSmall, color = SoftGray)
                        }
                    }
                    "ERROR" -> {
                        TextButton(
                            onClick = onRetry,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_retry), style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 공유 버튼 (추가)
                IconButton(
                    onClick = {
                        val shareText = StringBuilder().apply {
                            append("[MindLog] ${dateFormat.format(Date(diary.date))}\n\n")
                            if (!diary.summary.isNullOrBlank()) append("📌 Summary: ${diary.summary}\n\n")
                            append("📝 Diary:\n${diary.content}\n\n")
                            if (diary.moodTags.isNotBlank()) append("🏷️ Tags: ${diary.moodTags}\n\n")
                            if (!diary.aiComment.isNullOrBlank()) append("🤖 AI Advice:\n${diary.aiComment}")
                        }.toString()
                        onShare(shareText)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = SoftGray)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 메뉴 버튼 (점 3개)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = SoftGray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_edit)) },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_delete), color = Color.Red) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 일기 본문 (요약이 있으면 요약 표시, 아니면 원문 표시)
            // 일기 본문 (요약이 있으면 요약 표시, 아니면 원문 표시)
            if (diary.summary != null) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Summary",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp).offset(y = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = diary.summary,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "📝 ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Text(
                        text = diary.content,
                        style = MaterialTheme.typography.labelLarge, // labelLarge matches Hashtags
                        color = MaterialTheme.colorScheme.onSecondary, // Matches Summary
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.Top) {
                     Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Content",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp).offset(y = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = diary.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp
                    )
                 }
            }


            // 감정 태그
            if (diary.moodTags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = diary.moodTags,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF5C6BC0)
                )
            }

            // AI 코멘트 영역
            if (diary.aiComment != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "💌 AI: ${diary.aiComment}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // 수정 다이얼로그
    if (showEditDialog) {
        var editContent by remember { mutableStateOf(diary.content) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.dialog_edit_title)) },
            text = {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text(stringResource(R.string.dialog_edit_placeholder)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEdit(editContent)
                    showEditDialog = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = Color.White
        )
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = Color.White
        )
    }
}

// --- 프리뷰 영역 ---
@Preview(showBackground = true)
@Composable
fun PreviewDiaryItem() {
    val mockDiary = DiaryEntity(
        id = 1,
        content = "오늘은 일찍 퇴근해서 정말 기뻐요! 오랜만에 집에서 맛있는 요리도 해 먹고 넷플릭스도 봤습니다.",
        date = System.currentTimeMillis(),
        moodScore = 5,
        moodTags = "#기쁨 #행복 #힐링",
        summary = "일찍 퇴근해서 맛있는 요리를 해 먹고 휴식을 취함",
        aiComment = "정말 완벽한 하루를 보내셨네요! 자신을 위한 이런 휴식 시간이 마음의 충전에 큰 도움이 된답니다. 😊",
        analysisStatus = "DONE"
    )
    
    Box(modifier = Modifier.padding(16.dp)) {
        DiaryItem(
            diary = mockDiary,
            onDelete = {},
            onEdit = {},
            onRetry = {},
            onShare = {}
        )
    }
}
