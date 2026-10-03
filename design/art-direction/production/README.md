---
title: Production art plan
design: draft
implementation: in-progress
art: n/a
depends-on: [.., ../../tech/roadmap, ../../tech/architecture, ../../audio/music, ../../audio/sfx]
updated: 2026-10-03
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

**How the build tells final from placeholder**: by the `Source` chunk. `importPlaceholders`
(`PlaceholderSprites`) skips every cut whose first frame in `assets/sprites/` carries one, so a
part is final-ready as soon as its generator in [tools/art/](../../../tools/art/README.md) has
written it. Pivot files (`assets/pivots/<sprite>.json`) hold per frame the points other sprites
attach to. **Budget check**: `packAtlases` checks the packed pages against the table below
(`AtlasBudget`) and fails the build when a budget is exceeded; the `sprites` atlas counts as the
shared pages (Level 01's enemies and loot targets are in it until levels get their own sprite
atlas), a level's pages are the backdrop pages holding its regions, and one sprite's frames must
fit a page. After the Level 01 batch: shared 1 page of 2048×256 (2 MiB of 32), Level 01
2 pages, 2048² and 1024×2048 (24 MiB of 96). With the HUD's metal parts (`assets/sprites/hud/`,
on the same pages since `packAtlases` combines the sprites' subfolders): shared 1 page of 1024²
(4 MiB of 32). With the glass UI kit (`assets/sprites/ui/`) and the 94 equipment icons
(`assets/sprites/icons/`) still 1 page of 1024² (4 MiB of 32); the title scene, logo and hangar map
are textures of their own in `assets/ui/` (about 3 MiB loaded, the scene always, the map in the
hangar), outside the atlas budget. With the portraits (`assets/sprites/portraits/`) and the intel
pictures (`assets/sprites/intel/`): 1 page of 2048×1024 (8 MiB of 32); the briefing images are
textures of their own in `assets/ui/briefing/` (0.6 MiB each, loaded by the briefing that shows them). The Leviathan's frames (Level 03,
with the death chunks): about 1.88 M px, about 7.2 MiB of its 16 MiB one-page budget.

**Sounds**: a final OGG carries a `SOURCE` Vorbis comment; `importPlaceholders`
(`PlaceholderSounds`) keeps every sound in `assets/sfx/` that has one.

### Order of work

| When | Parts |
|---|---|
| Now (alongside M3) | **Level 01 complete**: Stormhawk banking frames and wing pods, the Skitter's 16-heading angle set and the Needler's claw snap, pulse cannon shot/muzzle/impact, orb and needle bullets, pickups, the explosion ladder up to large, loot targets, Level 01 backdrop, Coalition Rising stems, launch rail, edge warnings and tone |
| M3 | **UI batch**, first the **in-game HUD** (the side panels as bevelled metal plates with corner rivets, the recessed wells and readout frames, label plates, bar troughs and fills, the portrait frame and the phosphor readout glow), then the UI kit and bitmap fonts; main menu, briefing, hangar (equipment icons), debrief, pause and options; portraits, with the three briefing expressions (neutral, grim, fierce) per main character; briefing images (tactical map or mission image per page) for the levels that use one; the hangar intel's sensor L2 enemy portraits and boss silhouette (from Level 02); title, hangar and briefing themes |
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
runtime, since that would move the top-left key light. The same holds for set pieces: a placement
that would be drawn mirrored uses its own `-mirrored` piece, rendered with the layout mirrored and
the key light fixed (Level 01: dock frame, bridge crane, crane jib, burning platform).

## Implementation

