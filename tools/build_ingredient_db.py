"""Build app/src/main/assets/ingredients_db.json from the Open Food Facts taxonomies and tools/halal_rulings.json.

    python tools/build_ingredient_db.py            # uses tools/.cache/ if present
    python tools/build_ingredient_db.py --refresh  # re-download the taxonomies first

halal_rulings.json is the hand-curated part: one ruling per OFF taxonomy id (e.g. "en:pork"), or per
"local:<id>" for ingredients OFF does not have. Every other taxonomy node is classified like this:

1. its own ruling, if it has one
2. otherwise the ruling of its nearest curated ancestor ("en:wine" -> "en:alcohol"); if equally near
   ancestors disagree, the most severe one wins and the conflict is printed for review. An inherited
   MASHBOOH ruling whose doubt is "animal_source" is lifted to HALAL when the node itself is flagged
   vegan or vegetarian by OFF ("en:soya-lecithin" under "en:e322")
3. otherwise OFF's vegan/vegetarian flags: yes -> HALAL, maybe -> MASHBOOH, vegetarian "no" -> MASHBOOH
4. otherwise the node is left out (the app shows it as unknown)

Taxonomy data: Open Food Facts, Open Database License (ODbL) 1.0. The generated file is ODbL as well.
"""
import argparse
import datetime
import hashlib
import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / "tools" / ".cache"
RULINGS = ROOT / "tools" / "halal_rulings.json"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "ingredients_db.json"
TAXONOMY_URL = "https://static.openfoodfacts.org/data/taxonomies/{}.full.json"
TAXONOMIES = ("ingredients", "additives", "additives_classes")
LANGS = ("de", "en", "fr", "tr", "ar")
SEVERITY = {"HALAL": 0, "UNKNOWN": 1, "MASHBOOH": 2, "HARAM": 3}
DOUBTS = {"animal_source", "alcohol", "other"}
E_NUMBER = re.compile(r"en:e\d{3,4}[a-z]*")
RULING_FIELDS = ("default_category", "hanafi_ruling", "maliki_ruling", "shafi_ruling", "hanbali_ruling",
                 "reason", "reason_de", "source", "alternatives", "doubt", "references", "composition_resolves")
DIET_VALUES = {"yes", "no", "maybe"}
# Taxonomy ids of mixtures ("en:vitamin-a-and-d"), as opposed to one material in a form.
MIXTURE_ID = re.compile(r"-(and|with|und|mit|et|avec|y|con|ve)-")
# Words that hint at alcohol in entries classified HALAL only from OFF's vegan flags; printed for review.
ALCOHOL_WORDS = re.compile(
    r"wein(?!s.ure|blatt|bl.tter|traube|beer|stein)|wine|bier|beer|rum|lik.r|liqueur|liquor|schnaps|brandy|"
    r"whisk|wodka|vodka|sake|cognac|sherry|alkohol|alcohol|spirit|gin|champagner|champagne|"
    r"cidre|cider(?! appl)|met|mead",
    re.IGNORECASE)


def load_taxonomy(name, refresh):
    path = CACHE / f"{name}.full.json"
    if refresh or not path.exists():
        CACHE.mkdir(parents=True, exist_ok=True)
        print(f"downloading {name} taxonomy ...", file=sys.stderr)
        req = urllib.request.Request(TAXONOMY_URL.format(name), headers={"User-Agent": "HalalCheck-build/1.0"})
        with urllib.request.urlopen(req, timeout=300) as resp:
            path.write_bytes(resp.read())
    return json.loads(path.read_text(encoding="utf-8"))


def app_id(taxonomy_id):
    """'en:soya-lecithin' -> 'soya_lecithin', 'fr:foo-bar' -> 'fr_foo_bar', 'local:blood' -> 'blood'."""
    lang, _, rest = taxonomy_id.partition(":")
    rest = rest.replace("-", "_")
    return rest if lang in ("en", "local") else f"{lang}_{rest}"


def flag(node, prop):
    return (node.get(prop) or {}).get("en")


