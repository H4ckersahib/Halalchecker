"""Summarize test data exports from the app (Settings → Testphase → Testdaten exportieren).

    python tools/analyze_feedback.py              # reads feedback/*.json, writes feedback/report.md
    python tools/analyze_feedback.py path/to/dir

The exports contain personal data (what someone scanned); feedback/ is not committed.
The report lists, most useful first:
1. reported mistakes with what the tester expected and why the app decided as it did,
2. verdicts that changed since the tester saw them (rule updates),
3. the most frequent unknown ingredients (candidates for tools/halal_rulings.json or synonyms),
4. the ingredients that most often make products doubtful or haram,
5. contradicted vegan/vegetarian labels and disagreeing data sources.
"""
import collections
import json
import sys
from pathlib import Path

SUPPORTED_EXPORT_VERSION = 1


def load_exports(folder):
    exports = []
    for path in sorted(folder.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as e:
            print(f"skipping {path.name}: {e}", file=sys.stderr)
            continue
        if data.get("export_version") != SUPPORTED_EXPORT_VERSION or "scans" not in data:
            print(f"skipping {path.name}: not a HalalCheck export (version {data.get('export_version')})", file=sys.stderr)
            continue
        data["_file"] = path.name
        exports.append(data)
    return exports


def tester_of(export):
    return export.get("tester") or export["_file"]


def product_label(scan):
    name = scan.get("product_name") or "?"
    brand = f" ({scan['brand']})" if scan.get("brand") else ""
    return f"{name}{brand} `{scan['barcode']}`"


def flagged_ingredients(scan):
    lines = []
    for ing in scan.get("ingredients", []):
        if ing["status"] in ("HARAM", "MASHBOOH", "UNKNOWN"):
            detail = f" [{ing['reason_detail']}]" if ing.get("reason_detail") else ""
            inferred = ", abgeleitet" if ing.get("inferred") else ""
            match = ing.get("matched_id") or "nicht erkannt"
            lines.append(f"  - {ing['status']}: {ing['text']} → `{match}` ({ing.get('reason')}{detail}{inferred})")
    return lines


def report(exports):
    out = []
    scans = [(tester_of(e), e, s) for e in exports for s in e["scans"]]
    builds = sorted({(e.get("app_build"), e.get("ingredients_db_version")) for e in exports}, key=str)

    out.append("# HalalCheck Testdaten-Auswertung\n")
    out.append(f"- Dateien: {len(exports)}, Tester: {len({tester_of(e) for e in exports})}")
    out.append(f"- App-Builds / DB-Versionen: {', '.join(f'{b} / {d}' for b, d in builds)}")
    out.append(f"- Produkte: {len(scans)} (davon {len({s['barcode'] for _, _, s in scans})} verschiedene)")
    verdicts = collections.Counter(s.get("verdict") or "nicht im Cache" for _, _, s in scans)
    out.append(f"- Aktuelle Urteile: {', '.join(f'{k} {v}' for k, v in verdicts.most_common())}\n")

    feedback = [(t, s) for t, _, s in scans if s.get("feedback")]
    out.append(f"## 1. Gemeldete Fehler ({len(feedback)})\n")
    for tester, scan in sorted(feedback, key=lambda x: x[1]["feedback"]["created_at"]):
        fb = scan["feedback"]
        expected = fb.get("expected") or "weiß nicht"
        out.append(f"### {product_label(scan)}")
        out.append(f"- Tester: {tester}, Madhab: {fb['madhab']}")
        out.append(f"- Angezeigt: **{fb['verdict_shown']}**, erwartet: **{expected}**, aktuell: **{scan.get('verdict')}**")
        if fb.get("comment"):
            out.append(f"- Kommentar: „{fb['comment']}“")
        if scan.get("user_claims"):
            out.append(f"- Bestätigte Kennzeichnung: {', '.join(scan['user_claims'])}")
        if scan.get("off_labels"):
            out.append(f"- OFF-Labels: {', '.join(scan['off_labels'])}")
        flagged = flagged_ingredients(scan)
        if flagged:
            out.append("- Auffällige Zutaten:")
            out.extend(flagged)
        if scan.get("ingredients_text"):
            out.append(f"- Zutatenliste: {scan['ingredients_text']}")
        out.append("")

    changed = [(t, s) for t, _, s in scans if s.get("verdict") and s.get("verdict_shown") and s["verdict"] != s["verdict_shown"]]
    out.append(f"## 2. Seit dem Scan geänderte Urteile ({len(changed)})\n")
    for tester, scan in changed:
        out.append(f"- {product_label(scan)}: {scan['verdict_shown']} → {scan['verdict']} ({tester})")
    out.append("")

    unknown = collections.Counter()
    unknown_examples = collections.defaultdict(set)
    for _, _, scan in scans:
        for name in scan.get("unknown", []):
            key = name.strip().lower()
            unknown[key] += 1
            unknown_examples[key].add(scan.get("product_name") or scan["barcode"])
    out.append(f"## 3. Häufigste unbekannte Zutaten ({len(unknown)} verschiedene)\n")
    for name, count in unknown.most_common(50):
        examples = ", ".join(sorted(unknown_examples[name])[:3])
        out.append(f"- {count}× **{name}** (z. B. {examples})")
    out.append("")

    drivers = collections.Counter()
    for _, _, scan in scans:
        for ing in scan.get("ingredients", []):
            if ing["status"] in ("HARAM", "MASHBOOH") and ing.get("matched_id"):
                drivers[(ing["matched_id"], ing["status"], ing.get("reason"))] += 1
    out.append("## 4. Was Produkte am häufigsten zweifelhaft oder haram macht\n")
    for (match, status, reason), count in drivers.most_common(30):
        out.append(f"- {count}× `{match}` → {status} ({reason})")
    out.append("")

    conflicts = [(t, s) for t, _, s in scans if s.get("vegan_conflicts") or s.get("vegetarian_conflicts") or s.get("source_conflict")]
    out.append(f"## 5. Widersprüche ({len(conflicts)})\n")
    for tester, scan in conflicts:
        parts = []
        if scan.get("vegan_conflicts"):
            parts.append(f"vegan passt nicht zu {', '.join(scan['vegan_conflicts'])}")
        if scan.get("vegetarian_conflicts"):
            parts.append(f"vegetarisch passt nicht zu {', '.join(scan['vegetarian_conflicts'])}")
        if scan.get("source_conflict"):
            parts.append("Zutatenliste und OFF-Daten weichen ab")
        out.append(f"- {product_label(scan)}: {'; '.join(parts)} ({tester})")
    out.append("")
    return "\n".join(out)


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    folder = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "feedback"
    if not folder.is_dir():
        sys.exit(f"{folder} not found. Put the exported JSON files there.")
    exports = load_exports(folder)
    if not exports:
        sys.exit(f"no HalalCheck exports in {folder}")
    text = report(exports)
    target = folder / "report.md"
    target.write_text(text, encoding="utf-8")
    print(text)
    print(f"\nwritten to {target}", file=sys.stderr)


if __name__ == "__main__":
    main()
