package lk.smscurtains.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** SMS Curtains palette: forest, jade, warm linen and brass. */
object ShopColors {
    val Forest = Color(0xFF183B36)
    val Jade = Color(0xFF087D70)
    val JadeDark = Color(0xFF08675E)
    val Linen = Color(0xFFFBF8F3)
    val Sand = Color(0xFFF0EAE0)
    val Brass = Color(0xFFE0B56D)
    val Ink = Color(0xFF273935)
    val Muted = Color(0xFF64736E)
    val SoftJade = Color(0xFFE1F1EB)
    val SoftAmber = Color(0xFFFFF2DC)
    val SoftBlue = Color(0xFFE9F1FB)
}

@Composable
fun CurtainsTheme(content: @Composable () -> Unit) {
    val base = Typography()
    val type = Typography(
        headlineLarge = base.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontSize = 27.sp, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = base.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = base.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontSize = 12.sp)
    )
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ShopColors.Jade, onPrimary = Color.White,
            secondary = ShopColors.Forest, onSecondary = Color.White,
            tertiary = ShopColors.Brass,
            background = ShopColors.Linen, onBackground = ShopColors.Ink,
            surface = Color.White, onSurface = ShopColors.Ink,
            surfaceVariant = ShopColors.Sand, onSurfaceVariant = ShopColors.Muted,
            primaryContainer = ShopColors.SoftJade, onPrimaryContainer = ShopColors.Forest,
            secondaryContainer = ShopColors.SoftAmber, onSecondaryContainer = ShopColors.Forest,
            outline = Color(0xFF91A29D), error = Color(0xFFB33F3B)
        ), typography = type, content = content
    )
}