def ancestors_by_depth(tax, node_id):
    """Yield (depth, ancestor_id) breadth-first over the parents graph."""
    seen, frontier, depth = {node_id}, [node_id], 0
    while frontier:
        depth += 1
        nxt = []
        for nid in frontier:
            for parent in tax.get(nid, {}).get("parents", []):
                if parent not in seen:
                    seen.add(parent)
                    nxt.append(parent)
                    yield depth, parent
        frontier = nxt


_inherited = {}


def inherited_flag(tax, node_id, prop, visiting=frozenset()):
    """The node's own flag, else its parents' flags combined.

    "no" from any parent wins. Otherwise a node with several parents is usually a material in a form
    ("en:strawberry-puree" under "en:strawberry" and "en:puree"): the material's "yes" beats the
    form's "maybe". A mixture ("en:vitamin-a-and-d") takes its most doubtful parent.
    """
    key = (node_id, prop)
    if key in _inherited:
        return _inherited[key]
    node = tax.get(node_id, {})
    value = flag(node, prop)
    if not value:
        values = {inherited_flag(tax, p, prop, visiting | {node_id})
                  for p in node.get("parents", []) if p not in visiting and p != node_id}
        order = ("no", "maybe", "yes") if MIXTURE_ID.search(node_id) else ("no", "yes", "maybe")
        value = next((v for v in order if v in values), None)
    _inherited[key] = value
    return value


def e_number_of(tax, node_id):
    """The node itself or its nearest ancestor that is an E-number ("en:soya-lecithin" -> "e322i")."""
    if E_NUMBER.fullmatch(node_id):
        return app_id(node_id)
    return next((app_id(a) for _, a in ancestors_by_depth(tax, node_id) if E_NUMBER.fullmatch(a)), None)


def resolve(tax, rulings, node_id, conflicts):
    """Return (ruling dict, inferred_from) or (None, None)."""
    if node_id in rulings:
        return rulings[node_id], None

    nearest, nearest_depth = [], None
    for depth, anc in ancestors_by_depth(tax, node_id):
        if nearest_depth is not None and depth > nearest_depth:
            break
        if anc in rulings:
            nearest.append(anc)
            nearest_depth = depth
    node = tax[node_id]
    if nearest:
        pick = max(nearest, key=lambda a: SEVERITY[rulings[a]["default_category"]])
        if len({rulings[a]["default_category"] for a in nearest}) > 1:
            conflicts.append((node_id, nearest, pick))
        ruling = rulings[pick]
        lifted = ruling["default_category"] == "MASHBOOH" and ruling.get("doubt") == "animal_source"
        if lifted and flag(node, "vegan") == "yes":
            return {"default_category": "HALAL", "source": "off_vegan"}, None
        if lifted and flag(node, "vegetarian") == "yes":
            return {"default_category": "HALAL", "source": "off_vegetarian"}, None
        return ruling, pick

    vegan, vegetarian = inherited_flag(tax, node_id, "vegan"), inherited_flag(tax, node_id, "vegetarian")
    if vegan == "yes":
        return {"default_category": "HALAL", "source": "off_vegan"}, None
    if vegetarian == "yes":
        return {"default_category": "HALAL", "source": "off_vegetarian"}, None
    if "maybe" in (vegan, vegetarian):
        return {"default_category": "MASHBOOH", "source": "off_maybe", "doubt": "animal_source"}, None
    if vegetarian == "no":
        return {"default_category": "MASHBOOH", "source": "off_animal", "doubt": "animal_source"}, None
    return None, None


def node_names(tax_node, additive_node):
    """Primary name per language. Additives carry descriptive names ("E471 - Mono- und ...")."""
    names = {}
    for lang in LANGS:
        name = (additive_node or {}).get("name", {}).get(lang)
        if name and " - " in name:
            name = name.split(" - ", 1)[1]
        names[lang] = name or tax_node.get("name", {}).get(lang)
    return names


