package com.ws.skelton.remind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.AdLoader
import com.ws.skelton.remind.utils.AdManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adWidth: Int? = null, // Custom width in DP
    adSizeOverride: AdSize? = null, // Specific AdSize (e.g. MEDIUM_RECTANGLE)
    isAnchored: Boolean = false, // True for anchored adaptive, false for inline
    onFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    
    // Calculate adaptive ad size unless overridden
    val width = adWidth ?: configuration.screenWidthDp
    val adSize = remember(width, isAnchored, adSizeOverride) {
        if (adSizeOverride != null) {
            adSizeOverride
        } else if (isAnchored) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)
        } else {
            AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, width)
        }
    }

    // Measure height in DP from AdSize to prevent layout jumping/clipping
    val adHeightDp = adSize.getHeightInPixels(context) / context.resources.displayMetrics.density

    key(adSize) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(adHeightDp.dp), // Set fixed height based on AdSize
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    adUnitId = AdManager.BANNER_ID
                    adListener = object : com.google.android.gms.ads.AdListener() {
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            super.onAdFailedToLoad(error)
                            onFailed()
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { /* AdSize fixed */ }
        )
    }
}

@Composable
fun NativeAdListItem() {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdFailed by remember { mutableStateOf(false) }
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    // Load ad once
    LaunchedEffect(Unit) {
        val adLoader = AdLoader.Builder(context, AdManager.NATIVE_ID)
            .forNativeAd { ad ->
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    isAdFailed = true
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    // Dispose ad
    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    if (isAdFailed) return
    if (nativeAd == null) {
        // Shimmer or placeholder
        Spacer(modifier = Modifier.height(80.dp).fillMaxWidth())
        return
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                // NativeAdView를 수동으로 생성하고 내부 뷰들을 연결
                // 여기서는 간단한 레이아웃을 코드로 생성하거나 XML을 inflate함
                val adView = NativeAdView(ctx)
                val layout = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    setPadding(24, 20, 24, 20)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val iconView = android.widget.ImageView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(40.dpToPx(ctx), 40.dpToPx(ctx))
                }

                val textLayout = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(16, 0, 0, 0)
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1f)
                }

                val adLabel = android.widget.TextView(ctx).apply {
                    text = "AD"
                    textSize = 10f
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#FFCC00"))
                    setPadding(8, 2, 8, 2)
                    layoutParams = android.widget.LinearLayout.LayoutParams(-2, -2).apply {
                        bottomMargin = 4.dpToPx(ctx)
                    }
                }

                val headline = android.widget.TextView(ctx).apply {
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                val body = android.widget.TextView(ctx).apply {
                    textSize = 12f
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }

                textLayout.addView(adLabel)
                textLayout.addView(headline)
                textLayout.addView(body)

                layout.addView(iconView)
                layout.addView(textLayout)

                adView.addView(layout)
                adView.headlineView = headline
                adView.bodyView = body
                adView.iconView = iconView
                
                adView
            },
            update = { adView ->
                // 테마에 따른 텍스트 색상 업데이트 (Compose 색상을 ARGB Int로 변환)
                val headlineColor = if (isDarkTheme) android.graphics.Color.parseColor("#E6E1DC") else android.graphics.Color.BLACK
                val bodyColor = if (isDarkTheme) android.graphics.Color.parseColor("#B0A8A4") else android.graphics.Color.GRAY
                
                (adView.headlineView as android.widget.TextView).apply {
                    text = nativeAd?.headline
                    setTextColor(headlineColor)
                }
                (adView.bodyView as android.widget.TextView).apply {
                    text = nativeAd?.body
                    setTextColor(bodyColor)
                }
                nativeAd?.icon?.let { icon ->
                    (adView.iconView as android.widget.ImageView).setImageDrawable(icon.drawable)
                }
                
                adView.setNativeAd(nativeAd!!)
            }
        )
    }
}

@Composable
fun SmallNativeAdView(
    modifier: Modifier = Modifier,
    onFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdFailed by remember { mutableStateOf(false) }
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        val adLoader = AdLoader.Builder(context, AdManager.NATIVE_ID)
            .forNativeAd { ad -> nativeAd = ad }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    isAdFailed = true
                    onFailed()
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose { nativeAd?.destroy() }
    }

    if (isAdFailed || nativeAd == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp), // Minimum height added
            factory = { ctx ->
                val adView = NativeAdView(ctx)
                val layout = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(12, 8, 12, 8)
                }

                // mandatory: Icon View
                val iconView = android.widget.ImageView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(32.dpToPx(ctx), 32.dpToPx(ctx))
                }

                val adLabel = android.widget.TextView(ctx).apply {
                    text = "AD"
                    textSize = 8f
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#FFCC00"))
                    setPadding(4, 1, 4, 1)
                }

                val textLayout = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(12, 0, 12, 0)
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1f)
                }

                val headline = android.widget.TextView(ctx).apply {
                    textSize = 12f
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxLines = 1
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                
                val body = android.widget.TextView(ctx).apply {
                    textSize = 10f
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxLines = 1
                }

                textLayout.addView(headline)
                textLayout.addView(body)

                layout.addView(adLabel)
                layout.addView(iconView)
                layout.addView(textLayout)

                adView.addView(layout)
                adView.headlineView = headline
                adView.iconView = iconView
                adView.bodyView = body
                adView
            },
            update = { adView ->
                val headlineColor = if (isDarkTheme) android.graphics.Color.parseColor("#E6E1DC") else android.graphics.Color.BLACK
                val bodyColor = if (isDarkTheme) android.graphics.Color.parseColor("#B0A8A4") else android.graphics.Color.GRAY

                (adView.headlineView as android.widget.TextView).apply {
                    text = nativeAd?.headline
                    setTextColor(headlineColor)
                }
                (adView.bodyView as android.widget.TextView).apply {
                    text = nativeAd?.body
                    setTextColor(bodyColor)
                }

                nativeAd?.icon?.let { icon ->
                    (adView.iconView as android.widget.ImageView).setImageDrawable(icon.drawable)
                }
                adView.setNativeAd(nativeAd!!)
            }
        )
    }
}

// Helper extension for pixel conversion
private fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

@Composable
fun ExitAdDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isAdFailed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.98f) // 0.95 -> 0.98 for more width
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 20 -> 16 for better internal width
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Re:Mind를 종료하시겠습니까?", 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "소중한 마음 기록이 보관되었습니다. 😊", 
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))
                
                // 광고 영역 (300x250 Medium Rectangle)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp) // 250dp + padding
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAdFailed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Create,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = androidx.compose.ui.Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Re:Mind", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    } else {
                        // 종료 광고용 프리로드된 AdView 사용
                        val context = LocalContext.current
                        AndroidView(
                            factory = { AdManager.getExitAdView(it) },
                            modifier = Modifier.fillMaxWidth().height(250.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("앱 종료", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
