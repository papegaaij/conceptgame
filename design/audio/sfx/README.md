---
title: Sound effects
design: approved
implementation: in-progress
art: chosen
depends-on: [../../player, ../../enemies, ../../ui]
updated: 2026-10-02
---

# Sound effects

## Summary

The full list of sound effects, grouped by category, each with a priority for the first
playable build (P1), the first act (P2), or later (P3). Mixing rules keep the constant player
fire from drowning out everything else.

## Design

### Sourcing

Player shots and explosions use **recorded third-party sounds** instead of synthesis (round 01
decision). Only CC0 or CC-BY sources are used (the repository is public); each file is listed
in [CREDITS.md](../../../CREDITS.md) and CC-BY authors appear on the in-game credits screen.
Imports are reproducible via `tools/concept/audio/import_sfx.py` (concept: Freesound HQ
previews; production: rebuild from the original files). Pickups and UI sounds stay synthesized.
Round 08 follows the same split: recorded hits, player damage, enemy, special, radio-squelch,
klaxon and ambience sounds (`import_sfx.py`); synthesized pickups by type and UI blips in the
family of the chosen round-01 pickups (`tools/concept/audio/sfx_r08.py`). Recorded one-shots are
levelled on the 200 Hz–5 kHz band so sub-heavy sources stay audible on small speakers;
where a source needs EQ (sub-sonic drift, a harsh top end) the high- or low-pass is set and
commented on its `import_sfx.py` entry. A source that is almost all sub-bass is replaced, not
boosted.

### Weapon sound families

Every weapon and weapon-like special plays the sound of its **family**, so a new weapon only
needs a family, and levels of the same weapon differ by pitch and layering, not by new files.
Round 03 proposals; the reused round 02 shots are marked (r02). Files are in `concept/`.

| Family | Weapons (slot) | Specials | Concept files | Character |
|---|---|---|---|---|
| `pulse` | Pulse Cannon (front), Tail Gun, Fan Blaster (rear), Side Splitter (rear), Light Drone Bay shots (wing) | — | [a](concept/player-shot-r02-a.ogg) (r02), [b, heavy](concept/player-shot-r02-b.ogg) (r02) | Short energy projectile; the most-heard sound, so quiet and pitch-varied (±5%). Rear guns play it ~10% lower. |
| `vulcan` | Scatter Vulcan (front) | — | [a](concept/player-shot-r02-e.ogg) (r02), [b](concept/shot-vulcan-r03-b.ogg) | Heavy rotary/autocannon chatter; one sound per volley, not per bullet of the fan. |
| `ballistic` | Autocannon Pod, Swivel Gun (wing) | — | [a](concept/player-shot-r02-d.ogg) (r02) | Recorded single gunshot, dry and short. |
| `laser` | Lance Laser (front), Rear Lance (rear) | — | [a](concept/player-shot-r02-c.ogg) (r02), [b](concept/shot-laser-r03-b.ogg) | Descending energy sweep; piercing weapons sound "longer" than pulse. |
| `beam` | Ion Beam (front) | Orbital Lance | loops: [a](concept/shot-beam-r04-a.ogg) (Ion Beam), [b](concept/shot-beam-r04-b.ogg) (alternative), [c](concept/shot-beam-r04-c.ogg) (Orbital Lance); [start](concept/shot-beam-start-r04-a.ogg), [stop](concept/shot-beam-stop-r04-a.ogg) (round 04) | **Seamless loops** played while firing: start → loop → stop. a = smooth mid-range laser hum (~850 Hz), b = gritty pulsing energy loop, c = piercing ~1.65 kHz ray. The round 03 loops were ~100% below 200 Hz (inaudible on small speakers), so beams now keep ≥ 70% of their energy in 200 Hz–5 kHz and are normalised on that band's RMS (−30 dB) to match the shots' loudness. |
| `missile` | Hornet Launcher (front) | — | [a](concept/shot-missile-r03-a.ogg) | Rocket ignition + whoosh. |
| `micromissile` | Micro-missile Pod (wing), Swarm Tail (rear) | Decoy Flares (pitched up) | [a](concept/shot-micromissile-r03-a.ogg) | Small, quick rocket launch; frequent, so short. |
| `mortar` | Hammer Mortar (front) | — | [a](concept/shot-mortar-r03-a.ogg) | Hollow tube thump for lobbed shells. |
| `bomb` | Bomb Rack (wing) | Airstrike (full-length whistle) | [a](concept/shot-bomb-r03-a.ogg) | Falling-bomb whistle, cut short for the rack; the Airstrike uses the source's full whistle. |
| `torpedo` | Torpedo Pod (wing), Harpoon Torpedoes (front) | Sonar Pulse (see Specials) | [a](concept/shot-torpedo-r03-a.ogg) | Muffled underwater launch with bubbles; above water the same file is played drier/brighter. |
| `mine` | Proximity Mines, Depth Charges (rear) | — | [a](concept/shot-mine-r03-a.ogg) | Metallic drop-and-bounce clunk; mines add an arming beep (UI synth), depth charges a splash. |
| `tesla` | Tesla Coil Pod (wing), Plasma Arc (front) | EMP Burst (layered, longer) | [a](concept/shot-tesla-r03-a.ogg) | Electric zap/crackle; Plasma Arc chains replay it per jump at rising pitch. |
| `resonator` | Choir Resonator (front, captured Vrell tech) | Vrell Swarm Call | [a](concept/shot-resonator-r03-a.ogg) | Big alien energy cannon; later layered with a Choir chord from the music. |
| — | Deflector Pod (wing, defensive) | Smart Bomb, Shield Overcharge, Time Dilation | — | Not shots: use shield/impact and special sounds (see below); Smart Bomb uses the `huge` explosion rung. |

### Explosion ladder

Explosions are chosen by **enemy size**, with a random variant per kill (never the same file
twice in a row) plus ±4% pitch, so repeated kills don't sound identical. Round 03 proposals;
reused round 02 files marked (r02).