def build_entry(entry_id, names, synonyms, german, ruling, inferred_from, is_class, e_number=None,
                diet=(None, None)):
    names = {k: v.strip() for k, v in names.items() if v and v.strip()}
    fallback = next((names[lang] for lang in LANGS if names.get(lang)), entry_id.replace("_", " "))
    entry = {
        "id": entry_id,
        "name_de": names.get("de") or names.get("en") or fallback,
        "name_en": names.get("en") or names.get("de") or fallback,
    }
    for lang in ("tr", "ar", "fr"):
        if names.get(lang):
            entry[f"name_{lang}"] = names[lang]
    primary = {v.lower() for k, v in entry.items() if k.startswith("name_")}
    unique, seen = [], set(primary)
    for s in synonyms:
        s = s.strip()
        if s and s.lower() not in seen:
            seen.add(s.lower())
            unique.append(s)
    if unique:
        entry["synonyms"] = unique
    # Only real German names (name_de falls back to English); the app splits German compounds with them
    german_unique, seen = [], set()
    for s in german:
        s = (s or "").strip()
        if s and s.lower() not in seen:
            seen.add(s.lower())
            german_unique.append(s)
    if german_unique:
        entry["names_de"] = german_unique
    for field in RULING_FIELDS:
        if ruling.get(field):
            entry[field] = ruling[field]
    entry.setdefault("source", "unknown")
    if inferred_from:
        entry["inferred_from"] = app_id(inferred_from)
    if is_class:
        entry["is_class"] = True
    if e_number:
        entry["e_number"] = e_number
    if diet[0]:
        entry["vegan"] = diet[0]
    if diet[1]:
        entry["vegetarian"] = diet[1]
    return entry


