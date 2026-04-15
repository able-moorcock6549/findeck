package dev.codexremote.android.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

internal enum class ComposerSuggestionKind {
    Command,
    File,
    Skill,
}

internal data class ComposerSuggestion(
    val id: String,
    val label: String,
    val insertText: String = label,
    val detail: String? = null,
    val kind: ComposerSuggestionKind,
    val browsePath: String? = null,
)

internal data class ComposerSuggestionFilterOption(
    val id: String?,
    val label: String,
)

internal data class ComposerTokenContext(
    val prefix: Char,
    val query: String,
    val replaceStart: Int,
    val replaceEnd: Int,
)

internal fun detectComposerTokenContext(
    text: String,
    cursorPosition: Int,
): ComposerTokenContext? {
    if (text.isEmpty()) return null

    val cursor = cursorPosition.coerceIn(0, text.length)
    val start = findTokenBoundaryBackward(text, cursor)
    val end = findTokenBoundaryForward(text, cursor)
    if (start >= end) return null

    val token = text.substring(start, end)
    val prefix = token.firstOrNull() ?: return null
    if (prefix !in setOf('/', '@', '$')) return null

    return ComposerTokenContext(
        prefix = prefix,
        query = token.drop(1),
        replaceStart = start,
        replaceEnd = end,
    )
}

internal fun applyComposerSuggestion(
    text: String,
    context: ComposerTokenContext,
    suggestion: ComposerSuggestion,
): String {
    val tail = text.substring(context.replaceEnd)
    val adjustedTail = if (
        suggestion.insertText.lastOrNull()?.isWhitespace() == true &&
        tail.firstOrNull()?.isWhitespace() == true
    ) {
        tail.dropWhile { it.isWhitespace() }
    } else {
        tail
    }

    return buildString {
        append(text.substring(0, context.replaceStart))
        append(suggestion.insertText)
        append(adjustedTail)
    }
}

internal fun filterComposerSuggestions(
    suggestions: List<ComposerSuggestion>,
    context: ComposerTokenContext,
): List<ComposerSuggestion> {
    val query = context.query.trim()
    if (query.isBlank()) return suggestions.take(8)

    return suggestions.mapNotNull { suggestion ->
        val normalized = suggestion.label.removePrefix(context.prefix.toString()).trim()
        val score = when {
            normalized.equals(query, ignoreCase = true) -> 0
            normalized.startsWith(query, ignoreCase = true) -> 1
            normalized.contains(query, ignoreCase = true) -> 2
            suggestion.detail?.contains(query, ignoreCase = true) == true -> 3
            else -> null
        }
        score?.let { suggestion to it }
    }
        .sortedWith(
            compareBy<Pair<ComposerSuggestion, Int>> { it.second }
                .thenBy { it.first.label.lowercase() }
        )
        .map { it.first }
        .take(8)
}

internal fun replaceComposerSelection(
    value: TextFieldValue,
    suggestion: ComposerSuggestion,
): TextFieldValue {
    val context = detectComposerTokenContext(
        text = value.text,
        cursorPosition = value.selection.end,
    ) ?: return value.copy(
        text = suggestion.insertText,
        selection = androidx.compose.ui.text.TextRange(suggestion.insertText.length),
    )

    val nextText = applyComposerSuggestion(value.text, context, suggestion)
    return value.copy(
        text = nextText,
        selection = androidx.compose.ui.text.TextRange(nextText.length),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ComposerSuggestionPanel(
    title: String,
    hint: String,
    suggestions: List<ComposerSuggestion>,
    filterOptions: List<ComposerSuggestionFilterOption> = emptyList(),
    selectedFilterId: String? = null,
    onFilterSelect: ((String?) -> Unit)? = null,
    onSuggestionClick: (ComposerSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            )
            if (filterOptions.isNotEmpty() && onFilterSelect != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    filterOptions.forEach { option ->
                        SuggestionFilterChip(
                            label = option.label,
                            selected = option.id == selectedFilterId,
                            onClick = { onFilterSelect(option.id) },
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                suggestions.forEach { suggestion ->
                    SummaryPill(
                        text = suggestion.label,
                        subtitle = suggestion.detail,
                        onClick = { onSuggestionClick(suggestion) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Text(
            text = label,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun findTokenBoundaryBackward(text: String, cursor: Int): Int {
    var index = cursor
    while (index > 0 && !text[index - 1].isWhitespace()) {
        index -= 1
    }
    return index
}

private fun findTokenBoundaryForward(text: String, cursor: Int): Int {
    var index = cursor
    while (index < text.length && !text[index].isWhitespace()) {
        index += 1
    }
    return index
}