| Rung | Length | Files | Enemies (examples from the [roster](../../enemies/README.md)) |
|---|---|---|---|
| `tiny` | ≤ 0.6 s | [a](concept/explosion-tiny-r03-a.ogg), [b](concept/explosion-tiny-r03-b.ogg) | Skitter, Asteroid Mite, spores, shootable missiles and mines, Shard Drone links |
| `small` | 0.7–1.1 s | [a](concept/explosion-r02-a.ogg) (r02), [b](concept/explosion-r02-b.ogg) (r02), [c](concept/explosion-small-r03-a.ogg) | Needler, Stinger, Talon, Ghost Drone, Harrow, Skimmer, Spine Turret, Driftjelly |
| `medium` | 1.6–2 s | [a](concept/explosion-r02-c.ogg) (r02), [b](concept/explosion-medium-r03-b.ogg) | Gilded Gunship, Hornet, Chimera, Mantis, Creeper, SAM Nest, Crawler Tank, Reef Spitter, Minelayer, destroyed buildings |
| `large` | 2.8–3.1 s | [a](concept/explosion-r02-d.ogg) (r02), [b](concept/explosion-r02-e.ogg) (r02), [c](concept/explosion-large-r03-a.ogg) | Hive Node, Sentinel Tower, Rail Bunker, Abyss Ray, Choir Seraph, mid-boss parts, boss phase ends |
| `huge` | 5–6 s | [a](concept/explosion-huge-r03-b.ogg) (sub-heavy boom), [b](concept/explosion-huge-r04-a.ogg) (recorded 4 kg TNT blast with debris, round 04) | Act-boss deaths, capital ships, Smart Bomb, Iron Sovereign core |
| `underwater` | 3–3.5 s | [a, recorded](concept/explosion-underwater-r03-a.ogg), [b](concept/explosion-underwater-r04-a.ogg) (round 04) | Everything below the surface in Act 4 (Europa). The derived low-pass variant was rejected, so under-water sounds need recorded sources |
| `water` | 1.9 s | [a](concept/explosion-water-r03-a.ogg) | Surface naval kills (Act 2 ocean, Europa ice floes), depth-charge hits |

### Player weapons

| Sound | Notes | Priority |
|---|---|---|
| Pulse Cannon shot | Short, bright "pew"; very frequent, so quiet and varied (3 pitch variants) | P1 |
| Scatter Vulcan shot | Rapid chatter | P1 |
| Hornet / micro-missile launch | Whoosh with a small ignition pop | P1 |
| Hammer Mortar / bomb drop | Hollow thunk, whistle down | P2 |
| Lance Laser | Sustained zap with tail | P2 |
| Ion Beam loop | Humming loop with start/stop | P2 |
| Harpoon torpedo / depth charge | Muffled launch, bubbles; underwater boom | P2 |
| Plasma Arc | Crackle | P3 |
| Tail Gun / Fan Blaster | Like the front guns, a little lower | P1 |
| Proximity mine drop / arm | Click + beep | P2 |
| Choir Resonator | Alien chord shot | P3 |
| Overdrive start / end | Power-up surge / power-down | P1 |

### Impacts and explosions

| Sound | Notes | Priority |
|---|---|---|
| Hit: metal | Tick/clank for Ascendancy and machines — [a](concept/hit-metal-r08-a.ogg), [b](concept/hit-metal-r08-b.ogg) | P1 |
| Hit: organic | Wet, chitinous crunch for the Vrell — [a](concept/hit-organic-r08-a.ogg), [b](concept/hit-organic-r08-b.ogg) | P1 |
| Hit: enemy shield | Glassy ping | P2 |
| Explosion small | Popcorn enemies; 3 variants | P1 |
| Explosion medium | 3 variants | P1 |
| Explosion large | Heavy enemies, buildings; with debris tail | P1 |
| Explosion boss | Long, multi-stage, with sub-bass | P2 |
| Explosion underwater | Muffled variants for the Europa act | P2 |
| Ground target destroyed | Crumbling structure — [a](concept/hit-crumble-r08-a.ogg), [b](concept/hit-crumble-r08-b.ogg) | P2 |

### Player ship

| Sound | Notes | Priority |
|---|---|---|
| Shield hit | Electric fizz — [a](concept/player-shield-hit-r08-a.ogg) | P1 |
| Shield break | Descending zap + alarm blip — [a](concept/player-shield-break-r08-a.ogg) | P1 |
| Shield restored | Rising chime — [a](concept/player-shield-restore-r08-a.ogg) | P2 |
| Armour hit | Metallic crunch — [a](concept/player-armour-hit-r08-a.ogg) | P1 |
| Low armour warning | Beeps (slow at 30 %, fast at 15 %) — [a](concept/player-low-armour-r08-a.ogg) | P1 |
| Ship destroyed | Big explosion, then the music sting — [a](concept/player-destroyed-r08-a.ogg) | P1 |
| Dash | Thruster burst | P3 |
| Engine hum | Subtle loop (optional) | P3 |

### Pickups

| Sound | Priority |
|---|---|
| Salvage small / medium / large (rising pitch) — small [a](concept/pickup-salvage-small-r08-a.ogg), medium [pickup-r01-c](concept/pickup-r01-c.ogg), large [a](concept/pickup-salvage-large-r08-a.ogg) | P1 |
| Shield cell — [a](concept/pickup-shield-cell-r08-a.ogg) | P1 |
| Armour patch — [a](concept/pickup-armour-patch-r08-a.ogg) | P2 |
| Special charge — [a](concept/pickup-special-charge-r08-a.ogg) | P2 |
| Overdrive pickup — [pickup-r01-a](concept/pickup-r01-a.ogg); start/end cues [a](concept/overdrive-start-r08-a.ogg), [a](concept/overdrive-end-r08-a.ogg) | P1 |
| Data core (distinct, rewarding) — [pickup-r01-b](concept/pickup-r01-b.ogg) | P2 |

### Specials

