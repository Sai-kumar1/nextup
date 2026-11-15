package io.github.saikumar1.nextup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.saikumar1.nextup.R
import java.time.LocalDate

private data class Message(val title: String, val body: String)

private val EMPTY_TODAY = listOf(
    Message("A clear day ahead", "Nothing planned for today. Enjoy the calm — or add something small."),
    Message("Room to breathe", "Your day is wide open. Slow down, sip something warm."),
    Message("Nothing on the list", "A free day is a gift. Spend it however you like."),
)
private val EMPTY_FUTURE = listOf(
    Message("Wide open", "Nothing planned yet. Future you says thanks!"),
    Message("A blank page", "This day is all yours. Plan something nice."),
)
private val EMPTY_PAST = listOf(
    Message("A quiet day", "Nothing was planned for this day."),
)
private val DONE_TODAY = listOf(
    Message("All done for today!", "You crushed it. Time to relax."),
    Message("Everything's checked off!", "Look at you go. Enjoy the rest of your day."),
    Message("Clean sweep!", "Every task done. Treat yourself."),
)
private val DONE_OTHER = listOf(
    Message("All done!", "Every task for this day is checked off."),
)

/** Picks a message that stays the same for a given date. */
private fun List<Message>.pick(date: LocalDate): Message =
    this[Math.floorMod(date.toEpochDay(), size.toLong()).toInt()]

/** Full-screen encouragement when a day has no tasks at all. */
@Composable
fun NothingPlanned(date: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    val message = when {
        date == today -> EMPTY_TODAY
        date.isAfter(today) -> EMPTY_FUTURE
        else -> EMPTY_PAST
    }.pick(date)
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedEmoji(R.raw.emoji_coffee, contentDescription = null, modifier = Modifier.size(140.dp))
        Spacer(Modifier.height(20.dp))
        MoodText(message)
        Spacer(Modifier.height(72.dp)) // optical centring above the FAB
    }
}

/** Celebration shown above the list when every task of the day is done. */
@Composable
fun AllDoneBanner(date: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    val message = (if (date == today) DONE_TODAY else DONE_OTHER).pick(date)
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedEmoji(R.raw.emoji_party, contentDescription = null, modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(16.dp))
        MoodText(message)
    }
}

@Composable
private fun MoodText(message: Message) {
    Text(
        message.title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        message.body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
