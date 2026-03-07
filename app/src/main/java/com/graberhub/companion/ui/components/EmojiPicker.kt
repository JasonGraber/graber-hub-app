package com.graberhub.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.graberhub.companion.ui.theme.*

data class EmojiCategory(
    val name: String,
    val icon: String,
    val emojis: List<String>
)

val EMOJI_CATEGORIES = listOf(
    EmojiCategory("Chores", "🧹", listOf("🧹", "🧽", "🧺", "🪣", "🧴", "🗑️", "🪥", "🧼", "🫧", "🪠", "🧤", "🪒", "✨", "💫", "🪄")),
    EmojiCategory("Food", "🍳", listOf("🍳", "🥣", "🍽️", "🧑‍🍳", "🥪", "🍕", "🥗", "🧁", "🍎", "🥤", "☕", "🧃", "🍪", "🥞", "🌮")),
    EmojiCategory("School", "📚", listOf("📚", "📖", "✏️", "📝", "🎒", "🖊️", "📐", "🧮", "💻", "🎓", "📓", "🔬", "🌎", "🧠", "📏")),
    EmojiCategory("Sports", "⚽", listOf("⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏊", "🚴", "🏃", "🧘", "💪", "🤸", "🎯", "🏋️", "⛹️")),
    EmojiCategory("Animals", "🐕", listOf("🐕", "🐈", "🐟", "🐦", "🐰", "🐹", "🦎", "🌱", "🌻", "🌳", "🪴", "🦋", "🐝", "🐢", "🐾")),
    EmojiCategory("Home", "🏠", listOf("🏠", "🛏️", "🛋️", "🚿", "🪟", "🚪", "🪑", "💡", "📺", "🧸", "👨‍👩‍👧‍👦", "❤️", "🫂", "🙏", "✝️")),
    EmojiCategory("Time", "⏰", listOf("⏰", "🌅", "🌙", "⭐", "☀️", "🌧️", "❄️", "🌈", "🕐", "📅", "🔔", "⏳", "🌤️", "🌊", "🌸")),
    EmojiCategory("Fun", "🎮", listOf("🎮", "🎨", "🎵", "🎬", "📱", "🎭", "🎪", "🎉", "🏆", "⭐", "🌟", "💎", "👑", "🎁", "🥇"))
)

@Composable
fun EmojiPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    val category = EMOJI_CATEGORIES[selectedCategory]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Choose Emoji",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Category tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EMOJI_CATEGORIES.forEachIndexed { index, cat ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == selectedCategory) ShedBlueLight
                                    else BackgroundGray
                                )
                                .clickable { selectedCategory = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat.icon, fontSize = 20.sp)
                        }
                    }
                }

                Text(
                    category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Emoji grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(category.emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (emoji == selected) ShedBlueLight
                                    else BackgroundGray
                                )
                                .clickable {
                                    onSelect(emoji)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp, textAlign = TextAlign.Center)
                        }
                    }
                }

                // Clear button
                TextButton(
                    onClick = {
                        onSelect("")
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text("Clear", color = TextSecondary)
                }
            }
        }
    }
}