| Sound | Priority |
|---|---|
| Airstrike: radio call (radio squelch + text), jets flyby [a](concept/special-airstrike-jets-r08-a.ogg), bomb carpet [a](concept/special-airstrike-bombs-r08-a.ogg) | P1 |
| Smart bomb: charge-up + white-out boom — [a](concept/special-smartbomb-r08-a.ogg) | P1 |
| EMP: electric thump + power-down whine | P2 |
| Decoy flares — [a](concept/special-flares-r08-a.ogg) | P2 |
| Orbital lance: charge + sustained beam | P3 |
| Sonar pulse ping — [a](concept/special-sonar-r04-a.ogg) (clean single ping); round 04 | P2 |
| Shield overcharge | P3 |
| Time dilation: slow-down / speed-up sweeps | P3 |
| Special unavailable (denied buzz) — [a](concept/special-denied-r08-a.ogg) | P1 |

### Enemies

| Sound | Priority |
|---|---|
| Enemy shot: small [a](concept/enemy-shot-small-r08-a.ogg), [b](concept/enemy-shot-small-r08-b.ogg) / heavy [a](concept/enemy-shot-heavy-r08-a.ogg), [b](concept/enemy-shot-heavy-r08-b.ogg) / laser charge warning [a](concept/enemy-laser-warning-r08-a.ogg) | P1 |
| Missile launch (enemy) — [a](concept/enemy-missile-r08-a.ogg) | P1 |
| Vrell screech (spawn/attack cue), 2 variants: [c](concept/enemy-screech-r08-c.ogg), [d](concept/enemy-screech-r08-d.ogg) | P2 |
| Turret rotate / lock-on beep — [a](concept/enemy-lock-r08-a.ogg) | P2 |
| Portal / warp-in; Vrell spawn (Brood Pod bursting, Hive Node and Brood Carrier spawns) — [a](concept/enemy-spawn-r08-a.ogg) (wet creature swell), [b](concept/enemy-spawn-r08-b.ogg) (fleshy burst) | P2 |
| Carrier launching drones | P3 |
| Boss roars and phase-change cues (per boss) | P3 |

### UI and radio

| Sound | Priority |
|---|---|
| Menu move / confirm / back — [a](concept/ui-menu-move-r08-a.ogg) / [a](concept/ui-menu-confirm-r08-a.ogg) / [a](concept/ui-menu-back-r08-a.ogg) | P1 |
| Buy / sell / equip / upgrade / can't afford / won't fit (power) — buy [a](concept/ui-shop-buy-r08-a.ogg), sell [a](concept/ui-shop-sell-r08-a.ogg), can't afford / won't fit [a](concept/ui-shop-denied-r08-a.ogg), equip [a](concept/ui-equip-r08-a.ogg), upgrade [a](concept/ui-upgrade-r08-a.ogg) | P1 |
| Save done | P2 |
| Typewriter blip (briefing text) — [a](concept/ui-typewriter-r08-a.ogg) | P1 |
| Radio squelch open / close — [a](concept/ui-radio-open-r08-a.ogg) / [a](concept/ui-radio-close-r08-a.ogg) | P1 |
| Warning klaxon (boss, rear attack) — [a](concept/ui-klaxon-r08-a.ogg) (seamless loop; a single blast can be cut from it) | P1 |
| Debrief tally tick / grade stamp — tick [a](concept/ui-tally-tick-r08-a.ogg), total [a](concept/ui-tally-total-r08-a.ogg), grade stamp [a](concept/ui-grade-stamp-r08-a.ogg) | P2 |

### Ambience (per setting)

Space hum, orbital station creaks, city wind and sirens, Martian dust wind, underwater drone and
whale-like calls under Europa's ice, asteroid rumble, Jovian storm, alien pulsing beyond the
gate. P2–P3, one loop per setting in the [world](../../world/README.md).

| Setting | Loop (round 08) |
|---|---|
| Earth orbit | [a](concept/ambience-orbit-r08-a.ogg) — space drone, 16 s |
| Luna | [a](concept/ambience-luna-r08-a.ogg) — desolate space-wind drone, 16 s |
| Earth megacity | [a](concept/ambience-city-r08-a.ogg) — night city with distant sirens, 20 s |
| Earth ocean | [a](concept/ambience-ocean-r08-a.ogg) — waves at speed, 16 s |
| Earth ocean storm | [a](concept/ambience-storm-r08-a.ogg) — rain and thunder, 24 s |
| Earth arctic | [a](concept/ambience-arctic-r08-a.ogg) — cold wind, 16 s |

### Mixing rules

- **Voice limit**: 32 simultaneous voices. Per-sound instance limits: player fire 2, small
  explosions 6, hits 4. Beyond the limit, the oldest instance is stolen.
- **Priority** when stealing voices: warnings and player damage > boss sounds > explosions > enemy
  fire > player fire > pickups > ambience.
- **Levels** (relative, first draft): player fire −12 dB, enemy fire −9 dB, explosions 0 dB,
  player damage and warnings +2 dB, pickups −6 dB, enemy hits −6 dB (8 dB below player damage).
- **Variation**: every frequent sound gets ±5 % random pitch and 2–3 variants.
- **Stereo**: pan by horizontal play-field position, subtle (max ±40 %).
- **Underwater**: a low-pass filter on the sfx bus in `sub` settings, and muffled variants for
  explosions.

## Concept art

Concept round 01 — see [round 01](../../concept-rounds/round-01/README.md). Briefs and
AI-generator prompts: [concept/prompts.md](concept/prompts.md). Generated by
`tools/concept/audio/sfx.py`. Levels: shots peak at −10 dBFS, pickups −4 to −7, explosions −1.5.

