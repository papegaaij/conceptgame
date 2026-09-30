---
title: Sound effects
design: draft
implementation: not-started
art: chosen
depends-on: [../../player, ../../enemies, ../../ui]
updated: 2026-09-30
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
| Hit: metal | Tick/clank for Ascendancy and machines | P1 |
| Hit: organic | Wet, chitinous crunch for the Vrell | P1 |
| Hit: enemy shield | Glassy ping | P2 |
| Explosion small | Popcorn enemies; 3 variants | P1 |
| Explosion medium | 3 variants | P1 |
| Explosion large | Heavy enemies, buildings; with debris tail | P1 |
| Explosion boss | Long, multi-stage, with sub-bass | P2 |
| Explosion underwater | Muffled variants for the Europa act | P2 |
| Ground target destroyed | Crumbling structure | P2 |

### Player ship

| Sound | Notes | Priority |
|---|---|---|
| Shield hit | Electric fizz | P1 |
| Shield break | Descending zap + alarm blip | P1 |
| Shield restored | Rising chime | P2 |
| Armour hit | Metallic crunch | P1 |
| Low armour warning | Beeps (slow at 30 %, fast at 15 %) | P1 |
| Ship destroyed | Big explosion, then the music sting | P1 |
| Dash | Thruster burst | P3 |
| Engine hum | Subtle loop (optional) | P3 |

### Pickups

| Sound | Priority |
|---|---|
| Salvage small / medium / large (rising pitch) | P1 |
| Shield cell | P1 |
| Armour patch | P2 |
| Special charge | P2 |
| Overdrive pickup | P1 |
| Data core (distinct, rewarding) | P2 |

### Specials

| Sound | Priority |
|---|---|
| Airstrike: radio call, jets flyby, bomb carpet | P1 |
| Smart bomb: charge-up + white-out boom | P1 |
| EMP: electric thump + power-down whine | P2 |
| Decoy flares | P2 |
| Orbital lance: charge + sustained beam | P3 |
| Sonar pulse ping — [a](concept/special-sonar-r04-a.ogg) (clean single ping); round 04 | P2 |
| Shield overcharge | P3 |
| Time dilation: slow-down / speed-up sweeps | P3 |
| Special unavailable (denied buzz) | P1 |

### Enemies

| Sound | Priority |
|---|---|
| Enemy shot: small / heavy / laser charge warning | P1 |
| Missile launch (enemy) | P1 |
| Vrell screech (spawn/attack cue), 4 variants | P2 |
| Turret rotate / lock-on beep | P2 |
| Portal / warp-in | P2 |
| Carrier launching drones | P3 |
| Boss roars and phase-change cues (per boss) | P3 |

### UI and radio

| Sound | Priority |
|---|---|
| Menu move / confirm / back | P1 |
| Buy / sell / equip / upgrade / can't afford / won't fit (power) | P1 |
| Save done | P2 |
| Typewriter blip (briefing text) | P1 |
| Radio squelch open / close | P1 |
| Warning klaxon (boss, rear attack) | P1 |
| Debrief tally tick / grade stamp | P2 |

### Ambience (per setting)

Space hum, orbital station creaks, city wind and sirens, Martian dust wind, underwater drone and
whale-like calls under Europa's ice, asteroid rumble, Jovian storm, alien pulsing beyond the
gate. P2–P3, one loop per setting in the [world](../../world/README.md).

### Mixing rules

- **Voice limit**: 32 simultaneous voices. Per-sound instance limits: player fire 2, small
  explosions 6, hits 4. Beyond the limit, the oldest instance is stolen.
- **Priority** when stealing voices: warnings and player damage > boss sounds > explosions > enemy
  fire > player fire > pickups > ambience.
- **Levels** (relative, first draft): player fire −12 dB, enemy fire −9 dB, explosions 0 dB,
  player damage and warnings +2 dB, pickups −6 dB.
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

## Implementation

- [ ] SFX playback with instance limits, stealing by priority, pitch variation
- [ ] Stereo panning by play-field X
- [ ] Underwater low-pass on the sfx bus
- [ ] All P1 sounds

## Decisions

- 2026-09-30: Priorities P1–P3 guide production order; player fire is deliberately quiet.
- 2026-09-30: Concept round 01: all three pickups chosen, one per item type — A standard power-ups, B rare upgrades, C credits.
- 2026-09-30: Concept round 01: all synthesized player shots and explosions rejected. Explosions must be somewhat realistic; source recorded sound effects from free online libraries instead.
- 2026-09-30: Concept round 02: recorded shots and explosions are much better; all ten kept as a starting set. Needed next: a distinct shot sound per weapon type (see [weapons](../../player/weapons/README.md)) and more explosions covering the full range from small pops to large booms.
- 2026-09-30: Concept round 03: rejected huge-a, medium-a, tiny-c, underwater-b and both beam loops (B barely audible); all other r03 shots and explosions chosen. The `beam` family needs new sources (round 04).
- 2026-09-30: Concept round 04: all new sounds chosen (beam loops, start/stop, huge and under-water explosions, sonar A); sonar B rejected — A is enough.
