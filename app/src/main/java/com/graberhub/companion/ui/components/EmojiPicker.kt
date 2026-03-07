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

val emojiCategories = listOf(
    EmojiCategory("Smileys", "😊", listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😊",
        "😇", "🥰", "😍", "🤩", "😘", "😎", "🤓", "🧐", "😏", "😴"
    )),
    EmojiCategory("Activities", "⚽", listOf(
        "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🎱", "🏓", "🏸", "🥅",
        "⛳", "🎣", "🤿", "🎿", "⛷️", "🏂", "🏋️", "🤸", "🚴", "🏊"
    )),
    EmojiCategory("Food", "🍎", listOf(
        "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍒",
        "🍕", "🍔", "🌮", "🍣", "🍜", "🧁", "🍰", "🍪", "🍩", "🍫"
    )),
    EmojiCategory("Animals", "🐶", listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
        "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🦄", "🐝"
    )),
    EmojiCategory("Travel", "✈️", listOf(
        "🚗", "🚕", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "✈️", "🚀",
        "🛸", "🚁", "⛵", "🚢", "🏠", "🏰", "⛪", "🏫", "🏛️", "🗼"
    )),
    EmojiCategory("Objects", "💡", listOf(
        "⌚", "📱", "💻", "⌨️", "🖥️", "📷", "📺", "🔦", "💡", "🔑",
        "📚", "✏️", "📐", "🎒", "👑", "💎", "🔔", "🎵", "🎨", "🧸"
    )),
    EmojiCategory("Symbols", "❤️", listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💯", "✨",
        "⭐", "🌟", "💫", "🔥", "💧", "🌈", "☀️", "🌙", "⚡", "❄️"
    )),
    EmojiCategory("Hands", "👍", listOf(
        "👍", "👎", "👊", "✊", "🤛", "🤜", "👏", "🙌", "🤝", "🙏",
        "✌️", "🤞", "🤟", "🤘", "👌", "🤌", "👈", "👉", "👆", "👇"
    ))
)

@Composable
fun EmojiPickerDialog(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp),
            shape = RoundedCornerShape(16.dp),
            color = CardBackground
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Choose Emoji",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Category tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    emojiCategories.forEachIndexed { index, category ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == selectedCategory) ShedBlueLight
                                    else BackgroundGray
                                )
                                .clickable { selectedCategory = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = category.icon, fontSize = 18.sp)
                        }
                    }
                }

                // Emoji grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(emojiCategories[selectedCategory].emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (emoji == selectedEmoji) ShedBlueLight
                                    else BackgroundGray
                                )
                                .clickable {
                                    onEmojiSelected(emoji)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 24.sp, textAlign = TextAlign.Center)
                        }
                    }
                }

                // Clear button
                TextButton(
                    onClick = {
                        onEmojiSelected("")
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text("Clear")
                }
            }
        }
    }
}
