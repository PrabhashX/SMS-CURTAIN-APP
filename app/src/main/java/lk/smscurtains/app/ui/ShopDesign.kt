package lk.smscurtains.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Use for fields that belong together; automatically stack on smaller screens. */
@Composable
fun TwoFields(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 280.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ShopAvatar(role: String, onClick: (() -> Unit)? = null, big: Boolean = false) {
    val size = if (big) 70.dp else 48.dp
    val modifier = Modifier.size(size).background(ShopColors.SoftJade, CircleShape)
        .border(1.dp, ShopColors.Jade.copy(alpha = .3f), CircleShape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Box(modifier, contentAlignment = Alignment.Center) {
        Icon(if (role.equals("admin", true)) Icons.Outlined.AdminPanelSettings else Icons.Outlined.Person,
            contentDescription = if (onClick != null) "Open profile" else "User profile",
            tint = ShopColors.Forest, modifier = Modifier.size(if (big) 36.dp else 27.dp))
    }
}

@Composable
fun StateChip(label: String, background: Color = ShopColors.SoftJade, foreground: Color = ShopColors.Forest) {
    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium, color = foreground,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun InfoTile(
    title: String, value: String, icon: ImageVector, tint: Color, background: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(42.dp).background(background, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
            }
            Text(title, style = MaterialTheme.typography.bodySmall, color = ShopColors.Muted)
            Text(value, style = MaterialTheme.typography.titleMedium, color = ShopColors.Forest,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(role: String, username: String, online: Boolean, queued: Int,
    onDismiss: () -> Unit, onLogout: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ShopColors.Linen) {
        Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ShopAvatar(role, big = true)
            Text(username.ifBlank { role.replaceFirstChar { it.uppercase() } }, style = MaterialTheme.typography.headlineSmall)
            StateChip(if (role == "admin") "Administrator · full access" else "Employee · limited access")
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DetailLine(Icons.Outlined.Storefront, "Shop", "SMS Curtains · Mawathagama")
                    HorizontalDivider()
                    DetailLine(Icons.Outlined.CloudDone, "Cloud connection", if (online) "Online · connected" else "Offline · local saving")
                    HorizontalDivider()
                    DetailLine(Icons.Outlined.Sync, "Waiting to sync", "$queued change(s)")
                    HorizontalDivider()
                    DetailLine(Icons.Outlined.VerifiedUser, "Account", role.replaceFirstChar { it.uppercase() })
                }
            }
            Button(onClick = { onDismiss(); onLogout() }, modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ShopColors.Forest),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Sign out")
            }
            Text("SMS Curtains • Phone Edition", style = MaterialTheme.typography.bodySmall, color = ShopColors.Muted)
        }
    }
}

@Composable
private fun DetailLine(icon: ImageVector, heading: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = ShopColors.Jade)
        Column {
            Text(heading, style = MaterialTheme.typography.bodySmall, color = ShopColors.Muted)
            Text(detail, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