| File | What | Status |
|---|---|---|
| [concept/rejected/player-shot-r01-a.ogg](concept/rejected/player-shot-r01-a.ogg) | Classic laser "pew", descending square, 0.14 s | rejected — synthesized shots not good enough; source real recordings |
| [concept/rejected/player-shot-r01-b.ogg](concept/rejected/player-shot-r01-b.ogg) | Pulse cannon: low thump with a crisp snap, 0.12 s | rejected — synthesized shots not good enough; source real recordings |
| [concept/rejected/player-shot-r01-c.ogg](concept/rejected/player-shot-r01-c.ogg) | Plasma bolt "zwip", rise-fall with small stereo echo, 0.23 s | rejected — synthesized shots not good enough; source real recordings |
| [concept/rejected/explosion-r01-a.ogg](concept/rejected/explosion-r01-a.ogg) | Small crunchy 8-bit pop, abrupt cut, 0.62 s | rejected — must sound more realistic; source real recordings |
| [concept/rejected/explosion-r01-b.ogg](concept/rejected/explosion-r01-b.ogg) | Big boom with sub drop and debris tail, 1.45 s | rejected — must sound more realistic; source real recordings |
| [concept/rejected/explosion-r01-c.ogg](concept/rejected/explosion-r01-c.ogg) | Sci-fi plasma "whoom" with swirling stereo (Vrell flavour), 1.25 s | rejected — must sound more realistic; source real recordings |
| [concept/pickup-r01-a.ogg](concept/pickup-r01-a.ogg) | Classic rising 4-note power-up arpeggio, 0.57 s | chosen — standard power-ups |
| [concept/pickup-r01-b.ogg](concept/pickup-r01-b.ogg) | Two-bell chime with echo (rare upgrades), 0.75 s | chosen — rare upgrades |
| [concept/pickup-r01-c.ogg](concept/pickup-r01-c.ogg) | Rising synth sweep with sparkles (credits), 0.50 s | chosen — credits |

Concept round 02 — recorded sounds from Freesound, imported by
`tools/concept/audio/import_sfx.py`; sources and licences in [CREDITS.md](../../../CREDITS.md).

| File | What | Status |
|---|---|---|
| [concept/player-shot-r02-a.ogg](concept/player-shot-r02-a.ogg) | "Projectile Shoot" by unfa (CC0 1.0) — Pulse Cannon (starter front gun) | chosen — starting set, weapon mapping as described |
| [concept/player-shot-r02-b.ogg](concept/player-shot-r02-b.ogg) | "Sci Fi Gun Shot" by Bird_man (CC0 1.0) — heavy front gun / Hammer Mortar | chosen — starting set, weapon mapping as described |
| [concept/player-shot-r02-c.ogg](concept/player-shot-r02-c.ogg) | "laser3" by nsstudios (CC-BY 4.0) — Lance Laser / light laser weapons | chosen — starting set, weapon mapping as described |
| [concept/player-shot-r02-d.ogg](concept/player-shot-r02-d.ogg) | "Machine Gun 001 - single shot" by pgi (CC0 1.0) — Autocannon Pod / ballistic guns | chosen — starting set, weapon mapping as described |
| [concept/player-shot-r02-e.ogg](concept/player-shot-r02-e.ogg) | "Autocannon Three Shot Burst" by qubodup (CC0 1.0) — Scatter Vulcan / heavy ballistic front gun | chosen — starting set, weapon mapping as described |
| [concept/explosion-r02-a.ogg](concept/explosion-r02-a.ogg) | "small explosion" by bevibeldesign (CC0 1.0) — small enemy destroyed (fighters, drones) | chosen — starting set for the size ladder |
| [concept/explosion-r02-b.ogg](concept/explosion-r02-b.ogg) | "Explosion 1" by magnuswaker (CC0 1.0) — small-to-medium enemy (heavy fighters, turrets) | chosen — starting set for the size ladder |
| [concept/explosion-r02-c.ogg](concept/explosion-r02-c.ogg) | "Explosion" by qubodup (CC0 1.0) — medium enemy (gunships, ground vehicles, buildings) | chosen — starting set for the size ladder |
| [concept/explosion-r02-d.ogg](concept/explosion-r02-d.ogg) | "Nearby explosion with debris" by juskiddink (CC-BY 4.0) — large enemy / mid-boss / building collapse | chosen — starting set for the size ladder |
| [concept/explosion-r02-e.ogg](concept/explosion-r02-e.ogg) | "explosion_big_01" by derplayer (CC0 1.0) — boss destroyed / capital ship | chosen — starting set for the size ladder |

