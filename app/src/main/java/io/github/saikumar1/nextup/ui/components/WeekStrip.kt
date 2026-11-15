package io.github.saikumar1.nextup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.saikumar1.nextup.data.DayCount
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private const val WEEK_PAGES = 1041 // ±10 years of weeks
private const val CENTER_PAGE = WEEK_PAGES / 2

private fun LocalDate.weekStart(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * A swipeable Monday–Sunday strip. The selected day is filled, today is outlined, and a dot
 * marks days with tasks (hollow once all of them are done).
 */
@Composable
fun WeekStrip(
    selected: LocalDate,
    today: LocalDate,
    dayCounts: (from: LocalDate, to: LocalDate) -> Flow<Map<LocalDate, DayCount>>,
    onSelect: (LocalDate) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseWeek = remember(today) { today.weekStart() }
    fun weekForPage(page: Int): LocalDate = baseWeek.plusWeeks((page - CENTER_PAGE).toLong())
    fun pageFor(date: LocalDate): Int =
        (CENTER_PAGE + ChronoUnit.WEEKS.between(baseWeek, date.weekStart()).toInt())
            .coerceIn(0, WEEK_PAGES - 1)

    val pagerState = rememberPagerState(initialPage = pageFor(selected)) { WEEK_PAGES }

    // Keep the strip on the selected day's week when the date changes elsewhere (picker, Today).
    LaunchedEffect(selected, baseWeek) {
        val target = pageFor(selected)
        if (target != pagerState.currentPage) pagerState.animateScrollToPage(target)
    }

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thursday decides which month an ISO week "belongs" to.
            val labelDay = weekForPage(pagerState.currentPage).plusDays(3)
            Text(
                text = labelDay.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (selected != today) {
                AssistChip(
                    onClick = onToday,
                    label = { Text("Today") },
                    leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null) },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            beyondViewportPageCount = 1,
        ) { page ->
            val weekStart = weekForPage(page)
            val counts by remember(weekStart) { dayCounts(weekStart, weekStart.plusDays(6)) }
                .collectAsStateWithLifecycle(initialValue = emptyMap())
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                for (offset in 0L..6L) {
                    val day = weekStart.plusDays(offset)
                    DayCell(
                        date = day,
                        isSelected = day == selected,
                        isToday = day == today,
                        count = counts[day],
                        onClick = { onSelect(day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    count: DayCount?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val description = buildString {
        append(longDate(date))
        if (count != null && count.total > 0) append(", ${count.total} tasks, ${count.done} done")
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                selected = isSelected
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        val circle = when {
            isSelected -> Modifier.background(colors.primary, CircleShape)
            isToday -> Modifier.border(1.5.dp, colors.primary, CircleShape)
            else -> Modifier
        }
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).then(circle),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> colors.onPrimary
                    isToday -> colors.primary
                    else -> colors.onSurface
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        val dot = when {
            count == null || count.total == 0 -> Modifier
            count.done >= count.total -> Modifier.border(1.dp, colors.outline, CircleShape)
            else -> Modifier.background(colors.primary, CircleShape)
        }
        Box(Modifier.size(6.dp).then(dot))
    }
}
