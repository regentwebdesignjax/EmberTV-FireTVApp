package com.regentmediagroup.embertv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.regentmediagroup.embertv.R

object EmberTheme {
    val Primary = Color(0xFFEF6418)
    val Background = Color(0xFF1A1A1A)
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.7f)

    val AlbertSansFamily = FontFamily(
        Font(R.font.albertsans_regular, FontWeight.Normal),
        Font(R.font.albertsans_semibold, FontWeight.SemiBold),
        Font(R.font.albertsans_bold, FontWeight.Bold),
        Font(R.font.albertsans_light, FontWeight.Light)
    )

    fun titleFont(size: Int = 64) = TextStyle(
        fontFamily = AlbertSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp,
        color = TextPrimary
    )

    fun bodyFont(size: Int = 28) = TextStyle(
        fontFamily = AlbertSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp,
        color = TextPrimary
    )

    fun bodySemibold(size: Int = 24) = TextStyle(
        fontFamily = AlbertSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp,
        color = TextPrimary
    )
}