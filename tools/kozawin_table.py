"""Generate the 81-day ကိုးနဝင်း schedule from content/kozawin.json.

Checks the rule against the printed chart and writes docs/kozawin-schedule.md
so the schedule can be compared cell by cell with the original card.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WEEKDAYS = ["တနင်္လာ", "အင်္ဂါ", "ဗုဒ္ဓဟူး", "ကြာသပတေး", "သောကြာ", "စနေ", "တနင်္ဂနွေ"]
STAGE_NAMES = ["ပထမ", "ဒုတိယ", "တတိယ", "စတုတ္ထ", "ပဉ္စမ", "ဆဋ္ဌမ", "သတ္တမ", "အဋ္ဌမ", "နဝမ"]
MM_DIGITS = str.maketrans("0123456789", "၀၁၂၃၄၅၆၇၈၉")

# Guna numbers read row by row from the printed chart, for verification.
CHART = [
    [2, 9, 4, 7, 5, 3, 6, 1, 8],
    [3, 1, 5, 8, 6, 4, 7, 2, 9],
    [4, 2, 6, 9, 7, 5, 8, 3, 1],
    [5, 3, 7, 1, 8, 6, 9, 4, 2],
    [6, 4, 8, 2, 9, 7, 1, 5, 3],
    [7, 5, 9, 3, 1, 8, 2, 6, 4],
    [8, 6, 1, 4, 2, 9, 3, 7, 5],
    [9, 7, 2, 5, 3, 1, 4, 8, 6],
    [1, 8, 3, 6, 4, 2, 5, 9, 7],
]


def mm(n):
    return str(n).translate(MM_DIGITS)


def schedule(data):
    prog = data["program"]
    base = prog["baseSequence"]
    days = []
    for d in range(prog["totalDays"]):
        s, p = divmod(d, prog["daysPerStage"])
        guna = (base[p] - 1 + s) % 9 + 1
        days.append({
            "day": d + 1,
            "stage": s + 1,
            "weekday": WEEKDAYS[d % 7],  # program always starts on Monday
            "guna": guna,
            "rounds": guna,
            "vegetarian": p == prog["vegetarianPosition"],
        })
    return days


def main():
    data = json.loads((ROOT / "content" / "kozawin.json").read_text(encoding="utf-8"))
    days = schedule(data)
    for s, row in enumerate(CHART):
        got = [d["guna"] for d in days[s * 9:(s + 1) * 9]]
        assert got == row, f"stage {s + 1}: rule {got} != chart {row}"

    names = {g["number"]: g["pali"] for g in data["gunas"]}
    beads = data["beadsPerRound"]
    lines = [
        "# ကိုးနဝင်း ရက် ၈၁ ရက် ဇယား",
        "",
        "`tools/kozawin_table.py` မှ `content/kozawin.json` ကို အသုံးပြု၍ ထုတ်ထားသည်။ 🟩 = သက်သက်လွတ်နေ့",
        "",
    ]
    for s in range(9):
        lines += [f"## {STAGE_NAMES[s]} အဆင့်", "",
                  "| ရက် | နေ့ | ဂုဏ်တော် | ပတ် | ပုတီးလုံး |", "|---|---|---|---|---|"]
        for d in days[s * 9:(s + 1) * 9]:
            mark = " 🟩" if d["vegetarian"] else ""
            lines.append(f"| {mm(d['day'])}{mark} | {d['weekday']} | {names[d['guna']]} "
                         f"| {mm(d['rounds'])} | {mm(d['rounds'] * beads)} |")
        lines.append("")
    total = sum(d["rounds"] for d in days)
    lines.append(f"စုစုပေါင်း: {mm(total)} ပတ် ({mm(total * beads)} လုံး) — အဆင့်တစ်ဆင့်လျှင် {mm(total // 9)} ပတ်")
    out = ROOT / "docs" / "kozawin-schedule.md"
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"OK: rule matches chart; total {total} rounds; wrote {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