Concept round 03 — per-weapon shot families and the explosion ladder (see Design above),
recorded sounds from Freesound imported by `tools/concept/audio/import_sfx.py`; sources in
[CREDITS.md](../../../CREDITS.md), briefs in [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/shot-vulcan-r03-b.ogg](concept/shot-vulcan-r03-b.ogg) | "minigun.wav" by pgi (CC0 1.0) — `vulcan` b, rotary chatter | chosen |
| [concept/shot-laser-r03-b.ogg](concept/shot-laser-r03-b.ogg) | "Laser shot.wav" by michael_grinnell (CC0 1.0) — `laser` b | chosen |
| [concept/rejected/shot-beam-r03-a.ogg](concept/rejected/shot-beam-r03-a.ogg) | "heavy beam weapon" by a deleted Freesound user (CC0 1.0) — `beam` a, 1.4 s seamless loop (Ion Beam) | rejected — not good |
| [concept/rejected/shot-beam-r03-b.ogg](concept/rejected/shot-beam-r03-b.ogg) | "SFX Oscilating Laser Beam" by bolkmar (CC-BY 4.0) — `beam` b, 2.62 s seamless loop (Orbital Lance) | rejected — barely audible |
| [concept/shot-missile-r03-a.ogg](concept/shot-missile-r03-a.ogg) | "Rocket Launch" by Jarusca (CC0 1.0) — `missile` (Hornet Launcher) | chosen |
| [concept/shot-micromissile-r03-a.ogg](concept/shot-micromissile-r03-a.ogg) | "Rocket Shots" by Audionautics (CC-BY 3.0) — `micromissile`, first shot | chosen |
| [concept/shot-mortar-r03-a.ogg](concept/shot-mortar-r03-a.ogg) | "Mortar Shots.flac" by qubodup (CC0 1.0) — `mortar`, first shot | chosen |
| [concept/shot-bomb-r03-a.ogg](concept/shot-bomb-r03-a.ogg) | "Falling Bomb.wav" by Daleonfire (CC0 1.0) — `bomb`, whistle cut to 1.2 s | chosen |
| [concept/shot-torpedo-r03-a.ogg](concept/shot-torpedo-r03-a.ogg) | "Torpedo launch underwater.wav" by jobro (CC-BY 3.0) — `torpedo` | chosen |
| [concept/shot-mine-r03-a.ogg](concept/shot-mine-r03-a.ogg) | "small metal object fall" by nicktermer (CC0 1.0) — `mine` drop-and-bounce | chosen |
| [concept/shot-tesla-r03-a.ogg](concept/shot-tesla-r03-a.ogg) | "Electric zap.wav" by michael_grinnell (CC0 1.0) — `tesla` | chosen |
| [concept/shot-resonator-r03-a.ogg](concept/shot-resonator-r03-a.ogg) | "sci-fi cannon" by humanoide9000 (CC-BY 4.0) — `resonator` (Choir Resonator) | chosen |
| [concept/explosion-tiny-r03-a.ogg](concept/explosion-tiny-r03-a.ogg) | "Small Explosion" by Cyberios (CC0 1.0) — `tiny` a | chosen |
| [concept/explosion-tiny-r03-b.ogg](concept/explosion-tiny-r03-b.ogg) | "Small explosion" by dinodilopho (CC0 1.0) — `tiny` b | chosen |
| [concept/rejected/explosion-tiny-r03-c.ogg](concept/rejected/explosion-tiny-r03-c.ogg) | "Firecracker Explosion" by unfa (CC0 1.0) — `tiny` c, sharp crack | rejected — not good |
| [concept/explosion-small-r03-a.ogg](concept/explosion-small-r03-a.ogg) | "Small Explosion" by lorenzgillner (CC0 1.0) — `small` c | chosen |
| [concept/rejected/explosion-medium-r03-a.ogg](concept/rejected/explosion-medium-r03-a.ogg) | "Air Explosion.wav" by 1histori (CC0 1.0) — `medium` b | rejected — not good |
| [concept/explosion-medium-r03-b.ogg](concept/explosion-medium-r03-b.ogg) | "Explode001" by mitchelk (CC0 1.0) — `medium` c | chosen |
| [concept/explosion-large-r03-a.ogg](concept/explosion-large-r03-a.ogg) | "explosion_big_02" by derplayer (CC0 1.0) — `large` c | chosen |
| [concept/rejected/explosion-huge-r03-a.ogg](concept/rejected/explosion-huge-r03-a.ogg) | "Explosion_01.wav" by tommccann (CC0 1.0) — `huge` a, 5 s | rejected — not good |
| [concept/explosion-huge-r03-b.ogg](concept/explosion-huge-r03-b.ogg) | "Big Boom" by unfa (CC0 1.0) — `huge` b, 6 s, deep sub-bass | chosen |
| [concept/explosion-underwater-r03-a.ogg](concept/explosion-underwater-r03-a.ogg) | "underwater explosion.wav" by cubix (CC0 1.0) — `underwater` a, recorded | chosen |
| [concept/rejected/explosion-underwater-r03-b.ogg](concept/rejected/explosion-underwater-r03-b.ogg) | "Explosion" by qubodup (CC0 1.0) through a 4-pole 500 Hz low-pass — `underwater` b, derived | rejected — not good (derived low-pass) |
| [concept/explosion-water-r03-a.ogg](concept/explosion-water-r03-a.ogg) | "Water Explosion" by Sheyvan (CC0 1.0) — `water`, surface naval kills | chosen |

Concept round 04 — audible beam loops with start/stop, Sonar Pulse, extra `huge` and
`underwater` explosions; imported by `tools/concept/audio/import_sfx.py`, sources in
[CREDITS.md](../../../CREDITS.md), briefs in [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/shot-beam-r04-a.ogg](concept/shot-beam-r04-a.ogg) | "laser beam" by peepholecircus (CC0 1.0) — `beam` loop a, 2.6 s, smooth ~850 Hz hum (Ion Beam) | chosen |
| [concept/shot-beam-r04-b.ogg](concept/shot-beam-r04-b.ogg) | "Weapons Beam Loop" by unfa (CC0 1.0) — `beam` loop b, 1.9 s, gritty pulsing texture | chosen |
| [concept/shot-beam-r04-c.ogg](concept/shot-beam-r04-c.ogg) | "SonicDeathRay_1.2KHzNoCrackle" by zimbot (CC-BY 4.0) — `beam` loop c, 2.0 s, piercing ray (Orbital Lance) | chosen |
| [concept/shot-beam-start-r04-a.ogg](concept/shot-beam-start-r04-a.ogg) | "Machine Charge" by Glitchedtones (CC0 1.0) — `beam` start, 0.9 s rising charge | chosen |
| [concept/shot-beam-stop-r04-a.ogg](concept/shot-beam-stop-r04-a.ogg) | "Power Down" by noirenex (CC0 1.0) — `beam` stop, 1.3 s power-down | chosen |
| [concept/special-sonar-r04-a.ogg](concept/special-sonar-r04-a.ogg) | "Sonar Ping" by SamsterBirdies (CC0 1.0) — Sonar Pulse, clean single ping | chosen |
| [concept/rejected/special-sonar-r04-b.ogg](concept/rejected/special-sonar-r04-b.ogg) | "Ping!" by unfa (CC0 1.0) — Sonar Pulse, long ringing ping | rejected — A is enough |
| [concept/explosion-huge-r04-a.ogg](concept/explosion-huge-r04-a.ogg) | "Explosion with debris - authentic. 4kg TNT" by sidohzen (CC0 1.0) — `huge` b, 6 s real blast | chosen |
| [concept/explosion-underwater-r04-a.ogg](concept/explosion-underwater-r04-a.ogg) | "underwater explosion" by mokasza (CC-BY 4.0) — `underwater` b | chosen |

