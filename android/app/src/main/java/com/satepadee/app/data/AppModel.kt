package com.satepadee.app.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

data class Progress(val beads: Int = 0, val rounds: Int = 0)

data class AppState(
    /** Epoch day of ကိုးနဝင်း day 1 (always a Monday), or null before the user starts. */
    val start: Long? = null,
    val done: Set<Int> = emptySet(),
    /** Progress belongs to [progressDay]; any other day starts from zero. */
    val progressDay: Int = -1,
    val progress: Progress = Progress(),
    val wishes: Map<Int, String> = emptyMap(),
    val mode: CountMode = CountMode.Tap,
    val woodId: String? = null,
    val birthDay: String? = null,
    val free: Progress = Progress(),
)

enum class CountMode { Tap, Swipe }

enum class Target { Kozawin, Free }

enum class CountResult { Bead, Round, DayDone, StageDone, ProgramDone }

/** All app state, saved to SharedPreferences on every change so a count is never lost. */
class AppModel(private val prefs: SharedPreferences, val content: Content) {
    var state by mutableStateOf(load())
        private set
    var today by mutableLongStateOf(Days.today())
        private set

    fun refreshToday() { today = Days.today() }

    /** Index of today in the program; negative before the start date, ≥ 81 after the end. */
    val dayIndex: Int? get() = state.start?.let { (today - it).toInt() }

    val started: Boolean get() = dayIndex?.let { it >= 0 } == true

    fun todayDay(): Kozawin.Day? = dayIndex?.takeIf { it in 0 until Kozawin.TOTAL_DAYS }?.let(Kozawin::day)

    fun missedDays(): List<Int> {
        val i = dayIndex ?: return emptyList()
        return (0 until minOf(i, Kozawin.TOTAL_DAYS)).filter { it !in state.done }
    }

    fun todayProgress(): Progress = if (state.progressDay == dayIndex) state.progress else Progress()

    fun progressFor(target: Target): Progress = if (target == Target.Kozawin) todayProgress() else state.free

    fun beadsPerRound(target: Target): Int = Kozawin.BEADS_PER_ROUND

    fun targetRounds(target: Target): Int = if (target == Target.Kozawin) todayDay()?.rounds ?: 0 else 0

    fun wood(): Wood = content.woods.firstOrNull { it.id == state.woodId }
        ?: content.woods.firstOrNull { it.day == state.birthDay }
        ?: content.woods.first { it.id == "padauk" }

    fun count(target: Target): CountResult {
        val per = beadsPerRound(target)
        val p = progressFor(target)
        var beads = p.beads + 1
        var rounds = p.rounds
        var result = CountResult.Bead
        if (beads >= per) { beads = 0; rounds++; result = CountResult.Round }
        val next = Progress(beads, rounds)
        if (target == Target.Free) { update(state.copy(free = next)); return result }

        val day = todayDay() ?: return result
        var done = state.done
        if (rounds >= day.rounds && day.index !in done) {
            done = done + day.index
            result = when {
                day.index == Kozawin.TOTAL_DAYS - 1 -> CountResult.ProgramDone
                day.lastOfStage -> CountResult.StageDone
                else -> CountResult.DayDone
            }
        }
        update(state.copy(progressDay = day.index, progress = next, done = done))
        return result
    }

    fun start() {
        update(state.copy(start = Days.nextMondayOrToday(today), done = emptySet(), wishes = emptyMap(), progressDay = -1, progress = Progress()))
    }

    /** "I recited without the app" — count every missed day as done. */
    fun markMissedDone() = update(state.copy(done = state.done + missedDays()))

    /** Start the stage of the first missed day again, from today. */
    fun restartStage() {
        val first = missedDays().firstOrNull() ?: return
        val stageStart = first / Kozawin.DAYS_PER_STAGE * Kozawin.DAYS_PER_STAGE
        update(state.copy(start = today - stageStart, done = state.done.filter { it < stageStart }.toSet(), progressDay = -1, progress = Progress()))
    }

    fun restartProgram() = start()

    fun setWish(stage: Int, wish: String) = update(state.copy(wishes = state.wishes + (stage to wish.trim())))
    fun setMode(mode: CountMode) = update(state.copy(mode = mode))
    fun setWood(id: String) = update(state.copy(woodId = id))
    fun setBirthDay(id: String) {
        val woodId = content.woods.firstOrNull { it.day == id }?.id ?: state.woodId
        update(state.copy(birthDay = id, woodId = woodId))
    }

    private fun update(new: AppState) { state = new; save(new) }

    private fun load(): AppState = AppState(
        start = if (prefs.contains(K_START)) prefs.getLong(K_START, 0) else null,
        done = prefs.getString(K_DONE, "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet(),
        progressDay = prefs.getInt(K_PDAY, -1),
        progress = Progress(prefs.getInt(K_PBEADS, 0), prefs.getInt(K_PROUNDS, 0)),
        wishes = prefs.getString(K_WISHES, null)?.let { s ->
            val o = JSONObject(s); o.keys().asSequence().associate { it.toInt() to o.getString(it) }
        }.orEmpty(),
        mode = runCatching { CountMode.valueOf(prefs.getString(K_MODE, "Tap")!!) }.getOrDefault(CountMode.Tap),
        woodId = prefs.getString(K_WOOD, null),
        birthDay = prefs.getString(K_BIRTH, null),
        free = Progress(prefs.getInt(K_FBEADS, 0), prefs.getInt(K_FROUNDS, 0)),
    )

    private fun save(s: AppState) {
        prefs.edit().apply {
            if (s.start == null) remove(K_START) else putLong(K_START, s.start)
            putString(K_DONE, s.done.sorted().joinToString(","))
            putInt(K_PDAY, s.progressDay)
            putInt(K_PBEADS, s.progress.beads)
            putInt(K_PROUNDS, s.progress.rounds)
            putString(K_WISHES, JSONObject(s.wishes.mapKeys { it.key.toString() }).toString())
            putString(K_MODE, s.mode.name)
            putString(K_WOOD, s.woodId)
            putString(K_BIRTH, s.birthDay)
            putInt(K_FBEADS, s.free.beads)
            putInt(K_FROUNDS, s.free.rounds)
        }.apply()
    }

    private companion object {
        const val K_START = "start"; const val K_DONE = "done"; const val K_PDAY = "progressDay"
        const val K_PBEADS = "progressBeads"; const val K_PROUNDS = "progressRounds"; const val K_WISHES = "wishes"
        const val K_MODE = "mode"; const val K_WOOD = "wood"; const val K_BIRTH = "birthDay"
        const val K_FBEADS = "freeBeads"; const val K_FROUNDS = "freeRounds"
    }
}