- [x] `tools/art/` with the production render path (angle-set renderer, pivot files, `Source` chunk)
- [x] Level 01 parts final and approved by the user (list under *Order of work*)
- [x] `importPlaceholders` skips parts that have final assets
- [x] Atlas budget check per level in the build (pages and MiB against the table)
- [x] Level 01 backdrop rendered by `tools/art/backdrop_l01.py` (proposed in round 12)
- [x] Recorded SFX rebuilt from the Freesound originals (`tools/art/sfx_originals.py`); `importPlaceholders` keeps final OGGs
- [x] Concept round 12 (the Level 01 batch review) opened, with a game capture
- [x] UI batch part U1: the in-game HUD's metal parts rendered by `tools/art/hud.py` and drawn by the game (review files proposed for round 13)
- [x] UI batch part U2: the glass UI kit (`tools/art/ui_kit.py`), the title scene, logo D and the hangar's tactical map (`tools/art/ui_scenes.py`) and the equipment icons (`tools/art/icons.py`) drawn by the game; the Level 01 north arm reworked (review files proposed for round 13)
- [x] UI batch part U3: portraits with the three expressions (`tools/art/portraits.py`), the briefing images (`tools/art/briefing_images.py`), the hangar intel's sensor-L2 pictures (`tools/art/intel.py`), the title, hangar and briefing themes (`tools/art/themes.py`) and the bitmap fonts (`tools/art/fonts.py`), drawn and played by the game; concept round 13 (the UI batch review) opened
- [x] M3 parts final and approved by the user (the UI batch, concept round 13)
- [x] M4 part A: the Act 1 arsenal's effects rendered by `tools/art/weapon_fx.py` and drawn by the game; approved as final in round 14
- [x] M4 part B: Level 02's Stinger, Spine Turret, salvage M and overdrive, backdrop, Crane Four and the "Afterburner" stems approved as final in round 15
- [x] M4 part C (Level 03, M4 part C batch): the Spore Bomber and its spore mine, the Whirl Seed and the debris chunks (`tools/art/vrell_l03.py`), the Leviathan (`tools/art/leviathan.py`) and its break-up at its death (`tools/art/leviathan_death.py`), the Level 03 backdrop (`tools/art/backdrop_l03.py`), salvage L (`tools/art/pickups.py`) and the Spore Bomber's and Whirl Seed's intel portraits (`tools/art/intel.py`) rendered, review files for round 16; approved as final there (the Leviathan's death effect as the redo, variant b, choice 19)
- [x] M4 part D (Level 04): the civilian crawler (`tools/art/civilian_crawler.py`), the Airstrike's CDF bomber (`tools/art/airstrike_bomber.py`), the Level 04 Luna backdrop with the road bridge and the terminal hangar (`tools/art/backdrop_l04.py`), the dugout and the supply drop (`tools/art/l04_targets.py`) and the Brood Pod's and Scuttler's intel portraits (`tools/art/intel.py`) rendered, review files for round 17; approved as final there (the bridge and the enlarged hangar as the redo, choice 9)
- [ ] M4 and M5 parts final, each with its milestone (a round per M4 part, user decision)

## Open questions

- Already open elsewhere, they shape this plan: [perspective towers](../README.md#open-questions),
  [tracker modules vs rendered audio and the 28-track scope](../../audio/music/README.md#open-questions).

## Decisions

- 2026-10-02: Drafted as the start of the production-art track (after M2).
- 2026-10-02: User decisions: everything is rendered with the extended SDF ray-marcher (no
  Blender); final art is reviewed per level batch; the budgets above are accepted; audio stays at
  q6 including the base stems; the recorded SFX are rebuilt from the Freesound originals via the
  API (OAuth2 by the user).
- 2026-10-02: M3 close-out (user decision): the briefing portrait expressions, the briefing images and the hangar intel's L2 portraits and boss silhouette are pending art of this track (the [briefing](../../ui/briefing/README.md) and [hangar](../../ui/hangar/README.md) checklists mark them `later: art track`); the game shows one expression and no image until they exist.
- 2026-10-02: Level 01 batch, part P1 (sprites and effects): the production path in
  [tools/art/](../../../tools/art/README.md) (`artkit.py`: 8×/4× render, 1-bit alpha, unsharp
  mask, one median-cut palette per frame set, additive effects premultiplied on black, stepped
  glow translucency, ordered dither, angle sets, pivot files, `Source` chunk, review sheets) and
  one generator per part: Stormhawk (banking frames, wing pods with pivots, engine flame), Skitter
  and Needler, pulse cannon, Vrell orb and needle, pickups, the explosion ladder up to large, the
  loot targets. The models stay the chosen concept models (panel-line detail would not read at
  12–96 px; it comes with the large units). Effects are the chosen 2D light fields at their 4×
  supersampling, not ray-marched. Review files are proposed for round 12 (opened by part P2).
  `importPlaceholders` skips final parts; `packAtlases` enforces the budgets.
- 2026-10-02: Level 01 batch, part P2: the final Level 01 backdrop
  (`tools/art/backdrop_l01.py`: the placeholder's kit, layouts and seeds at the production bar,
  12–32 colours per piece, dithered limb and Vrell glow, a 4× Moon, Aegis Two through the sprite
  render path); the four mirrored placements of Level 01 got `-mirrored` renders (the rule above
  said one; there were four). The 75 chosen recorded sounds rebuilt from the Freesound originals
  with their unchanged `import_sfx.py` settings (`tools/art/sfx_originals.py`; originals above
  full scale are clipped first, as the previews were), all within 1 dB of the chosen files in
  length, peak and band RMS but one peak (1.7 dB); `importPlaceholders` keeps final OGGs by their
  `SOURCE` comment. [Round 12](../../concept-rounds/round-12/README.md) opened for the whole batch;
  no part is `final` until the user approves it there.
- 2026-10-02: Level 01 batch, part P3 (before the round-12 review): the Skitter turns to face its
  flight (`16 angles`, user decision; the Needler stays `fixed`), so its final sprite is a 16 × 6
  angle set; the clear weak spots of P1 are reworked: the Stormhawk's banking frames roll ±15° and
  ±30° (within the art direction's "about ±14° and ±28°") through a mild perspective camera
  (`artkit.perspective`), so the raised wing grows and the lowered one shrinks, and its pods are
  lighter, shaded metal; the Skitter's wings lift and sweep; the Needler's claws swing wider; the
  pulse impact's last frame no longer fades to black; the needle's rim is gold and its halo tight;
  the crate shows its cross on every face; the loot container's pieces crumble from their edges
  instead of dissolving in a dither checker. Explosions, backdrop and SFX unchanged.
- 2026-10-02: Concept round 12 closed (user decision): every Level 01 part approved as final; the parts with their own doc are `art: final`, the aggregate docs (enemies, player, art direction, SFX) record the final assets in their Decisions and keep `chosen`.
- 2026-10-02: The in-game HUD is the first item of the UI batch (user decision: "It should be metallic bevelled panels, but at the moment it's flat and no texture at all. Also the rivets in the corners are missing."). It was named in neither the Level 01 row nor the M3 row, so the Level 01 batch shipped the M1 placeholder panels drawn in code.
- 2026-10-02: UI batch, part U1 (the in-game HUD): `tools/art/hud.py` renders the side panels as
  whole 240×540 plates (fixed layout; a nine-patch centre would smear the brushed steel) and the
  stretching parts (LCD well, portrait well, phosphor fill, readout glow) as libGDX nine-patches
  (`.9.png`), the label plate as a fixed piece; the game draws them in the existing HUD regions
  ([HUD](../../ui/hud/README.md) Decisions). They are packed onto the shared sprite pages
  (`packAtlases` now combines the sprites' subfolders; the backdrop keeps a page set per level):
  1 page of 1024², 4 MiB. Review sheet and capture proposed for round 13, which later parts of the
  UI batch open.
- 2026-10-02: UI batch, part U2 (the out-of-game screens): `tools/art/ui_kit.py` renders the
  glass kit of ui-kit-r08-a as nine-patches and pieces in `assets/sprites/ui/` (glass body and a
  separate trim with corner tabs, so a panel's opacity never fades its metal; amber and dialog
  trims, inset, rule, selection bar, cursors, chips, tabs, knob, bar cell, row, key-hint plate, tag,
  scroll markers, callouts, diamond, chevron), drawn by `Glass` instead of fills;
  `tools/art/ui_scenes.py` renders main menu A's hero scene and logo D and the hangar's tactical
  map (Act 1) into `assets/ui/` at the production bar (per-layer palettes with ordered dither, the
  ships ray-marched at their size with the heading in the model); `tools/art/icons.py` renders one
  icon per catalogue item and Rook's escort in two sizes (16 and 24 px), from the models where they
  exist and otherwise one emblem per kind; the debrief now draws the kit over the dimmed title
  scene. The Level 01 north arm's solar wings were cut off at the piece's edge (user decision): only
  that piece is re-rendered and is a round-13 review item. Review sheets and game captures proposed
  for round 13 (opened by part U3). The bitmap fonts stay the kit's BMFont files from
  `tools/concept/ui_assets.py`.
- 2026-10-02: UI batch, part U3 (portraits, briefing images, intel pictures): `tools/art/portraits.py`
  renders every speaker of Acts 1–2 in the chosen style B: Okafor, Rook and Varga in neutral, grim
  and fierce (the concept busts with the expression's brows, lids, mouth and head pitch, one palette
  per character over its expressions), the briefing size for Okafor and Varga, the generic CDF
  officer and civilian neutral, the Choir's 32-frame glyph loop; the placeholder portrait cuts are
  gone from `importPlaceholders`. Briefing pages and radio lines take an optional `expression`
  (neutral by default), set in the Act 1 and Level 01 data where the text calls for it (see the
  [briefing](../../ui/briefing/README.md) and [HUD](../../ui/hud/README.md) Decisions); the mission
  failed screen shows Okafor grim. `tools/art/briefing_images.py` draws one 672×240 tactical map or
  mission image per briefing page of the Act 1 intro and Levels 01–02 (`image` on a page; a page
  too long for the space below its image goes on over the next screens). `tools/art/intel.py`
  renders the hangar intel's sensor-L2 portraits of the enemy types of Levels 01–02 and the
  silhouettes of the Act 1 bosses. Vorne gets his portraits with Act 6, where he first speaks.
- 2026-10-02: UI batch, part U3 (fonts): `tools/art/fonts.py` takes the bitmap fonts over from
  `tools/concept/ui_assets.py`, with the same font, cells, baselines and line heights. The 114
  existing glyphs are unchanged pixel for pixel, and the set grows to 127 (en dash, typographic
  quotes, →, ≈, upper-case accented letters), so the game no longer replaces the en dash and the
  apostrophe. `--check` keeps the set complete against the data and the code. `Source` chunk on
  every page.
- 2026-10-02: UI batch, part U3 (themes): `tools/art/themes.py` renders the title, hangar and
  briefing themes with their chosen generator, which already used the final settings (q6, −14 LUFS,
  sample-exact loop comments). It writes them to `assets/music/` with a `SOURCE` comment, so the
  audio is identical to the chosen files. `importPlaceholders` copies the music through
  `PlaceholderSounds` (`copyPlaceholderMusic`), which keeps final OGGs.
  [Round 13](../../concept-rounds/round-13/README.md) opened for the whole UI batch (U1–U3); no part
  is `final` until the user approves it there.
- 2026-10-02: Concept round 13 closed (user decision): every part of the UI batch (U1–U3) and the Level 01 north arm approved as final; the docs whose art is now entirely final are `art: final` (HUD, main menu, debrief, pause, options), the docs that also cover later art (UI, hangar, briefing, characters, music) record the final assets in their Decisions and keep `chosen`.
- 2026-10-02: M4 plan (user decision): each M4 part gets its production-art round right after it; part A's is round 14, the arsenal's effects (`tools/art/weapon_fx.py`), made by the production generator at once instead of placeholders.
- 2026-10-02: Concept round 14 closed (user decision): M4 part A's effects and the HUD's weapon rows approved as final.
- 2026-10-02: Concept round 15 closed (user decision): M4 part B's art (Level 02) approved as final.
- 2026-10-02: M4 part C batch (Level 03), review files for round 16: `tools/art/vrell_l03.py`
  renders the Spore Bomber (the chosen round-04 model at 72 px, the gas bag breathing over four
  frames), its spore mine (a 2D lime light field, additive), the Whirl Seed (the round-05 seed with
  six blades instead of five, as the six-fold spinner and its 60° loop need, the material patterns
  turning with the blades) and the debris field's wreck chunks (the Earth-orbit station kit broken
  along jagged cuts, posed in the model under the fixed key light); `tools/art/leviathan.py` the
  Leviathan set piece, its second-pass body in three tail-sway frames with every shootable part
  (vents, fins, fluke) as its own sprite and a `-wrecked` one, cut from whole-unit renders by an ID
  pass so occlusion and shadows match, and the whole unit on its diagonal first pass;
  `tools/art/backdrop_l03.py` the Level 03 backdrop from the shared kit. Salvage L (the Leviathan's
  200-credit drop, `pickup-salvage-large`) has no concept model of its own, since the round-09
  salvage L is the crate that stays the hidden crate: `tools/art/pickups.py` grows the salvage chips
  into a cluster of seven (a raised centre chip in a ring of six) with the other pickups'
  presentation. `tools/art/intel.py` adds the sensor portraits of Level 03's new wave enemies, the
  Spore Bomber and the Whirl Seed (the six-blade production seed); the Leviathan, a set piece and
  not one of the waves' enemy types, gets no intel portrait, as the hangar lists only the waves'
  types and draws a silhouette only for the threat profile's boss (none in Level 03). No part is
  `final` until the user approves it in round 16.
- 2026-10-02: M4 part C batch, the death effects the Level 03 units left to this track:
  `tools/art/vrell_fx.py` renders the Spore Bomber's lime spore cloud (additive) and membrane
  tatters (solid), the Whirl Seed's husk split (solid) and teal glint (additive) and the
  Leviathan's ichor cloud (additive, one per wound), each played with the unit's
  explosion-ladder burst; physical pieces are a solid set of their own, ray-marched per frame
  from the units' models, since opaque hide added as light would glow. The Leviathan's
  whale-song cry is synthesized (`tools/concept/audio/sfx_r16.py`). Review files for round 16.
- 2026-10-03: Concept round 16 closed (user decision): M4 part C's art (Level 03) approved as final, except the Leviathan's death effect, which is redone as variant b in round 16 (choice 19); the part D head starts (the Brood Pod and the Scuttler) approved as final too.
- 2026-10-03: Concept round 16 closed again (user decision): the Leviathan's death redo, variant b (choice 19), approved as final: `tools/art/leviathan_death.py` cuts the second-pass body into five chunks under a cluster of large blasts and writes the death's data; with it M4 part C's art is final. The Leviathan's frames are about 1.88 M px, about 7.2 MiB of the 16 MiB one-page budget.
- 2026-10-03: Concept round 17 closed (user decision): M4 part D's art (Level 04) approved as final, the bridge and the terminal hangar (enlarged to 380×300) as the redo of choice 9 and the dugout's and supply drop's production art as choice 10.
