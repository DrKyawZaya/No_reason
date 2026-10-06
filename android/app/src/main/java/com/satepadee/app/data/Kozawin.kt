package com.satepadee.app.data

/**
 * The ကိုးနဝင်း schedule: 9 stages × 9 days, always starting on a Monday.
 *
 * Stage 1 follows [BASE]; every later stage adds 1 to each guna number (9 wraps to 1).
 * The rounds recited on a day equal its guna number, and the middle day of each stage
 * is a vegetarian day. Verified against every cell of the printed chart in tests.
 */
object Kozawin {
    const val DAYS_PER_STAGE = 9
    const val STAGES = 9
    const val TOTAL_DAYS = STAGES * DAYS_PER_STAGE
    const val BEADS_PER_ROUND = 108
    val BASE = listOf(2, 9, 4, 7, 5, 3, 6, 1, 8)
    private const val VEGETARIAN_POSITION = 4

    data class Day(val index: Int) {
        val stage: Int get() = index / DAYS_PER_STAGE
        val position: Int get() = index % DAYS_PER_STAGE
        val guna: Int get() = (BASE[position] - 1 + stage) % 9 + 1
        val rounds: Int get() = guna
        val vegetarian: Boolean get() = position == VEGETARIAN_POSITION
        val lastOfStage: Boolean get() = position == DAYS_PER_STAGE - 1
        /** Monday = 0, because the program always starts on a Monday. */
        val weekday: Int get() = index % 7
    }

    fun day(index: Int): Day {
        require(index in 0 until TOTAL_DAYS) { "day index $index out of range" }
        return Day(index)
    }
}

/** Calendar days as local epoch days, without java.time so minSdk 24 needs no desugaring. */
object Days {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun today(): Long {
        val now = System.currentTimeMillis()
        return Math.floorDiv(now + java.util.TimeZone.getDefault().getOffset(now), MILLIS_PER_DAY)
    }

    /** Monday = 0 … Sunday = 6. 1970-01-01 was a Thursday. */
    fun weekday(epochDay: Long): Int = Math.floorMod(epochDay + 3, 7L).toInt()

    fun nextMondayOrToday(epochDay: Long): Long = epochDay + Math.floorMod(-weekday(epochDay).toLong(), 7L)
}

/** Myanmar numerals and names used across the UI. */
object Mm {
    private const val DIGITS = "၀၁၂၃၄၅၆၇၈၉"
    val WEEKDAYS = listOf("တနင်္လာ", "အင်္ဂါ", "ဗုဒ္ဓဟူး", "ကြာသပတေး", "သောကြာ", "စနေ", "တနင်္ဂနွေ")
    val STAGES = listOf("ပထမ", "ဒုတိယ", "တတိယ", "စတုတ္ထ", "ပဉ္စမ", "ဆဋ္ဌမ", "သတ္တမ", "အဋ္ဌမ", "နဝမ")

    fun n(value: Int): String = value.toString().map { if (it.isDigit()) DIGITS[it - '0'] else it }.joinToString("")
    fun n(value: Long): String = n(value.toInt())
}
