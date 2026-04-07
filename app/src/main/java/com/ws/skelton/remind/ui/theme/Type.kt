package com.ws.skelton.remind.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ws.skelton.remind.R

// Google Fonts 설정
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

fun getGoogleFontFamily(fontName: String): FontFamily {
    val font = GoogleFont(fontName)
    return FontFamily(
        Font(googleFont = font, fontProvider = provider)
    )
}

// 폰트 인덱스에 따른 FontFamily 매핑
fun getFontFamily(index: Int): FontFamily {
    return when (index) {
        1 -> FontFamily.Serif      // 명조 (Classic)
        2 -> FontFamily.SansSerif  // 고딕 (Modern)
        3 -> FontFamily.Cursive    // 필기체 (Handwriting)
        4 -> FontFamily(Font(R.font.cafe24_ssurround)) // 카페24
        5 -> FontFamily(
            Font(R.font.gmarket_sans_light, FontWeight.Light),
            Font(R.font.gmarket_sans_medium, FontWeight.Medium),
            Font(R.font.gmarket_sans_bold, FontWeight.Bold)
        ) // 지마켓 산스
        6 -> FontFamily(Font(R.font.nanum_gangbu))  // 나눔 강부
        7 -> FontFamily(Font(R.font.nanum_gyuri))   // 나눔 규리
        8 -> FontFamily(Font(R.font.nanum_gippeum)) // 나눔 기쁨
        9 -> FontFamily(Font(R.font.nanum_kkeut))   // 나눔 끄트
        10 -> FontFamily(Font(R.font.nanum_neurit))  // 나눔 느릿
        else -> FontFamily.Default // 기본 (System Default)
    }
}

fun getTypography(index: Int, fontSizeIndex: Int = 1): Typography {
    val family = getFontFamily(index)
    
    // 글꼴 크기 배율 (0: 작게 0.85, 1: 보통 1.0, 2: 크게 1.2)
    val scale = when (fontSizeIndex) {
        0 -> 0.85f
        2 -> 1.15f
        else -> 1.0f
    }

    return Typography(
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.4.sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (22 * scale).sp,
            lineHeight = (28 * scale).sp,
            letterSpacing = 0.15.sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.15.sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.5.sp
        )
    )
}