def validate_rulings(rulings, tax):
    errors = []
    for rid, r in rulings.items():
        if not rid.startswith("local:") and rid not in tax:
            errors.append(f"{rid}: not in the OFF taxonomy (use a 'local:' id or fix the id)")
        for field in ("default_category", "hanafi_ruling", "maliki_ruling", "shafi_ruling", "hanbali_ruling"):
            if field in r and r[field] not in SEVERITY:
                errors.append(f"{rid}: invalid {field} {r[field]!r}")
        if "doubt" in r and r["doubt"] not in DOUBTS:
            errors.append(f"{rid}: invalid doubt {r['doubt']!r}")
        if r.get("default_category") == "MASHBOOH" and "doubt" not in r:
            errors.append(f"{rid}: MASHBOOH rulings need a doubt ({', '.join(sorted(DOUBTS))})")
        if rid.startswith("local:") and not r.get("names", {}).get("de"):
            errors.append(f"{rid}: local entries need names.de")
        for diet in ("vegan", "vegetarian"):
            if diet in r and r[diet] not in DIET_VALUES:
                errors.append(f"{rid}: invalid {diet} {r[diet]!r}")
        for ref in r.get("references", []):
            if not (ref.get("url", "").startswith("https://") and ref.get("title")):
                errors.append(f"{rid}: references need an https url and a title")
    if errors:
        sys.exit("halal_rulings.json is invalid:\n  " + "\n  ".join(errors))


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--refresh", action="store_true", help="re-download the OFF taxonomies")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")

    ingredients, additives, classes = (load_taxonomy(n, args.refresh) for n in TAXONOMIES)
    tax = dict(ingredients)
    for aid, node in additives.items():
        tax.setdefault(aid, node)
    rulings = {r["id"]: r for r in json.loads(RULINGS.read_text(encoding="utf-8"))}
    validate_rulings(rulings, tax)

    entries, origin, conflicts, skipped = {}, {}, [], 0
    for node_id in sorted(tax):
        ruling, inferred_from = resolve(tax, rulings, node_id, conflicts)
        is_class = node_id in classes
        if ruling is None and not is_class:
            skipped += 1
            continue
        node = tax[node_id]
        names = node_names(node, additives.get(node_id))
        synonyms = [s for lang in LANGS for s in (node.get("synonyms", {}).get(lang) or [])]
        synonyms += [n for n in names.values() if n]
        curated = rulings.get(node_id, {})
        for extra in [curated.get("names", {})] + curated.get("extra_names", []):
            synonyms += [v for v in extra.values() if v]
        if curated.get("names"):
            names = {**names, **curated["names"]}
        german = [names.get("de")] + (node.get("synonyms", {}).get("de") or [])
        german += [extra.get("de") for extra in curated.get("extra_names", [])]
        diet = (inherited_flag(tax, node_id, "vegan"), inherited_flag(tax, node_id, "vegetarian"))
        entry = build_entry(app_id(node_id), names, synonyms, german, ruling or {"default_category": "UNKNOWN"},
                            inferred_from, is_class, e_number_of(tax, node_id), diet)
        entries[entry["id"]] = entry
        origin[entry["id"]] = node_id

    for rid, r in rulings.items():
        if rid.startswith("local:"):
            eid = app_id(rid)
            if eid in entries:
                sys.exit(f"{rid} collides with the OFF-derived id {eid}")
            synonyms = [v for extra in r.get("extra_names", []) for v in extra.values()]
            german = [r["names"]["de"]] + [extra.get("de") for extra in r.get("extra_names", [])]
            entries[eid] = build_entry(eid, r["names"], synonyms, german, r, None, False, None,
                                       (r.get("vegan"), r.get("vegetarian")))

    ingredient_list = [entries[k] for k in sorted(entries)]
    digest = hashlib.sha256(json.dumps(ingredient_list, ensure_ascii=False, sort_keys=True).encode()).hexdigest()
    output = {
        "version": int(digest[:7], 16),
        "generated": datetime.date.today().isoformat(),
        "license": "Open Database License (ODbL) 1.0. Ingredient names, synonyms and vegan/vegetarian flags: "
                   "Open Food Facts taxonomies (https://world.openfoodfacts.org). Halal rulings: HalalCheck.",
        "ingredients": ingredient_list,
    }
    OUTPUT.write_text(json.dumps(output, ensure_ascii=False, indent=1) + "\n", encoding="utf-8", newline="\n")

    counts = {}
    for e in ingredient_list:
        key = (e["default_category"], e["source"] if e["source"].startswith("off_") else "curated/inherited")
        counts[key] = counts.get(key, 0) + 1
    print(f"wrote {len(ingredient_list)} ingredients to {OUTPUT.relative_to(ROOT)} "
          f"({OUTPUT.stat().st_size // 1024} KB, version {output['version']}); {skipped} taxonomy nodes skipped")
    for (cat, src), n in sorted(counts.items()):
        print(f"  {cat:9} {src:18} {n}")
    if conflicts:
        print(f"\n{len(conflicts)} nodes with disagreeing curated ancestors (most severe wins; "
              f"add a ruling for the node to override):")
        for node_id, nearest, pick in conflicts:
            verdicts = ", ".join(a + "=" + rulings[a]["default_category"] for a in nearest)
            print(f"  {node_id}: {verdicts} -> {pick}")
    suspicious = [e for e in ingredient_list if e["default_category"] == "HARAM" and e.get("inferred_from")
                  and "yes" in (flag(tax[origin[e["id"]]], "vegan"), flag(tax[origin[e["id"]]], "vegetarian"))]
    alcohol_like = [e for e in ingredient_list if e["source"] in ("off_vegan", "off_vegetarian")
                    and any(ALCOHOL_WORDS.search(n) for n in [e["name_de"], e["name_en"]] + e.get("synonyms", []))]
    if alcohol_like:
        print(f"\n{len(alcohol_like)} entries classified HALAL from OFF vegan flags whose names mention alcohol "
              f"(review; add a ruling to override):")
        for e in alcohol_like:
            print(f"  {e['id']}: {e['name_de']}")
    if suspicious:
        print(f"\n{len(suspicious)} inherited HARAM entries that OFF flags as vegan/vegetarian (review):")
        for e in suspicious:
            print(f"  {e['id']} <- {e['inferred_from']}")
    # A nearer, milder ruling hides a HARAM one further up ("en:pork-breast" under "en:breast" and "en:pork")
    hidden_haram = []
    for e in ingredient_list:
        node_id = origin.get(e["id"])
        if not node_id or node_id in rulings or e["default_category"] == "HARAM":
            continue
        haram = [a for _, a in ancestors_by_depth(tax, node_id) if rulings.get(a, {}).get("default_category") == "HARAM"]
        if haram:
            hidden_haram.append((e["id"], e["default_category"], e.get("inferred_from"), haram[0]))
    if hidden_haram:
        print(f"\n{len(hidden_haram)} entries with a HARAM ancestor that a nearer ruling overrides "
              f"(review; add a ruling for the node):")
        for eid, category, inferred, haram in hidden_haram:
            print(f"  {eid}: {category} <- {inferred}, but {haram} is HARAM")


if __name__ == "__main__":
    main()
