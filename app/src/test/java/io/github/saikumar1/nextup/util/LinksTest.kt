package io.github.saikumar1.nextup.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksTest {
    private fun urls(text: String) = findLinks(text).map { it.url }

    @Test
    fun findsHttpAndHttpsLinks() {
        assertEquals(
            listOf("https://example.com/a?b=1", "http://test.org"),
            urls("see https://example.com/a?b=1 and http://test.org"),
        )
    }

    @Test
    fun wwwLinksGetHttps() {
        assertEquals(listOf("https://www.example.com/page"), urls("go to www.example.com/page"))
    }

    @Test
    fun trailingPunctuationIsNotPartOfTheLink() {
        assertEquals(listOf("https://example.com"), urls("Visit https://example.com."))
        assertEquals(listOf("https://example.com/x"), urls("(https://example.com/x)"))
        assertEquals(listOf("https://example.com"), urls("is it https://example.com?"))
    }

    @Test
    fun balancedParenthesesAreKept() {
        assertEquals(
            listOf("https://en.wikipedia.org/wiki/Kotlin_(programming_language)"),
            urls("see (https://en.wikipedia.org/wiki/Kotlin_(programming_language))."),
        )
    }

    @Test
    fun rangesPointAtTheLinkText() {
        val text = "a https://x.io b"
        val link = findLinks(text).single()
        assertEquals("https://x.io", text.substring(link.range.first, link.range.last + 1))
    }

    @Test
    fun ignoresTextWithoutLinks() {
        assertTrue(findLinks("buy milk, call mom. www. https://").isEmpty())
    }

    @Test
    fun findsMultipleLinksAcrossLines() {
        assertEquals(
            listOf("https://a.com", "https://www.b.com"),
            urls("first: https://a.com\nsecond: www.b.com"),
        )
    }
}