Concept [round 08](../../concept-rounds/round-08/README.md) — the remaining Acts 1–2 sounds: recorded hits, player damage, enemy, special, radio, klaxon and ambience sounds (`tools/concept/audio/import_sfx.py`, sources in [CREDITS.md](../../../CREDITS.md)) and synthesized pickups and UI blips (`tools/concept/audio/sfx_r08.py`). Briefs and AI prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/hit-metal-r08-a.ogg](concept/hit-metal-r08-a.ogg) | "HeavyBulletPing.mp3" by wilhellboy (CC0 1.0) — Hit: metal — bullet ping on Ascendancy hulls and machines | chosen |
| [concept/hit-metal-r08-b.ogg](concept/hit-metal-r08-b.ogg) | "Bullet Hit Metal" by coolguy244e (CC0 1.0) — Hit: metal, variant | chosen |
| [concept/hit-organic-r08-a.ogg](concept/hit-organic-r08-a.ogg) | "splat.ogg" by gprosser (CC0 1.0) — Hit: organic — wet hit on Vrell chitin | chosen |
| [concept/hit-organic-r08-b.ogg](concept/hit-organic-r08-b.ogg) | "cracking.wav" by smidoid (CC-BY 4.0) — Hit: organic, chitin crunch | chosen |
| [concept/hit-crumble-r08-a.ogg](concept/hit-crumble-r08-a.ogg) | "building_collapse02_close.wav" by onteca (CC-BY 3.0) — Ground target destroyed — crumbling structure | chosen |
| [concept/hit-crumble-r08-b.ogg](concept/hit-crumble-r08-b.ogg) | "Bricks/Stones/Rocks/Gravel Falling" by iwanPlays (CC0 1.0) — Ground target destroyed, small (rubble burst); replaces "Rock Smash" by NeoSpica, which was almost all sub-bass | chosen |
| [concept/player-shield-hit-r08-a.ogg](concept/player-shield-hit-r08-a.ogg) | "ELECTRIC_ZAP_001.wav" by JoelAudio (CC0 1.0) — Shield hit — electric fizz, low-passed at 7 kHz | chosen |
| [concept/rejected/player-shield-hit-r08-b.ogg](concept/rejected/player-shield-hit-r08-b.ogg) | "Sci-Fi Force Field Impact 15.wav" by StormwaveAudio (CC-BY 4.0) — Shield hit, force-field variant | rejected (user, round 08) |
| [concept/player-shield-break-r08-a.ogg](concept/player-shield-break-r08-a.ogg) | "Synthesized_Pitch-Down_Zap" by joe_bou_khalil (CC-BY 4.0) — Shield break — descending zap | chosen |
| [concept/player-shield-restore-r08-a.ogg](concept/player-shield-restore-r08-a.ogg) | "Power Up Charge [Remix of LegoLunatic's Charged laser 151243]" by qubodup (CC0 1.0) — Shield restored — rising charge | chosen |
| [concept/rejected/player-shield-restore-r08-b.ogg](concept/rejected/player-shield-restore-r08-b.ogg) | "Shield recharging" by Bychop (CC0 1.0) — Shield restored, longer recharge | rejected (user, round 08) |
| [concept/player-armour-hit-r08-a.ogg](concept/player-armour-hit-r08-a.ogg) | "Impact on metal" by JoMungus (CC0 1.0) — Armour hit — metallic crunch | chosen |
| [concept/player-low-armour-r08-a.ogg](concept/player-low-armour-r08-a.ogg) | "Bleeper 1" by magnuswaker (CC0 1.0) — Low armour warning — one beep | chosen |
| [concept/player-destroyed-r08-a.ogg](concept/player-destroyed-r08-a.ogg) | "spaceship explosion9.WAV" by phantastonia (CC-BY 4.0) — Ship destroyed | chosen |
| [concept/overdrive-start-r08-a.ogg](concept/overdrive-start-r08-a.ogg) | "Spacey 1up/Power up" by GameAudio (CC0 1.0) — Overdrive start | chosen |
| [concept/overdrive-end-r08-a.ogg](concept/overdrive-end-r08-a.ogg) | "Teleport Error" by Jerimee (CC0 1.0) — Overdrive end | chosen |
| [concept/enemy-shot-small-r08-a.ogg](concept/enemy-shot-small-r08-a.ogg) | "Sci-fi gun shot x6" by humanoide9000 (CC0 1.0) — Enemy shot, small | chosen |
| [concept/enemy-shot-small-r08-b.ogg](concept/enemy-shot-small-r08-b.ogg) | "retro shot blaster" by JavierZumer (CC-BY 4.0) — Enemy shot, small (retro blaster) | chosen |
| [concept/enemy-shot-heavy-r08-a.ogg](concept/enemy-shot-heavy-r08-a.ogg) | "ScifiHeavyBlasterShot.wav" by SuperPhat (CC0 1.0) — Enemy shot, heavy | chosen |
| [concept/enemy-shot-heavy-r08-b.ogg](concept/enemy-shot-heavy-r08-b.ogg) | "Heavy blaster shot 05" by xkeril (CC0 1.0) — Enemy shot, heavy b — bassy blaster with a falling sweep | chosen |
| [concept/enemy-laser-warning-r08-a.ogg](concept/enemy-laser-warning-r08-a.ogg) | "Laser Charging" by plasterbrain (CC0 1.0) — Enemy laser charge warning | chosen |
| [concept/enemy-missile-r08-a.ogg](concept/enemy-missile-r08-a.ogg) | "Missile firing fl.mp3" by NHMWretched (CC0 1.0) — Enemy missile launch | chosen |
| [concept/rejected/enemy-screech-r08-a.ogg](concept/rejected/enemy-screech-r08-a.ogg) | "Monster screech" by Khrinx (CC0 1.0) — Vrell screech (spawn/attack cue) a | rejected (user, round 08) |
| [concept/rejected/enemy-screech-r08-b.ogg](concept/rejected/enemy-screech-r08-b.ogg) | "inhuman screech.wav" by Wolfsinger (CC-BY 4.0) — Vrell screech b | rejected (user, round 08) |
| [concept/enemy-screech-r08-c.ogg](concept/enemy-screech-r08-c.ogg) | "alien4.wav" by AlienXXX (CC-BY 4.0) — Vrell screech c | chosen |
| [concept/enemy-screech-r08-d.ogg](concept/enemy-screech-r08-d.ogg) | "alien screech.wav" by jvmyka@gmail.com (CC0 1.0) — Vrell screech d — reversed, processed horse sounds | chosen |
| [concept/enemy-spawn-r08-a.ogg](concept/enemy-spawn-r08-a.ogg) | "DCA Alien Spawning Birth.aif" by darcyadam (CC0 1.0) — Vrell spawn a — wet creature swell (Hive Node / Brood Carrier spawns) | chosen |
| [concept/enemy-spawn-r08-b.ogg](concept/enemy-spawn-r08-b.ogg) | "goreSplat.wav" by ThefitzyG (CC0 1.0) — Vrell spawn b — fleshy burst (Brood Pod bursting) | chosen |
| [concept/enemy-lock-r08-a.ogg](concept/enemy-lock-r08-a.ogg) | "lock on" by SamsterBirdies (CC0 1.0) — Turret lock-on beep | chosen |
| [concept/special-airstrike-jets-r08-a.ogg](concept/special-airstrike-jets-r08-a.ogg) | "Jet Plane Flyby.flac" by qubodup (CC0 1.0) — Airstrike: jets flyby | chosen |
| [concept/special-airstrike-bombs-r08-a.ogg](concept/special-airstrike-bombs-r08-a.ogg) | "R11-55-Large Blasts.wav" by craigsmith (CC0 1.0) — Airstrike: bomb carpet | chosen |
| [concept/special-smartbomb-r08-a.ogg](concept/special-smartbomb-r08-a.ogg) | "Energy Blast" by Kinoton (CC0 1.0) — Smart bomb: charge-up + white-out boom | chosen |
| [concept/special-flares-r08-a.ogg](concept/special-flares-r08-a.ogg) | "Guns & Explosions Album - Flare gun 5-2.wav" by OGsoundFX (CC-BY 4.0) — Decoy flares | chosen |
| [concept/special-denied-r08-a.ogg](concept/special-denied-r08-a.ogg) | "acess denied buzz" by Jacco18 (CC0 1.0) — Special unavailable (denied buzz) | chosen |
| [concept/ui-radio-open-r08-a.ogg](concept/ui-radio-open-r08-a.ogg) | "Power On.wav" by JustinBW (CC-BY 4.0) — Radio squelch open | chosen |
| [concept/ui-radio-close-r08-a.ogg](concept/ui-radio-close-r08-a.ogg) | "Radio Sign Off / Squelch" by JovianSounds (CC0 1.0) — Radio squelch close | chosen |
| [concept/ui-klaxon-r08-a.ogg](concept/ui-klaxon-r08-a.ogg) | "Sci-Fi Alarm" by noirenex (CC0 1.0) — Warning klaxon (boss, rear attack) — loop | chosen |
| [concept/rejected/ui-klaxon-r08-b.ogg](concept/rejected/ui-klaxon-r08-b.ogg) | "RedAlert_Klaxon_STTOS_recreated.wav" by zimbot (CC-BY 4.0) — Warning klaxon, single blast | rejected (user, round 08) |
| [concept/ambience-orbit-r08-a.ogg](concept/ambience-orbit-r08-a.ogg) | "spacedrone3.wav" by Elektrocell (CC0 1.0) — Ambience: Earth orbit (space hum) | chosen |
| [concept/ambience-luna-r08-a.ogg](concept/ambience-luna-r08-a.ogg) | "drone Space wind scifi.wav" by ztitchez (CC-BY 4.0) — Ambience: Luna | chosen |
| [concept/ambience-city-r08-a.ogg](concept/ambience-city-r08-a.ogg) | "201110 Distant sirens, urban, night, quiet, roof 11pm.flac" by TRP (CC0 1.0) — Ambience: megacity | chosen |
| [concept/ambience-ocean-r08-a.ogg](concept/ambience-ocean-r08-a.ogg) | "Ocean waves hitting bow of moving boat." by byjoshberry (CC-BY 4.0) — Ambience: ocean | chosen |
| [concept/ambience-storm-r08-a.ogg](concept/ambience-storm-r08-a.ogg) | "Rain and Thunder 4" by FlatHill (CC0 1.0) — Ambience: ocean storm (rain + thunder) | chosen |
| [concept/ambience-arctic-r08-a.ogg](concept/ambience-arctic-r08-a.ogg) | "Wind__Artic__Cold.wav" by cobratronik (CC0 1.0) — Ambience: arctic wind | chosen |
| [concept/pickup-salvage-small-r08-a.ogg](concept/pickup-salvage-small-r08-a.ogg) | Synthesized — Salvage small — short, high PWM blip with one sparkle (pickup-r01-c family, which itself is salvage medium) | chosen |
| [concept/pickup-salvage-large-r08-a.ogg](concept/pickup-salvage-large-r08-a.ogg) | Synthesized — Salvage large — long PWM sweep two octaves up with an octave layer and a sparkle shower | chosen |
| [concept/pickup-shield-cell-r08-a.ogg](concept/pickup-shield-cell-r08-a.ogg) | Synthesized — Shield cell — cool rising triangle arpeggio (G major) with a chorus shimmer | chosen |
| [concept/pickup-armour-patch-r08-a.ogg](concept/pickup-armour-patch-r08-a.ogg) | Synthesized — Armour patch — low square 'clunk' plus a metallic ding: a plate bolted on | chosen |
| [concept/pickup-special-charge-r08-a.ogg](concept/pickup-special-charge-r08-a.ogg) | Synthesized — Special charge — three rising square notes ending in a bell | chosen |
| [concept/ui-menu-move-r08-a.ogg](concept/ui-menu-move-r08-a.ogg) | Synthesized — Menu move — soft 50 ms triangle blip | chosen |
| [concept/ui-menu-confirm-r08-a.ogg](concept/ui-menu-confirm-r08-a.ogg) | Synthesized — Menu confirm — two rising square notes (E6–B6) | chosen |
| [concept/ui-menu-back-r08-a.ogg](concept/ui-menu-back-r08-a.ogg) | Synthesized — Menu back — two falling square notes (B5–E5) | chosen |
| [concept/ui-shop-buy-r08-a.ogg](concept/ui-shop-buy-r08-a.ogg) | Synthesized — Shop buy — blip plus coin sparkle ('ka-ching') | chosen |
| [concept/ui-shop-sell-r08-a.ogg](concept/ui-shop-sell-r08-a.ogg) | Synthesized — Shop sell — three descending coin blips | chosen |
| [concept/ui-shop-denied-r08-a.ogg](concept/ui-shop-denied-r08-a.ogg) | Synthesized — Shop denied (can't afford / won't fit) — low beating square buzz | chosen |
| [concept/ui-typewriter-r08-a.ogg](concept/ui-typewriter-r08-a.ogg) | Synthesized — Typewriter blip — 30 ms click-blip, played per character of briefing text | chosen |
| [concept/ui-tally-tick-r08-a.ogg](concept/ui-tally-tick-r08-a.ogg) | Synthesized — Debrief tally tick — 30 ms high sine tick | chosen |
| [concept/ui-tally-total-r08-a.ogg](concept/ui-tally-total-r08-a.ogg) | Synthesized — Debrief total — C-major bell chord stinger | chosen |
| [concept/ui-equip-r08-a.ogg](concept/ui-equip-r08-a.ogg) | Synthesized — Hangar equip — square clunk, latch click and a quiet rising two-note blip | chosen |
| [concept/ui-upgrade-r08-a.ogg](concept/ui-upgrade-r08-a.ogg) | Synthesized — Hangar upgrade — short PWM rise landing on a G6 bell | chosen |
| [concept/ui-grade-stamp-r08-a.ogg](concept/ui-grade-stamp-r08-a.ogg) | Synthesized — Debrief grade stamp — punchy thump with a paper slap and a low bell | chosen |

