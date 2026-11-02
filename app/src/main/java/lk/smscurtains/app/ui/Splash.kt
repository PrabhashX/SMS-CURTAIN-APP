package lk.smscurtains.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Curtains
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import lk.smscurtains.app.R

/** Branded transition displayed briefly while session and local data load. */
@Composable
fun BrandSplash() {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    val scale by animateFloatAsState(if (show) 1f else .86f,
        animationSpec = tween(500), label = "brand-scale")
    val opacity by animateFloatAsState(if (show) 1f else 0f,
        animationSpec = tween(520), label = "brand-fade")
    Box(Modifier.fillMaxSize().background(ShopColors.Forest), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.scale(scale).alpha(opacity)) {
            Box(Modifier.size(116.dp).background(ShopColors.SoftJade, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(R.drawable.ic_curtains), contentDescription = null,
                    tint = Color.Unspecified, modifier = Modifier.size(66.dp))
            }
            Text("SMS CURTAINS", style = MaterialTheme.typography.headlineLarge,
                color = Color.White, fontWeight = FontWeight.Bold)
            Text("MAWATHAGAMA", style = MaterialTheme.typography.labelLarge,
                color = ShopColors.Brass)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = ShopColors.Brass, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}
