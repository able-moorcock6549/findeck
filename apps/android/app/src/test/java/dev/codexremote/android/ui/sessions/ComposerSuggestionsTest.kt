package dev.codexremote.android.ui.sessions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComposerSuggestionsTest {

    @Test
    fun `detect composer token context covers the whole slash token`() {
        val context = detectComposerTokenContext("please /archivx now", cursorPosition = 11)

        assertEquals('/', context?.prefix)
        assertEquals("archivx", context?.query)
        assertEquals(7, context?.replaceStart)
        assertEquals(15, context?.replaceEnd)
    }

    @Test
    fun `apply composer suggestion replaces the active token and keeps the tail`() {
        val context = detectComposerTokenContext("please /archivx now", cursorPosition = 11)
        val suggestion = ComposerSuggestion(
            id = "slash-archive",
            label = "/archive",
            insertText = "/archive ",
            kind = ComposerSuggestionKind.Command,
        )

        val nextText = applyComposerSuggestion("please /archivx now", context!!, suggestion)

        assertEquals("please /archive now", nextText)
    }

    @Test
    fun `filter composer suggestions respects the current query`() {
        val context = ComposerTokenContext(
            prefix = '/',
            query = "ar",
            replaceStart = 0,
            replaceEnd = 3,
        )
        val suggestions = listOf(
            ComposerSuggestion(
                id = "slash-status",
                label = "/status",
                kind = ComposerSuggestionKind.Command,
            ),
            ComposerSuggestion(
                id = "slash-archive",
                label = "/archive",
                kind = ComposerSuggestionKind.Command,
            ),
        )

        val filtered = filterComposerSuggestions(suggestions, context)

        assertEquals(listOf("/archive"), filtered.map { it.label })
    }

    @Test
    fun `blank query keeps only a compact suggestion set`() {
        val context = ComposerTokenContext(
            prefix = '$',
            query = "",
            replaceStart = 0,
            replaceEnd = 1,
        )
        val suggestions = (1..12).map { index ->
            ComposerSuggestion(
                id = "skill-$index",
                label = "\$skill-$index",
                kind = ComposerSuggestionKind.Skill,
            )
        }

        val filtered = filterComposerSuggestions(suggestions, context)

        assertEquals(8, filtered.size)
        assertEquals("\$skill-1", filtered.first().label)
    }

    @Test
    fun `detect composer token context ignores plain prose`() {
        assertNull(detectComposerTokenContext("hello world", cursorPosition = 5))
    }
}