## Implementation

- [ ] SFX playback with instance limits, stealing by priority, pitch variation
- [x] Stereo panning by play-field X
- [ ] Underwater low-pass on the sfx bus
- [ ] All P1 sounds

## Open questions

- (M2) Level 01's launch rail sound has no file yet (no chosen or proposed effect).

## Decisions

- 2026-09-30: Priorities P1–P3 guide production order; player fire is deliberately quiet.
- 2026-09-30: Concept round 01: all three pickups chosen, one per item type — A standard power-ups, B rare upgrades, C credits.
- 2026-09-30: Concept round 01: all synthesized player shots and explosions rejected. Explosions must be somewhat realistic; source recorded sound effects from free online libraries instead.
- 2026-09-30: Concept round 02: recorded shots and explosions are much better; all ten kept as a starting set. Needed next: a distinct shot sound per weapon type (see [weapons](../../player/weapons/README.md)) and more explosions covering the full range from small pops to large booms.
- 2026-09-30: Concept round 03: rejected huge-a, medium-a, tiny-c, underwater-b and both beam loops (B barely audible); all other r03 shots and explosions chosen. The `beam` family needs new sources (round 04).
- 2026-09-30: Concept round 04: all new sounds chosen (beam loops, start/stop, huge and under-water explosions, sonar A); sonar B rejected — A is enough.
- 2026-10-01: Concept round 08: remaining Acts 1–2 sounds proposed — 40 recorded (CC0/CC-BY, band-levelled; five sub-bass-only candidates rejected) and 14 synthesized pickups and UI blips. Overdrive pickup maps to pickup-r01-a, data core to pickup-r01-b, salvage medium to pickup-r01-c. Still open: equip/upgrade and grade-stamp UI sounds, a fourth Vrell screech, an Airstrike radio call (text + squelch for now, no voice).
- 2026-10-01: Confirmed by the user: pickups and UI sounds stay synthesized; everything else (shots, explosions, hits, enemies, specials, radio, klaxon, ambience) uses recorded CC0/CC-BY sounds.
- 2026-10-01: Round 08 verified (peak, 200 Hz–5 kHz band level, DC, edges, loop seams, spectrograms) and fixed: bell and shimmer tails in four synthesized sounds (armour patch, special charge, shield cell, debrief total) ended in a click — the bell now has a release and the armour patch is longer; ship destroyed is high-passed at 20 Hz (sub-sonic drift left a DC offset); shield hit a is low-passed at 7 kHz (83 % of its energy was above 5 kHz); crumble b got a new source (the first was almost all sub-bass and stayed ~8 dB quieter than a on its audible band, even high-passed). Added the missing Acts 1–2 sounds: heavy enemy shot b, Vrell screech d, two Vrell spawn sounds, and synthesized equip, upgrade and grade-stamp UI sounds. Rejected on licence grounds: Artninja's "morphing burst" (built from Warner Bros and Zapsplat library sounds). `sfx.py` now writes the rejected round-01 shots and explosions to `concept/rejected/`.
- 2026-10-01: Concept round 08: all round-08 sounds chosen except screech a and b, shield hit b, shield restore b and klaxon b (rejected).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-01: M1: the spike's `SfxBank` is carried over (64 OpenAL sources, played only from the render thread because libGDX's source pool is not thread-safe) and now enforces the per-sound instance limits by stopping the oldest instance. Levels relative to player damage: explosions −2 dB, hits −8 dB (the mixing rules give no level for hits), player fire −14 dB; ±5 % pitch (±4 % explosions), two-variant sounds alternate, pan up to ±40 % by play-field x. Voice stealing by priority is not done yet. Copies of the used files are in `assets/sfx/` (rows in CREDITS.md).
- 2026-10-01: Enemy-hit level added to the mixing rules (user decision): −6 dB on this scale, 8 dB below player damage, as the M1 SFX player uses.
- 2026-10-02: M2 imports the Level 01 sounds into `assets/sfx` (`:pipeline:importPlaceholders`,
  CREDITS.md rows for the third-party ones): small enemy shots, the small explosion family for
  Needlers and containers, metal hits for ground objects, the salvage, shield cell and armour patch
  pickups (salvage large for the hidden crate), radio squelch and typing, the debrief tick, total
  and stamp, and the Earth-orbit ambience loop.
