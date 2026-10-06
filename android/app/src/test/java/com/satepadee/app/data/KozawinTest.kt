package com.satepadee.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KozawinTest {
    /** Guna numbers read row by row from the printed ကိုးနဝင်း chart. */
    private val chart = listOf(
        listOf(2, 9, 4, 7, 5, 3, 6, 1, 8),
        listOf(3, 1, 5, 8, 6, 4, 7, 2, 9),
        listOf(4, 2, 6, 9, 7, 5, 8, 3, 1),
        listOf(5, 3, 7, 1, 8, 6, 9, 4, 2),
        listOf(6, 4, 8, 2, 9, 7, 1, 5, 3),
        listOf(7, 5, 9, 3, 1, 8, 2, 6, 4),
        listOf(8, 6, 1, 4, 2, 9, 3, 7, 5),
        listOf(9, 7, 2, 5, 3, 1, 4, 8, 6),
        listOf(1, 8, 3, 6, 4, 2, 5, 9, 7),
    )

    @Test fun scheduleMatchesPrintedChart() {
        for (s in 0 until 9) {
            assertEquals("stage ${s + 1}", chart[s], (0 until 9).map { Kozawin.day(s * 9 + it).guna })
        }
    }

    @Test fun totalsAndVegetarianDays() {
        val days = (0 until Kozawin.TOTAL_DAYS).map(Kozawin::day)
        assertEquals(405, days.sumOf { it.rounds })
        assertEquals((0 until 9).map { it * 9 + 4 }, days.filter { it.vegetarian }.map { it.index })
        assertTrue(days.filter { it.lastOfStage }.all { it.position == 8 })
    }

    @Test fun weekdaysFollowFromMondayStart() {
        // Stage 2 starts on Wednesday, stage 3 on Friday (as on the chart).
        assertEquals(2, Kozawin.day(9).weekday)
        assertEquals(4, Kozawin.day(18).weekday)
    }

    @Test fun epochDayWeekdays() {
        assertEquals(3, Days.weekday(0)) // 1970-01-01 Thursday
        assertEquals(0, Days.weekday(4)) // 1970-01-05 Monday
        assertEquals(4L, Days.nextMondayOrToday(0))
        assertEquals(4L, Days.nextMondayOrToday(4))
    }

    @Test fun myanmarDigits() {
        assertEquals("၁၀၈", Mm.n(108))
    }
}
