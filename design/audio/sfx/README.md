---
title: Sound effects
design: draft
implementation: not-started
art: proposed
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
| Sonar pulse ping | P2 |
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
| [concept/player-shot-r02-a.ogg](concept/player-shot-r02-a.ogg) | "Projectile Shoot" by unfa (CC0 1.0) — Pulse Cannon (starter front gun) | proposed |
| [concept/player-shot-r02-b.ogg](concept/player-shot-r02-b.ogg) | "Sci Fi Gun Shot" by Bird_man (CC0 1.0) — heavy front gun / Hammer Mortar | proposed |
| [concept/player-shot-r02-c.ogg](concept/player-shot-r02-c.ogg) | "laser3" by nsstudios (CC-BY 4.0) — Lance Laser / light laser weapons | proposed |
| [concept/player-shot-r02-d.ogg](concept/player-shot-r02-d.ogg) | "Machine Gun 001 - single shot" by pgi (CC0 1.0) — Autocannon Pod / ballistic guns | proposed |
| [concept/player-shot-r02-e.ogg](concept/player-shot-r02-e.ogg) | "Autocannon Three Shot Burst" by qubodup (CC0 1.0) — Scatter Vulcan / heavy ballistic front gun | proposed |
| [concept/explosion-r02-a.ogg](concept/explosion-r02-a.ogg) | "small explosion" by bevibeldesign (CC0 1.0) — small enemy destroyed (fighters, drones) | proposed |
| [concept/explosion-r02-b.ogg](concept/explosion-r02-b.ogg) | "Explosion 1" by magnuswaker (CC0 1.0) — small-to-medium enemy (heavy fighters, turrets) | proposed |
| [concept/explosion-r02-c.ogg](concept/explosion-r02-c.ogg) | "Explosion" by qubodup (CC0 1.0) — medium enemy (gunships, ground vehicles, buildings) | proposed |
| [concept/explosion-r02-d.ogg](concept/explosion-r02-d.ogg) | "Nearby explosion with debris" by juskiddink (CC-BY 4.0) — large enemy / mid-boss / building collapse | proposed |
| [concept/explosion-r02-e.ogg](concept/explosion-r02-e.ogg) | "explosion_big_01" by derplayer (CC0 1.0) — boss destroyed / capital ship | proposed |

## Implementation

- [ ] SFX playback with instance limits, stealing by priority, pitch variation
- [ ] Stereo panning by play-field X
- [ ] Underwater low-pass on the sfx bus
- [ ] All P1 sounds

## Decisions

- 2026-09-30: Priorities P1–P3 guide production order; player fire is deliberately quiet.
- 2026-09-30: Concept round 01: all three pickups chosen, one per item type — A standard power-ups, B rare upgrades, C credits.
- 2026-09-30: Concept round 01: all synthesized player shots and explosions rejected. Explosions must be somewhat realistic; source recorded sound effects from free online libraries instead.
