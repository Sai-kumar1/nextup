package io.github.saikumar1.nextup.util

/** A web link found in free text: [range] is where it sits in the text, [url] is openable. */
data class Link(val range: IntRange, val url: String)

private val LINK_REGEX = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"]+""")
private const val TRAILING_PUNCTUATION = ".,;:!?'\"]}>"

/**
 * Finds http(s):// and www. links in [text]. Trailing sentence punctuation is not part of
 * a link, and a closing parenthesis is kept only when the link opened one, so
 * "(see https://x.com/a_(b))." yields "https://x.com/a_(b)". www. links get https://.
 */
fun findLinks(text: String): List<Link> = LINK_REGEX.findAll(text).mapNotNull { match ->
    val start = match.range.first
    var end = match.range.last
    while (end >= start) {
        val c = text[end]
        val trim = when (c) {
            in TRAILING_PUNCTUATION -> true
            ')' -> {
                val candidate = text.substring(start, end + 1)
                candidate.count { it == ')' } > candidate.count { it == '(' }
            }
            else -> false
        }
        if (!trim) break
        end--
    }
    val raw = text.substring(start, end + 1)
    val hasHost = raw.substringAfter("://", raw).removePrefix("www.").removePrefix("WWW.")
        .any { it.isLetterOrDigit() }
    if (!hasHost) return@mapNotNull null
    val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
    Link(start..end, url)
}.toList()
