---
title: Production art plan
design: draft
implementation: not-started
art: n/a
depends-on: [.., ../../tech/roadmap, ../../tech/architecture, ../../audio/music, ../../audio/sfx]
updated: 2026-10-02
---

# Production art plan

## Summary

How the chosen concept art becomes the final sprites, backdrops, UI and audio of Acts 1–2. The
[roadmap](../../tech/roadmap/README.md#rules) starts this track after M2 and lets it replace
placeholders part by part; this document fixes what "final" means, how an asset gets from its
generator into `assets/` and to `art: final`, the order of work and the budgets.

## Design

### What "final" means

| Asset | The mockups today | Final |
|---|---|---|
| Sprites (ship, enemies, allies, pickups, bullets) | one sheet per part, a few frames or headings | every frame the game draws: 5 banking frames, 16/32 headings or dense sets for slow rotation, walk cycles, ripple, damaged and wreck frames, separate turrets with per-frame pivots ([animation rules](../README.md#animation-rules)); sizes from the part's `data.yaml` |
| Effects (shots, impacts, explosions, shields) | sheets and GIFs of rounds 08–09 | the frame sequences themselves (12–16 frames per fireball, additive), cut to the sizes in the data |
| Backdrops | one composed scene per setting; Level 01 as placeholder pieces (`backdrop_l01.py`) | per level the tile sets and set pieces of its `backdrop` data, with the haze, posterization and dithering rules; animated pieces within the motion budget |
| UI | screen mockups and the round-08 UI kit | panel kit as nine-patches, bitmap fonts (BMFont) from the kit's pixel font, icons, portraits per speaker |
| Music | chosen full-length mixes (round 08) | per level theme a base stem and the full mix, sample-aligned (round 11 shows the method); stingers as they are; OGG Vorbis q6 with `LOOPSTART` / `LOOPLENGTH`, −14 LUFS |
| SFX | Freesound HQ previews (lossy) and synthesized sounds | recorded sounds rebuilt from the original files with the same `import_sfx.py` settings; synthesized ones as they are |

**Quality bar.** The same look as the chosen mockups, but every frame consistent: render at 8×
for sprites up to 64 px and 4× above (render pipeline step 1), 1-bit alpha, unsharp mask,
24–48 colours per sprite, palette B ramps; backdrops posterized to 12–32 colours with ordered
dithering on wide gradients (round 11 fixed the first banding this way). No frame is
hand-edited.

**Generators.** Production art stays code-generated and reproducible: the SDF ray-marcher in
`tools/concept/render/` is extended rather than replaced — models get detail for full size
(panel lines and greebles as SDF displacement), an angle-set renderer writes the frames under
the names the game loads and a pivot file per sprite, and each setting gets a backdrop
generator built like `backdrop_l01.py` from a shared kit. Production generators live in
`tools/art/` and import the concept `render/` package; the concept scripts stay frozen.

### Pipeline

1. **Source**: a generator script with fixed seeds (git). Rendering caches stay outside the repo.
2. **`assets/`** (committed, LFS): the PNG frames and OGG files the game reads. Every PNG carries
   a `Source` text chunk naming its generator (placeholders keep `Placeholder`), every OGG a
   `SOURCE` comment.
3. **Build** (`pipeline`, never committed): atlas pages, symmetry expansion of angle sets,
   audio checks. `importPlaceholders` stops copying a part once its final files exist.
4. **Review**: a batch of final assets is a concept round like any other. Each part gets a review
   sheet and GIF made from its `assets/` files (`<subject>-final-rRR-a.png` in its `concept/`)
   plus a game capture (`--bench`). The user approves; only then is the part's `art` set to
   `final`. Claude never sets `final` itself.

### Order of work

| When | Parts |
|---|---|
| Now (alongside M3) | **Level 01 complete**: Stormhawk banking frames and wing pods, Skitter and Needler angle sets, pulse cannon shot/muzzle/impact, orb and needle bullets, pickups, the explosion ladder up to large, loot targets, Level 01 backdrop, Coalition Rising stems, launch rail, edge warnings and tone |
| M3 | UI kit and bitmap fonts; main menu, briefing, hangar (equipment icons), debrief, pause and options; portraits, with the three briefing expressions (neutral, grim, fierce) per main character; briefing images (tactical map or mission image per page) for the levels that use one; the hangar intel's sensor L2 enemy portraits and boss silhouette (from Level 02); title, hangar and briefing themes |
| M4 | Act 1 (L02–L07): its enemies and bosses, the weapons' shots and specials, Rook's craft, allies; Luna and Luna far side backdrops; Afterburner stems, boss music |
| M5 | Act 2 (L08–L14): its enemies, naval and ground units, the Kraken; city, ocean, storm, arctic and Geneva backdrops; Act 2 stems |
| M6 | Credits screen art, polish pass, any part still on a placeholder |

A level's art lands before or with the level, so a milestone never ships new placeholders.

### Budgets

Measured in M2: Level 01 packs into a 2048² and a 2048×1024 backdrop page (24 MiB RGBA8) and a
2048×128 sprite page (1 MiB). The spike measured a 768-frame Halo Platform at 10 pages
(160 MiB), 27 MiB with its 6-fold symmetry.

| Limit | Value |
|---|---|
| Atlas pages per level (backdrop + level sprites, boss included) | ≤ 6 pages of 2048² (96 MiB at RGBA8) |
| One unit or boss | ≤ 1 page (16 MiB); dense angle sets only with a symmetric design |
| Shared pages (ship, weapons, pickups, effects, HUD, fonts), always loaded | ≤ 2 pages (32 MiB) |
| Music on disk | ~3–4 MB per 2.5-minute stem at q6 |

**Symmetry rule** for angle sets: a design with *n*-fold radial symmetry renders only 360°/*n*
of its headings and the pipeline repeats them; nothing lit is ever mirrored or rotated at
runtime, since that would move the top-left key light. (Level 01's data mirrors one burning
platform placement; its final art gets a separate render instead.)

## Implementation

- [ ] `tools/art/` with the production render path (angle-set renderer, pivot files, `Source` chunk)
- [ ] Level 01 parts final and approved by the user (list under *Order of work*)
- [ ] `importPlaceholders` skips parts that have final assets
- [ ] Atlas budget check per level in the build (pages and MiB against the table)
- [ ] M3, M4 and M5 parts final, each with its milestone

## Open questions

- **Generators**: extend the SDF ray-marcher for everything (recommended: one look, fully
  reproducible), or add scripted Blender (`bpy`) renders for the largest organic models and bosses?
- **Review granularity**: approve final art per part, or per level batch (recommended: per level batch, with the board showing each part)?
- **Budgets**: are the limits above acceptable, or should integrated GPUs get a lower target?
- **Audio size**: about 28 tracks plus base stems for the level themes is ~150–200 MB at q6;
  keep q6 or drop the stems to q4?
- **Recorded SFX** from the original files need a Freesound login on your side (the previews are
  lossy): download them yourself, or keep the HQ previews?
- Already open elsewhere, they shape this plan: [perspective towers](../README.md#open-questions),
  [tracker modules vs rendered audio and the 28-track scope](../../audio/music/README.md#open-questions).

## Decisions

- 2026-10-02: Drafted as the start of the production-art track (after M2).
- 2026-10-02: M3 close-out (user decision): the briefing portrait expressions, the briefing images and the hangar intel's L2 portraits and boss silhouette are pending art of this track (the [briefing](../../ui/briefing/README.md) and [hangar](../../ui/hangar/README.md) checklists mark them `later: art track`); the game shows one expression and no image until they exist.
