# စိပ်ပုတီး app — Plan

Android prayer-bead (ပုတီး) counter for Myanmar Buddhists. Myanmar-only UI, fully offline,
to be published on Google Play.

## Decisions so far
- Interface language: Myanmar only (Unicode, never Zawgyi).
- Input: tap or swipe in a defined area of the screen. No volume-button counting.
- Recitations: built-in ones (ကိုးနဝင်း first) with description, rules and reminders,
  plus recitations the user creates.
- Target: Google Play Store.
- Stack: Kotlin + Jetpack Compose, Room, DataStore, AlarmManager/WorkManager. No internet permission.

## Features (v1)
1. **Counter** — large tap area, swipe up to undo, vibration per bead and per round, screen stays on.
2. **Built-in recitations** — content lives in `content/*.json`, never hard-coded.
3. **Description screen** — meaning, rules and benefits before starting.
4. **Multi-day programs** — e.g. ကိုးနဝင်း: 81 days, today's guna and rounds shown automatically.
5. **Custom recitations** — name, text, beads per round, daily target.
6. **Reminders** — daily time chosen when a recitation is picked, plus a day-before reminder for
   vegetarian (သက်သက်လွတ်) days.
7. **History** — daily log, calendar, streak.
8. **Settings** — vibration, sound, theme, font size.

## ကိုးနဝင်း model (from the printed chart)
- 9 stages (အဆင့်) × 9 days = 81 days, must start on a Monday, no missed days.
- Day `d` (0–80): stage `s = d / 9`, position `p = d % 9`,
  guna = `((base[p] − 1 + s) mod 9) + 1` with `base = [2, 9, 4, 7, 5, 3, 6, 1, 8]`.
- Rounds that day = guna number (×108 beads). Position 4 (middle day) = vegetarian day.
- 45 rounds per stage, 405 rounds (43,740 beads) total.
- One wish (ဆုတောင်း) per stage; stage benefit text shown when a stage is completed.
- Verified against every cell of the chart by `tools/kozawin_table.py`;
  full table in `docs/kozawin-schedule.md`.

## Screens
Home (today) → Recitation list → Recitation detail + reminder setup → Counter →
Round / day / stage complete → History → Settings.

## Phases
| Phase | Work | Output |
|---|---|---|
| 0 | Decisions: name, repo, content rules | This plan |
| 1 | UX: flow, clickable prototype, Canva mockups | Approved design |
| 2 | Content: ကိုးနဝင်း (done, pending review) + other recitations | `content/*.json` |
| 3 | Android project + counter screen | Working `.apk` |
| 4 | Recitations, programs, custom recitations | Core features |
| 5 | Reminders, history, settings | Beta |
| 6 | Real-device testing | Stable build |
| 7 | Play Store: account, signing, privacy policy, closed test (12 testers × 14 days) | Published |

## Open questions
- What happens if a ကိုးနဝင်း day is missed: restart from day 1, or restart the current stage?
- Per bead, is only the day's guna recited (e.g. "သမ္မာသမ္ဗုဒ္ဓေါ"), or the full ဣတိပိသော?
- Other recitations for v1 and their sources.
- App name and final repository.
