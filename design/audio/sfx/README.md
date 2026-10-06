---
title: Sound effects
design: approved
implementation: done
art: chosen
depends-on: [../../player, ../../enemies, ../../ui]
updated: 2026-10-06
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
| Hornet / micro-missile launch | Whoosh with a small ignition pop; the Hornet Launcher (L10) plays its `missile` family since M5 part A | P1 |
| Hammer Mortar / bomb drop | Hollow thunk, whistle down | P2 |
| Lance Laser | Sustained zap with tail | P2 |
| Ion Beam loop | Humming loop with start/stop | P2 |
| Harpoon torpedo / depth charge | Muffled launch, bubbles; underwater boom; the Torpedo Pod (L11) — **later: M5 part E**, Harpoon Torpedoes and Depth Charges Act 4 | P2 |
| Plasma Arc | Crackle | P3 |
| Tail Gun / Fan Blaster | Like the front guns, a little lower (rear guns play their family 10 % lower, built with the Side Splitter); Tail Gun (L08) and Fan Blaster (L10), built in M5 part A | P1 |
| Proximity mine drop / arm | Click + beep; Proximity Mines (L12): the drop plays the `mine` family at its own pitch (M5 part A); the arming beep as a mine arms: proposed in [round 28](#concept-art) (a "armed chirp", b "sensor ping"), a played until the choice | P2 |
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
| Launch rail | The catapult run at a level start (Level 01 section 1): pressure hiss and shuttle rumble to a buffer clunk at 3.6 s — chosen [b](concept/launch-rail-r11-b.ogg) (catapult; a mag-lev was rejected), [round 11](../../concept-rounds/round-11/README.md) | P2 |

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
| Vrell screech, 2 variants: [c](concept/enemy-screech-r08-c.ogg), [d](concept/enemy-screech-r08-d.ogg), in turn — a large Vrell unit entering the screen (its hit box first over the play field): the Mantis, the Coilwyrm's head (not its body or a regrown head), the Spore Bomber, the Scuttler; once per unit, at most one screech every 3 s (a unit entering inside them stays silent), at the Vrell spawns' level (user decision 2026-10-06) | P2 |
| Turret rotate / lock-on beep — [a](concept/enemy-lock-r08-a.ogg) | P2 |
| Portal / warp-in; Vrell spawn (Brood Pod bursting, Hive Node and Brood Carrier spawns) — [a](concept/enemy-spawn-r08-a.ogg) (wet creature swell), [b](concept/enemy-spawn-r08-b.ogg) (fleshy burst) | P2 |
| Carrier launching drones — the Brood Carrier's units leaving its sacs: chosen [b](concept/enemy-carrier-launch-r25-b.ogg) (slime lunge; a creature spit was rejected), [round 25](../../concept-rounds/round-25/README.md) | P2 |
| Boss roars and phase-change cues (per boss) — the Brood Carrier (Level 07): roar as it arrives and, lower, as it turns broadside, chosen [b](concept/enemy-carrier-roar-r25-b.ogg) (bear and didgeridoo; a deep roar with echo was rejected); a bay sac opening, chosen [b](concept/enemy-carrier-sac-open-r25-b.ogg), and closing, chosen [b](concept/enemy-carrier-sac-close-r25-b.ogg) (a tear out of sucking mud and its reverse; flesh pulled apart was rejected); a sac bursting, chosen [a](concept/enemy-carrier-sac-burst-r25-a.ogg) (very wet, fleshy explosion, CC-BY; a visceral tear was rejected); the plate iris opening, chosen [b](concept/enemy-carrier-iris-r25-b.ogg) (organic morph; an alien hatch was rejected), [round 25](../../concept-rounds/round-25/README.md); other bosses P3 | P2 |
| Lifeboat tow (Level 07 secret): the amber cable snapping on its third hit — chosen [a](concept/secret-cable-snap-r25-a.ogg) (chain snap, CC-BY; a slowed string twang was rejected), [round 25](../../concept-rounds/round-25/README.md); the hits play the metal hit, the crate the large salvage | P2 |
| Leviathan whale-song cry (its death, under the large burst) — [a](concept/enemy-leviathan-cry-r16-a.ogg) (synthesized, round 16) | P2 |
| Polyp Mortar lob — chosen [a](concept/enemy-mortar-lob-r21-a.ogg) (mortar thump; an organic spit was rejected); impact — chosen [a](concept/enemy-mortar-impact-r21-a.ogg) (wet splat; an acid sizzle was rejected), [round 21](../../concept-rounds/round-21/README.md) | P2 |
| Mass-driver sled (Level 05 hazard): whine while the rail lights chase — chosen [a](concept/hazard-sled-whine-r21-a.ogg) (charge hum loop, played twice over the 1.5 s chase; a rising charge was rejected); pass — chosen [b](concept/hazard-sled-pass-r21-b.ogg) (rushing flyby; a rail-gun crack was rejected), [round 21](../../concept-rounds/round-21/README.md) | P2 |
| Mantis (Level 06): telegraph, the 0.6 s arc — chosen [a](concept/enemy-mantis-telegraph-r23-a.ogg) (laser charge-up; a second charge was rejected); beam sweep, 1.3 s — chosen [b](concept/enemy-mantis-sweep-r23-b.ogg) (death ray with crackle, CC-BY; a game-style beam was rejected), [round 23](../../concept-rounds/round-23/README.md) | P2 |
| Coilwyrm chain-cut tear (Level 06): the body torn in two as a cut segment's rear part starts to regrow, over the segment's burst — chosen [a](concept/enemy-coilwyrm-cut-r27-a.ogg) (a run of wet flesh rips, 15 % slower; one juicy limb-tearing rip with a low body was rejected), [round 27](../../concept-rounds/round-27/README.md) | P2 |
| Coilwyrm head regrowth (Level 06), 0.6 s — chosen [b](concept/enemy-coilwyrm-regrow-r23-b.ogg) (insect growl and chitter; a wet slime was rejected), [round 23](../../concept-rounds/round-23/README.md) | P2 |
| Coilwyrm death (Level 06): the head's deeper burst — chosen [c](concept/enemy-coilwyrm-head-burst-r24-c.ogg) (messy splatter, slowed over a sub thump, CC-BY), then a segment's or the tail's wet burst, 13 times down the chain 0.25 s apart — chosen [b](concept/enemy-coilwyrm-burst-r24-b.ogg) (fleshy burst; a wet gib crack, the messy splatter and a synthesized pop were rejected); [preview](concept/enemy-coilwyrm-death-final-r24-a.ogg), [round 24](../../concept-rounds/round-24/README.md) | P2 |
| Perimeter beacon flares (Level 06 hazard): launch — chosen [a](concept/hazard-flare-launch-r23-a.ogg) (flare-gun shot, CC-BY; a firework ignition was rejected); burn loop while it falls — chosen [a](concept/hazard-flare-burn-r23-a.ogg) (road flare, the 3 s loop played back to back while a flare burns; a second road flare was rejected), [round 23](../../concept-rounds/round-23/README.md) | P2 |

### UI and radio

| Sound | Priority |
|---|---|
| Menu move / confirm / back — [a](concept/ui-menu-move-r08-a.ogg) / [a](concept/ui-menu-confirm-r08-a.ogg) / [a](concept/ui-menu-back-r08-a.ogg) | P1 |
| Buy / sell / equip / upgrade / can't afford / won't fit (power) — buy [a](concept/ui-shop-buy-r08-a.ogg), sell [a](concept/ui-shop-sell-r08-a.ogg), can't afford / won't fit [a](concept/ui-shop-denied-r08-a.ogg), equip [a](concept/ui-equip-r08-a.ogg), upgrade [a](concept/ui-upgrade-r08-a.ogg) | P1 |
| Save done: the hangar's autosave as it opens and a save to a slot (a failed save plays the back blip) — chosen [b](concept/ui-save-r27-b.ogg) (a latch click, two warm bells over a pad; data ticks landing on a bright fifth and a bell were rejected), [round 27](../../concept-rounds/round-27/README.md) | P2 |
| Typewriter blip (briefing text) — [a](concept/ui-typewriter-r08-a.ogg) | P1 |
| Radio squelch open / close — [a](concept/ui-radio-open-r08-a.ogg) / [a](concept/ui-radio-close-r08-a.ogg) | P1 |
| Warning klaxon (boss, rear attack) — [a](concept/ui-klaxon-r08-a.ogg) (seamless loop; a single blast can be cut from it) | P1 |
| Edge warning tone (side or rear wave, with the edge arrows) — chosen [b](concept/ui-edge-warning-r11-b.ogg) (contact ping; a triple chirp was rejected), [round 11](../../concept-rounds/round-11/README.md) | P1 |
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

### Chosen sounds the game does not play

Every other chosen sound in the Concept art tables below is played by the game (an entry of
`vanguard.game.audio.Sfx`; `SfxFilesTest` checks both lists against this README). Besides them the
game may play only `proposed` sounds of an open concept round, provisionally until the user's choice
(none at present).

| File | Why not |
|---|---|
| [player-shot-r02-b.ogg](concept/player-shot-r02-b.ogg) | `pulse` family b ("heavy"): the family plays a, levels differ by pitch; kept for layering a heavy level |
| [player-shot-r02-c.ogg](concept/player-shot-r02-c.ogg) | `laser` family a: the Lance Laser plays b (r03) |
| [player-shot-r02-e.ogg](concept/player-shot-r02-e.ogg) | `vulcan` family a: the Scatter Vulcan plays b (r03) |
| [shot-torpedo-r03-a.ogg](concept/shot-torpedo-r03-a.ogg) | Torpedo Pod, an Act 2 weapon (L11) — later: M5 part E |
| [shot-tesla-r03-a.ogg](concept/shot-tesla-r03-a.ogg) | Tesla Coil Pod, Plasma Arc, EMP Burst: later acts |
| [shot-resonator-r03-a.ogg](concept/shot-resonator-r03-a.ogg) | Choir Resonator: a later act |
| [shot-beam-r04-a.ogg](concept/shot-beam-r04-a.ogg) | Ion Beam loop: a later act |
| [shot-beam-r04-b.ogg](concept/shot-beam-r04-b.ogg) | Alternative beam loop: a later act |
| [shot-beam-r04-c.ogg](concept/shot-beam-r04-c.ogg) | Orbital Lance loop (L17): a later act |
| [shot-beam-start-r04-a.ogg](concept/shot-beam-start-r04-a.ogg) | Beam start: a later act |
| [shot-beam-stop-r04-a.ogg](concept/shot-beam-stop-r04-a.ogg) | Beam stop: a later act |
| [special-sonar-r04-a.ogg](concept/special-sonar-r04-a.ogg) | Sonar Pulse (L22): Act 4 |
| [special-flares-r08-a.ogg](concept/special-flares-r08-a.ogg) | Decoy Flares (L27): Act 4 |
| [explosion-underwater-r03-a.ogg](concept/explosion-underwater-r03-a.ogg) | Under-water kills: Act 4 |
| [explosion-underwater-r04-a.ogg](concept/explosion-underwater-r04-a.ogg) | Under-water kills: Act 4 |
| [explosion-water-r03-a.ogg](concept/explosion-water-r03-a.ogg) | Surface naval kills: Act 2's ocean — later: M5 |
| [ambience-city-r08-a.ogg](concept/ambience-city-r08-a.ogg) | Megacity setting: Act 2 — later: M5 |
| [ambience-ocean-r08-a.ogg](concept/ambience-ocean-r08-a.ogg) | Ocean setting: Act 2 — later: M5 |
| [ambience-storm-r08-a.ogg](concept/ambience-storm-r08-a.ogg) | Ocean storm setting: Act 2 — later: M5 |
| [ambience-arctic-r08-a.ogg](concept/ambience-arctic-r08-a.ogg) | Arctic setting: Act 2 — later: M5 |
| [enemy-missile-r08-a.ogg](concept/enemy-missile-r08-a.ogg) | No Act 1 enemy fires missiles (the SAM Nest, L29, and the Hornet, L31, are the first) |
| [enemy-lock-r08-a.ogg](concept/enemy-lock-r08-a.ogg) | No Act 1 turret locks on (the SAM Nest, L29, is the first) |
| [enemy-laser-warning-r08-a.ogg](concept/enemy-laser-warning-r08-a.ogg) | Act 1's only laser, the Mantis, has its own telegraph (round 23) |
| [enemy-spawn-r08-a.ogg](concept/enemy-spawn-r08-a.ogg) | Vrell spawn a: the Brood Carrier's launches have their own sound (round 25); the Hive Node is not in Act 1 |
| [enemy-coilwyrm-death-final-r24-a.ogg](concept/enemy-coilwyrm-death-final-r24-a.ogg) | A review preview of the Coilwyrm's chained death; the game plays its two bursts |

### Mixing rules

- **Voice limit**: 32 simultaneous voices. Per-sound instance limits: player fire 2, small
  explosions 6, hits 4. Beyond the limit, the oldest instance is stolen.
- **Priority** when stealing voices: warnings and player damage > boss sounds > explosions > enemy
  fire > player fire > pickups > ambience. A sound past the 32 voices steals the oldest voice of
  the lowest priority at or below its own, and is dropped when every voice ranks above it. The
  groups not named above: the interface (menus, radio blips, the hangar, the debrief, the music's
  cues) ranks with the warnings, the specials' sounds with the boss sounds, enemy hazards (mortar,
  sled, flares, the Mantis's beam) and the Vrell screech with enemy fire, hits with the player fire that lands them. A
  running loop (an ambience) is never stolen, since it would not come back, and always gets a
  voice.
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

Concept [round 11](../../concept-rounds/round-11/README.md) — synthesized by
`tools/concept/audio/sfx_r11.py`, levelled on the 200 Hz–5 kHz band like the recorded sounds
(launch rail −27 dB with a −4 dBFS ceiling, the UI/klaxon level, so it sits under the briefing's
last radio line; warning tones −24 dB / −2 dBFS, the player-damage level).

| File | What | Status |
|---|---|---|
| [concept/rejected/launch-rail-r11-a.ogg](concept/rejected/launch-rail-r11-a.ogg) | Synthesized — Launch rail "mag-lev": linear-motor hum and whine rising with the speed, coil ticks passing faster, release clunk and latch at 3.6 s, engine whoosh into open space (4.9 s) | rejected — b chosen (round 11) |
| [concept/launch-rail-r11-b.ogg](concept/launch-rail-r11-b.ogg) | Synthesized — Launch rail "catapult": pressure hiss building, shuttle rumble over the rail joints with a rattle, heavy two-stage buffer clunk at 3.6 s, steam vent dying away (4.9 s) | chosen |
| [concept/rejected/ui-edge-warning-r11-a.ogg](concept/rejected/ui-edge-warning-r11-a.ogg) | Synthesized — Edge warning tone "triple chirp": three soft square blips (A5, A5, E6) on the warning's flash cycle (0.267 s) | rejected — b chosen (round 11) |
| [concept/ui-edge-warning-r11-b.ogg](concept/ui-edge-warning-r11-b.ogg) | Synthesized — Edge warning tone "contact ping": an upward chirp into a ringing 1.6 kHz ping, quieter repeat one flash cycle later | chosen |

Concept round 16 (M4 part C, the Level 03 batch) — synthesized by
`tools/concept/audio/sfx_r16.py`, levelled on the 200 Hz–5 kHz band like the recorded enemy
sounds (−30 dB, the Vrell screech and spawn level; ceiling −6 dBFS), so it sits under the large
burst it plays with. Brief: [concept/prompts.md](concept/prompts.md#enemy-leviathan-cry-r16-a-synthesized).

| File | What | Status |
|---|---|---|
| [concept/enemy-leviathan-cry-r16-a.ogg](concept/enemy-leviathan-cry-r16-a.ogg) | Synthesized — the Leviathan's whale-song cry: a deep, mournful alien call (a throat-like pulse voice through moving "oo–aah–oo" formants, its fundamental rising 92 → 128 Hz and falling to 52 Hz with a yodel break, a second voice a tritone above beating against it, a breathy blowhole rasp at the start, a long dark reverb), 3.0 s with the tail; −22.5 LUFS, peak −11.3 dBFS | chosen |

Concept round 21 (M4 part E, Level 05; user decision D8) — recorded, imported by
`tools/concept/audio/import_sfx.py`, an a/b pair for each of the sled's whine and pass and the
Polyp Mortar's lob and impact. Briefs and sources: [concept/prompts.md](concept/prompts.md#round-21--level-05s-sled-and-polyp-mortar-m4-part-e-user-decision-d8), [CREDITS.md](../../../CREDITS.md).

| File | What | Status |
|---|---|---|
| [concept/hazard-sled-whine-r21-a.ogg](concept/hazard-sled-whine-r21-a.ogg) | "Railgun_ChargeLoop" by BaggoNotes (CC0 1.0) — sled whine a, a loop played while the rail lights chase (1.5 s) | chosen |
| [concept/rejected/hazard-sled-whine-r21-b.ogg](concept/rejected/hazard-sled-whine-r21-b.ogg) | "charging power" by JavierZumer (CC-BY 4.0) — sled whine b, played once over the 1.5 s chase | rejected |
| [concept/rejected/hazard-sled-pass-r21-a.ogg](concept/rejected/hazard-sled-pass-r21-a.ogg) | "shipboard_railgun.mp3" by deleted_user_1941307 (CC0 1.0) — sled pass a, the 0.4 s run | rejected |
| [concept/hazard-sled-pass-r21-b.ogg](concept/hazard-sled-pass-r21-b.ogg) | "Planetary Flyby faster.aif" by mattpavone (CC0 1.0) — sled pass b, the 0.4 s run | chosen |
| [concept/enemy-mortar-lob-r21-a.ogg](concept/enemy-mortar-lob-r21-a.ogg) | "Mortar" by Mozfoo (CC0 1.0) — Polyp Mortar lob a | chosen |
| [concept/rejected/enemy-mortar-lob-r21-b.ogg](concept/rejected/enemy-mortar-lob-r21-b.ogg) | "NPX Throat Gathering Spit 3.wav" by noahpardo (CC0 1.0) — Polyp Mortar lob b | rejected |
| [concept/enemy-mortar-impact-r21-a.ogg](concept/enemy-mortar-impact-r21-a.ogg) | "Wet Splat" by JustInvoke (CC-BY 4.0) — Polyp Mortar impact a, the blob landing | chosen |
| [concept/rejected/enemy-mortar-impact-r21-b.ogg](concept/rejected/enemy-mortar-impact-r21-b.ogg) | "Acid Bubbling.wav" by spookymodem (CC0 1.0) — Polyp Mortar impact b, the blob landing | rejected |

Concept round 23 (M4 part F, Level 06; user decision D8) — recorded, imported by
`tools/concept/audio/import_sfx.py`, an a/b pair for each of the Mantis's telegraph and beam sweep,
the perimeter beacon's flare launch and the falling flare's burn loop, and the Coilwyrm's head
regrowth. Briefs and sources: [concept/prompts.md](concept/prompts.md#round-23--level-06s-mantis-flares-and-coilwyrm-m4-part-f-user-decision-d8), [CREDITS.md](../../../CREDITS.md).

| File | What | Status |
|---|---|---|
| [concept/enemy-mantis-telegraph-r23-a.ogg](concept/enemy-mantis-telegraph-r23-a.ogg) | "Laser Charge Up" by magnuswaker (CC0 1.0) — Mantis telegraph a, the 0.6 s crimson arc before a sweep | chosen |
| [concept/rejected/enemy-mantis-telegraph-r23-b.ogg](concept/rejected/enemy-mantis-telegraph-r23-b.ogg) | "laser-charge.wav" by StavSounds (CC0 1.0) — Mantis telegraph b, the 0.6 s crimson arc before a sweep | rejected |
| [concept/rejected/enemy-mantis-sweep-r23-a.ogg](concept/rejected/enemy-mantis-sweep-r23-a.ogg) | "Game - Style Laser Beam" by Jofae (CC0 1.0) — Mantis sweep a, the 1.2 s beam | rejected |
| [concept/enemy-mantis-sweep-r23-b.ogg](concept/enemy-mantis-sweep-r23-b.ogg) | "SonicDeathRay_1.2KHzWithCrackle.wav" by zimbot (CC-BY 4.0) — Mantis sweep b, the 1.2 s beam | chosen |
| [concept/hazard-flare-launch-r23-a.ogg](concept/hazard-flare-launch-r23-a.ogg) | "FlareGun_Shot01.wav" by marb7e (CC-BY 4.0) — flare launch a, the perimeter beacon firing a flare shell (t=70, 112, 150) | chosen |
| [concept/rejected/hazard-flare-launch-r23-b.ogg](concept/rejected/hazard-flare-launch-r23-b.ogg) | "firework_rocket_ignition.ogg" by derplayer (CC0 1.0) — flare launch b, the perimeter beacon firing a flare shell (t=70, 112, 150) | rejected |
| [concept/hazard-flare-burn-r23-a.ogg](concept/hazard-flare-burn-r23-a.ogg) | "ROAD FLARE.wav" by frankelmedico (CC0 1.0) — flare burn a, a quiet loop while a flare falls (8 s, 12 s on easy) | chosen |
| [concept/rejected/hazard-flare-burn-r23-b.ogg](concept/rejected/hazard-flare-burn-r23-b.ogg) | "Road Flare1" by theshaggyfreak (CC-BY 4.0) — flare burn b, a quiet loop while a flare falls (8 s, 12 s on easy) | rejected |
| [concept/rejected/enemy-coilwyrm-regrow-r23-a.ogg](concept/rejected/enemy-coilwyrm-regrow-r23-a.ogg) | "Slime 3.wav" by Archos (CC0 1.0) — Coilwyrm regrowth a, the 0.6 s a cut chain's new head grows | rejected |
| [concept/enemy-coilwyrm-regrow-r23-b.ogg](concept/enemy-coilwyrm-regrow-r23-b.ogg) | "Insect Animal Growl Chitter" by SecureSubset (CC0 1.0) — Coilwyrm regrowth b, the 0.6 s a cut chain's new head grows | chosen |

Concept round 24 (the Coilwyrm's death, user request of 2026-10-05) — four options, each a
segment burst, the head's deeper burst and a preview of the whole chained death at the round's
0.05 s rhythm (`tools/concept/audio/sfx_r24.py`; a–c recorded, cut by `import_sfx.py`'s functions,
d synthesized). Closed 2026-10-05: segment burst **b**, head burst **c**, the ripple slowed to
0.25 s; the production files are rebuilt from the Freesound originals by
`tools/art/sfx_originals.py` (with sfx_r24.py's treatment). Briefs and sources: [concept/prompts.md](concept/prompts.md#round-24--the-coilwyrms-death-bursts), [CREDITS.md](../../../CREDITS.md).

| File | What | Status |
|---|---|---|
| [concept/rejected/enemy-coilwyrm-burst-r24-a.ogg](concept/rejected/enemy-coilwyrm-burst-r24-a.ogg) | "Gib sound" by RoozyDB (CC0 1.0) — Coilwyrm segment burst a, a sharp wet crack with gooey bits (0.30 s) | rejected |
| [concept/rejected/enemy-coilwyrm-head-burst-r24-a.ogg](concept/rejected/enemy-coilwyrm-head-burst-r24-a.ogg) | Coilwyrm head burst a: the same source, a longer cut 18 % slower over a sub thump (0.57 s) | rejected |
| [concept/rejected/enemy-coilwyrm-death-r24-a.ogg](concept/rejected/enemy-coilwyrm-death-r24-a.ogg) | Preview a (review only): the head's burst, then 13 segment bursts 0.05 s apart | rejected |
| [concept/enemy-coilwyrm-burst-r24-b.ogg](concept/enemy-coilwyrm-burst-r24-b.ogg) | "Burst Flesh" by magnuswaker (CC0 1.0) — Coilwyrm segment burst b, a dense fleshy burst with a low body (0.26 s): the segments' and the tail's burst | chosen |
| [concept/rejected/enemy-coilwyrm-head-burst-r24-b.ogg](concept/rejected/enemy-coilwyrm-head-burst-r24-b.ogg) | Coilwyrm head burst b: the same source, a longer cut 18 % slower over a sub thump (0.75 s) | rejected |
| [concept/rejected/enemy-coilwyrm-death-r24-b.ogg](concept/rejected/enemy-coilwyrm-death-r24-b.ogg) | Preview b (review only): the whole chained death at 0.05 s | rejected |
| [concept/rejected/enemy-coilwyrm-burst-r24-c.ogg](concept/rejected/enemy-coilwyrm-burst-r24-c.ogg) | "Messy Splat 3" by FoolBoyMedia (CC-BY 4.0) — Coilwyrm segment burst c, a bright, messy splatter (0.22 s) | rejected |
| [concept/enemy-coilwyrm-head-burst-r24-c.ogg](concept/enemy-coilwyrm-head-burst-r24-c.ogg) | "Messy Splat 3" by FoolBoyMedia (CC-BY 4.0) — Coilwyrm head burst c: a longer cut 18 % slower over a sub thump (0.75 s): the heads' burst (original and regrown) | chosen |
| [concept/rejected/enemy-coilwyrm-death-r24-c.ogg](concept/rejected/enemy-coilwyrm-death-r24-c.ogg) | Preview c (review only): the whole chained death at 0.05 s | rejected |
| [concept/rejected/enemy-coilwyrm-burst-r24-d.ogg](concept/rejected/enemy-coilwyrm-burst-r24-d.ogg) | Synthesized — Coilwyrm segment burst d, a pressurised pop: membrane snap, chitin crackle, a falling gas bloop, wet bubble chirps and a splash hiss (0.28 s) | rejected |
| [concept/rejected/enemy-coilwyrm-head-burst-r24-d.ogg](concept/rejected/enemy-coilwyrm-head-burst-r24-d.ogg) | Coilwyrm head burst d: a bigger synthesized pop (lower bloop, more drops), 18 % slower over a sub thump (0.73 s) | rejected |
| [concept/rejected/enemy-coilwyrm-death-r24-d.ogg](concept/rejected/enemy-coilwyrm-death-r24-d.ogg) | Preview d (review only): the whole chained death at 0.05 s | rejected |
| [concept/enemy-coilwyrm-death-final-r24-a.ogg](concept/enemy-coilwyrm-death-final-r24-a.ogg) | Preview of the chosen pair at the game's rhythm (review only, from the Freesound originals): the head burst c, then 13 bursts b 0.25 s apart, each at the explosion level, pitched up as the members narrow down the taper | chosen |

Concept round 25 (M4 part G, Level 07) — recorded (CC0 / CC-BY), an a/b pair for each of the
Brood Carrier's roar, a bay sac opening, closing and bursting, a unit launched from a sac, the
plate iris opening, and the lifeboat tow's cable snap (`tools/concept/audio/sfx_r25.py`, cut from
the cached Freesound **originals**, levelled on the loudest 100 ms of the 200 Hz–5 kHz band).
Closed 2026-10-05: roar **b**, sac opening **b**, closing **b**, launch **b**, sac burst **a**, iris
**b**, cable snap **a**; the production files in `assets/sfx/` are written by
`tools/art/sfx_originals.py` with sfx_r25.py's treatment (`PRODUCTION`). The klaxon keeps its chosen
`ui-klaxon-r08-a` (already rebuilt from its original), so it is not in the round. Briefs and
sources: [concept/prompts.md](concept/prompts.md#round-25--level-07s-brood-carrier-and-lifeboat-tow-m4-part-g), [CREDITS.md](../../../CREDITS.md).

| File | What | Status |
|---|---|---|
| [concept/rejected/enemy-carrier-roar-r25-a.ogg](concept/rejected/enemy-carrier-roar-r25-a.ogg) | "Deep Roar Echo 2.wav" by noahpardo (CC0 1.0) — carrier roar a: a 0.6 s swell, 2.5 s of deep roar, its echo (4.0 s) | rejected |
| [concept/enemy-carrier-roar-r25-b.ogg](concept/enemy-carrier-roar-r25-b.ogg) | "Didgeridoo Monster Roar" by Noxdl (CC0 1.0) — carrier roar b: a bear's roar over a didgeridoo drone, a buzzing, whale-like body (3.0 s) | chosen |
| [concept/rejected/enemy-carrier-sac-open-r25-a.ogg](concept/rejected/enemy-carrier-sac-open-r25-a.ogg) | "GOREFlsh_Flesh Manipulation 01_KVV AUDIO_FREE" by KVV_Audio (CC-BY 4.0) — sac opening a: flesh pulled apart close up (0.85 s) | rejected |
| [concept/enemy-carrier-sac-open-r25-b.ogg](concept/enemy-carrier-sac-open-r25-b.ogg) | "Squelch.mp3" by LucasDuff (CC0 1.0) — sac opening b: something torn out of a sucking mud (0.9 s) | chosen |
| [concept/rejected/enemy-carrier-sac-close-r25-a.ogg](concept/rejected/enemy-carrier-sac-close-r25-a.ogg) | "GOREFlsh_Flesh Manipulation 01_KVV AUDIO_FREE" by KVV_Audio (CC-BY 4.0) — sac closing a: the same recording's third gesture, quieter (0.6 s) | rejected |
| [concept/enemy-carrier-sac-close-r25-b.ogg](concept/enemy-carrier-sac-close-r25-b.ogg) | "Squelch.mp3" by LucasDuff (CC0 1.0) — sac closing b: opening b's tear reversed, a wet suck that ends shut (0.6 s) | chosen |
| [concept/rejected/enemy-carrier-launch-r25-a.ogg](concept/rejected/enemy-carrier-launch-r25-a.ogg) | "Spit 1 - The Ridge - Spanker" by bananplyte (CC0 1.0) — launch a: a creature's throaty spit (0.5 s) | rejected |
| [concept/enemy-carrier-launch-r25-b.ogg](concept/enemy-carrier-launch-r25-b.ogg) | "Slime Attack 1" by qubodup (CC0 1.0) — launch b: a slime monster's wet lunge, brighter (0.4 s) | chosen |
| [concept/enemy-carrier-sac-burst-r25-a.ogg](concept/enemy-carrier-sac-burst-r25-a.ogg) | "Headshot 2" by SilverIllusionist (CC-BY 4.0) — sac burst a: a very wet, fleshy explosion, 10 % slower (1.56 s) | chosen |
| [concept/rejected/enemy-carrier-sac-burst-r25-b.ogg](concept/rejected/enemy-carrier-sac-burst-r25-b.ogg) | "Gutsy Spillage 1" by magnuswaker (CC0 1.0) — sac burst b: a visceral tear with a wet tail, 10 % slower (1.22 s) | rejected |
| [concept/rejected/enemy-carrier-iris-r25-a.ogg](concept/rejected/enemy-carrier-iris-r25-a.ogg) | "SFX Door Open.wav" by Paul368 (CC0 1.0) — iris a: an alien hatch morphing open, a pressure release up front (1.6 s) | rejected |
| [concept/enemy-carrier-iris-r25-b.ogg](concept/enemy-carrier-iris-r25-b.ogg) | "Simple Mutate (Monster)" by Division4884 (CC0 1.0) — iris b: a grinding, organic morph (1.3 s) | chosen |
| [concept/secret-cable-snap-r25-a.ogg](concept/secret-cable-snap-r25-a.ogg) | "snapping-chain" by CosmicEmbers (CC-BY 3.0) — cable snap a: a sharp crack, the loose chain rattling after it (1.2 s) | chosen |
| [concept/rejected/secret-cable-snap-r25-b.ogg](concept/rejected/secret-cable-snap-r25-b.ogg) | "Guitar string snaps.wav" by juskiddink (CC-BY 4.0) — cable snap b: a string snapping, 25 % slower, a heavy cable's twang and ring (1.6 s) | rejected |

Concept round 27 (user decision of 2026-10-06 on round 26's open questions) — an a/b pair for the
save-done sound, synthesized in the round-08 UI family (peak −8 dBFS like the other UI blips), and
for the Coilwyrm's chain-cut tear, recorded (CC0 / CC-BY, cut from the cached Freesound
**originals**, levelled on the loudest 100 ms of the 200 Hz–5 kHz band to −20 dB, between the
segment burst's −17.3 and the regrowth's −27.4), all by `tools/concept/audio/sfx_r27.py`. Not
listened to by Claude. Closed 2026-10-06: save done **b**, chain-cut tear **a**; the save sound is
copied into `assets/sfx/` by `copyPlaceholderSounds`, the tear's production file is written by
`tools/art/sfx_originals.py` with sfx_r27.py's treatment (`PRODUCTION`). Briefs and sources:
[concept/prompts.md](concept/prompts.md#round-27--the-save-done-sound-and-the-coilwyrms-chain-cut-tear), [CREDITS.md](../../../CREDITS.md).

| File | What | Status |
|---|---|---|
| [concept/rejected/ui-save-r27-a.ogg](concept/rejected/ui-save-r27-a.ogg) | Synthesized — save done a, "write and seal": four quick rising pulse ticks (E6 G♯6 B6 E7), then a bright triangle fifth (E6 + B6) over a bell an octave up, a short echo (0.70 s) | rejected |
| [concept/ui-save-r27-b.ogg](concept/ui-save-r27-b.ogg) | Synthesized — save done b, "calm chime": a soft latch click, two warm bells a fourth apart (A5, D6 80 ms later) over a swelling triangle pad (0.92 s) | chosen |
| [concept/enemy-coilwyrm-cut-r27-a.ogg](concept/enemy-coilwyrm-cut-r27-a.ogg) | "rip_tear FLESH!!!!.wav" by aust_paul (CC0 1.0) — chain-cut tear a: a monster tearing at flesh, a run of short wet rips, 15 % slower for a bigger body (1.06 s) | chosen |
| [concept/rejected/enemy-coilwyrm-cut-r27-b.ogg](concept/rejected/enemy-coilwyrm-cut-r27-b.ogg) | "Tearing Flesh" by dereklieu (CC-BY 3.0) — chain-cut tear b: one juicy limb-tearing rip (layered bread breaks) with a low body (0.72 s) | rejected |

Concept round 28 (M5 part A) — an a/b pair for the proximity mine's arming beep, synthesized in the
round-08 UI family by `tools/concept/audio/sfx_r28.py`: short, dry, high and quiet (peak −12 dBFS,
the tick level), so mines arming together do not clutter. Not listened to by Claude. Option a plays
in the game until the round closes (`Sfx.MINE_ARM`, a concept copy by `copyPlaceholderSounds`).
Briefs: [concept/prompts.md](concept/prompts.md#round-28--the-proximity-mines-arming-beep).

| File | What | Status |
|---|---|---|
| [concept/weapon-mine-arm-r28-a.ogg](concept/weapon-mine-arm-r28-a.ogg) | Synthesized — mine arming a, "armed chirp": two soft rising pulse blips a fifth apart (A6, E7 45 ms later), low-passed (0.10 s) | proposed |
| [concept/weapon-mine-arm-r28-b.ogg](concept/weapon-mine-arm-r28-b.ogg) | Synthesized — mine arming b, "sensor ping": one sine ping gliding up a fifth in 15 ms, ringing out on a faint metallic overtone over a tiny latch click (0.15 s) | proposed |

## Implementation

- [x] SFX playback with instance limits, stealing by priority, pitch variation: the 32-voice
  limit with priority stealing (`VoiceLimit`, `Sfx.Priority`), the per-sound instance limits, ±pitch
- [x] Stereo panning by play-field X
- [ ] Underwater low-pass on the sfx bus — **later: Act 4** (the Europa levels)
- [x] The Act 1 sounds: every chosen sound an Act 1 event uses is played (the list of the ones
  that are not, with the reason, is above); the explosion ladder by size, the Smart Bomb, the
  shield's restore chime, the low-armour beeps, the pickups by type, the ground targets' crumble
- [x] Heavy enemy shots: `Sfx.ENEMY_HEAVY_SHOT_A`/`_B` play when an `ENEMY_FIRED` event's value
  (the gun's damage, sent by the simulation) is at least the medium bullet class's damage
  (`FlightSounds.heavyShot`, `BulletLooks.MEDIUM_DAMAGE`): the Leviathan's and Scuttler's `medium` shots
- [x] The hangar's shop sounds (`Sfx.SHOP_BUY`, `SHOP_SELL`, `SHOP_DENIED`, `EQUIP`, `UPGRADE`,
  chosen by `Sfx.shop`) played by the hangar screen instead of the menu blips: a shop action its
  own, a paid repair the purchase, an undo and a confirmed sale the refund, a refusal the denial
  (`HangarScreen.doneSound`; `HangarSoundsTest`)
- [x] The Vrell screech (user decision 2026-10-06): `Sfx.ENEMY_SCREECH_C`/`_D` in turn as a Mantis,
  a Coilwyrm's head, a Spore Bomber or a Scuttler first comes onto the screen, once per unit (by its
  serial), at most one every 3 s, at the explosion gain like the Vrell spawns, enemy-fire priority
  (`ScreechCue`, followed in `FlightSounds.watch`; `ScreechCueTest`, with Level 06 flown)
- [x] Save done (P2) and the Coilwyrm's chain-cut tear, round 27's choices: `Sfx.SAVE_DONE`
  (save b, a concept copy by `copyPlaceholderSounds`) when the hangar's autosave or a save to a slot
  is written (`HangarScreen`, `SlotsScreen`), `Sfx.COILWYRM_CUT` (tear a, rebuilt from its original
  by `tools/art/sfx_originals.py` with sfx_r27.py's `PRODUCTION`) on `CHAIN_CUT` (before: a lower
  Brood Pod burst)
- [x] The Act 2 weapon sounds built in M5 part A: the Tail Gun and Fan Blaster play the `pulse`
  family 10 % lower as rear guns, the Hornet Launcher `Sfx.MISSILE_SHOT` (shot-missile-r03-a), the
  Proximity Mines' drop `Sfx.MINE_DROP` (shot-mine-r03-a, not lowered: a drop, not a gun), the
  Swivel Gun the `ballistic` family; a mine's blast plays the small explosion like a bomb's
  (`FlightSounds`)
- [x] The proximity mine's arming beep: `Sfx.MINE_ARM` on the simulation's `PROXIMITY_MINE_ARMED`
  (the step a mine arms), at −10 dB in the flight mix, ±3 % pitch, panned, two instances at most
  (`FlightSounds`); round 28's option a until the round closes (M5 part A)
- [ ] Torpedo launch and the water explosion — **later: M5 part E**; the Act 2 ambiences —
  **later: M5** (parts B, E, F, G, with their levels)
- [x] Recorded sounds rebuilt from the Freesound originals: every chosen recorded concept sound (75
  from rounds 02–08, round 21's four, round 23's five, round 24's two, round 25's seven and round 27's one) in `assets/sfx/` under its concept name, with a `SOURCE` comment, by
  [`tools/art/sfx_originals.py`](../../../tools/art/README.md); `importPlaceholders` keeps them
  (only the synthesized sounds are still copied). `art: final` waits for the user's round-12 review.

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
- 2026-10-02: The launch rail sound goes into the next concept round (user decision); listed as a
  pending item in the Player ship table, no file yet.
- 2026-10-02: Concept round 11 proposed: two synthesized launch-rail sounds and two edge-warning
  tones (`tools/concept/audio/sfx_r11.py`). The edge warning tone is a new P1 row: the user
  found the side and rear warnings easy to miss after playing Level 01.
- 2026-10-02: Concept round 11 (user choices): launch rail **b** "catapult" and edge-warning tone **b** "contact ping" chosen; a "mag-lev" and a "triple chirp" moved to `concept/rejected/`. The launch-rail item is no longer pending.
- 2026-10-02: M3 part B1: the launch rail (b) plays at the start of every attempt's 5 s launch in
  Level 01, so its buffer clunk lands at 3.6 s; the edge-warning tone (b) plays when an edge warning
  starts, at the player-damage level, panned towards the warned edge, with an instance limit of 1
  so overlapping warnings never stack. Both are synthesized (no CREDITS.md rows) and imported by
  `:pipeline:importPlaceholders`. The launch rail plays at full level (the file is levelled like the
  interface sounds); no level is given for it beyond that.
- 2026-10-02: Production audio (Level 01 batch, part P2): the 75 chosen recorded sounds are rebuilt
  from the Freesound originals (cached by `tools/concept/audio/freesound_fetch.py`) with their
  unchanged `import_sfx.py` settings by `tools/art/sfx_originals.py` and written to `assets/sfx/`
  with a `SOURCE` comment; the concept files stay as the record of the choice. One step is added
  before the treatment: 13 originals decode above full scale (lossy originals and float WAVs, up
  to +18.7 dBFS for "Machine Gun 001") and are clipped at full scale first, as 16-bit playback
  clips them; levelled on the unclipped peak, the Autocannon Pod shot came out 11 dB quieter
  than the chosen sound and "explosion_big_02" 1.7 dB. Checked against the chosen files: same
  lengths, and peak and 200 Hz–5 kHz band RMS within 1 dB, except the peak of
  `enemy-shot-small-r08-b`, 1.7 dB lower at the same band RMS (it is levelled on its band, so only
  the crest of its attack differs from the preview's; the audible level is the same).
- 2026-10-02: Concept round 12 closed (user decision): the 75 recorded sounds rebuilt from the Freesound originals approved as **final** (the 1.7 dB lower peak of `enemy-shot-small-r08-b` accepted); this doc's `art` stays `chosen`, since the synthesized sounds were not part of the round.
- 2026-10-02: Concept round 16: the Leviathan's whale-song cry proposed as a synthesized sound
  (`tools/concept/audio/sfx_r16.py`, written to `concept/` like the other synthesized sounds,
  which `importPlaceholders` copies). The 2026-10-01 decision has enemy sounds recorded; this one
  is synthesized for the M4 part C batch, so a deep alien call with its own glide and formants
  exists without a source search. The user decides in round 16 whether it stays or a recorded
  CC0/CC-BY call replaces it.
- 2026-10-03: Concept round 16 closed (user decision): the synthesized Leviathan whale-song cry (`enemy-leviathan-cry-r16-a`) approved as **final**. The user accepts this synthesized cry, so its placeholder note against the 2026-10-01 rule (enemy sounds are recorded CC0/CC-BY) is resolved: no recorded replacement is sought. This doc's `art` stays `chosen`.
- 2026-10-04: Round 21 opened with the sounds of Level 05's props (user decision D8 of M4 part E): recorded a/b pairs for the sled's whine and pass and the Polyp Mortar's lob and impact.
- 2026-10-04: Round 21 closed for the sounds (user): mortar lob a, mortar impact a, sled whine a
  (its 0.78 s loop played twice over the 1.5 s chase) and sled pass b chosen and played by the game
  (`Sfx.MORTAR_LOB`, `MORTAR_IMPACT`, `SLED_WHINE`, `SLED_PASS`, in the `:pipeline` copy list),
  replacing the placeholders; the four others moved to `concept/rejected/`, their CREDITS.md rows
  kept with the new paths, as for the earlier rounds' rejected recordings.
- 2026-10-04: Round 23 opened with Level 06's new sounds (user decision D8 of M4 part F): recorded
  a/b pairs for the Mantis's telegraph and beam sweep, the flare launch and burn loop and the
  Coilwyrm's head regrowth. The data core keeps its round-08 mapping to the chosen `pickup-r01-b`
  and the Smart Bomb its chosen `special-smartbomb-r08-a`, so neither is in the round.
- 2026-10-05: Round 23 decided (user): Mantis telegraph **a** and sweep **b** (CC-BY), flare
  launch **a** (CC-BY) and burn **a**, Coilwyrm regrowth **b** chosen; the other five moved to
  `concept/rejected/` (their CREDITS.md rows follow). Wired into the game as round 21's sounds
  were: copied into `assets/sfx/` under their concept names by `:pipeline:importPlaceholders`
  (`copyPlaceholderSounds`), played by `FlightSounds` through new `Sfx` entries: the telegraph at
  the warnings' level, the sweep at the hits' level (a little above enemy fire), the launch with
  the burn's 3 s loop played back to back while the flare burns (8 s, 12 s on easy; the last play
  at half level as the pool fades), the regrowth at the explosions' level. The two CC-BY picks
  reach the credits screen through their CREDITS.md rows (the screen's list is generated from
  them, skipping rejected files). Rebuilding them (and round 21's) from the Freesound originals
  with `tools/art/sfx_originals.py` needs `freesound_fetch.py --download` first: the originals
  are not cached.
- 2026-10-05: Production audio for rounds 21 and 23, now that the originals are cached: the nine
  chosen recorded sounds (sled whine a, sled pass b, mortar lob a and impact a; Mantis telegraph a
  and sweep b, flare launch a and burn a, Coilwyrm regrowth b) rebuilt from the Freesound
  originals with their unchanged `import_sfx.py` settings by `tools/art/sfx_originals.py` into
  `assets/sfx/` under their concept names, with a `SOURCE` comment and CREDITS.md rows for the
  `assets/` files; `copyPlaceholderSounds` keeps them, as round 12's. The script builds every
  sound whose Concept art row is `chosen` and stops at the first one whose original is not cached,
  which was the stop above; the rows stay `chosen` (the concept files record the choice, the
  `SOURCE` comment marks the final file). Only the sled pass's original decodes above full scale
  (82 samples, +0.01 dBFS) and is clipped first. Checked against the chosen files: same lengths
  (the whine's 0.78 s and the burn's 3.00 s loops still seamless), peaks within 0.3 dB, 200 Hz–5
  kHz band RMS within 0.5 dB and 50 ms envelopes within 0.7 dB, except the regrowth: held at its
  −6 dBFS ceiling by the original's 0.6 dB higher crest, it is 0.8 dB lower on its band and 0.9
  dB on average over its envelope (within the 1 dB tolerance). This doc's `art` stays `chosen` (the synthesized sounds are not final).
- 2026-10-05: Round 24 closed (user): the Coilwyrm's head burst **c** (CC-BY) and segment burst
  **b**; a and d and the round's previews moved to `concept/rejected/` (their CREDITS.md rows
  follow). Rebuilt from the Freesound originals by `tools/art/sfx_originals.py`, which now also
  takes sounds with a treatment of their own (`DERIVED`: `sfx_r24.py`'s `PRODUCTION`, the cut,
  the loudest-100-ms levelling and the head's slowed thump), into `assets/sfx/` under their
  concept names; within 0.4 dB of the concept files on peak and band. The chained death plays one
  burst every 0.25 s at the explosion level (they no longer overlap), each pitched by the member's
  width down the taper; `enemy-coilwyrm-death-final-r24-a` previews it.
- 2026-10-05: Round 25 opened (M4 part G, Level 07): recorded a/b pairs for the Brood Carrier's
  roar (its arrival and its turn), a bay sac opening, closing and bursting, a unit launched from a
  sac, the plate iris opening and the lifeboat tow's cable snap (`tools/concept/audio/sfx_r25.py`,
  the first round cut from the Freesound originals, so the chosen files need no rebuild). Option a
  plays in Level 07 for now, through new `Sfx` entries and `FlightSounds`: the roar 0.75 s after
  the klaxon starts and again at 0.85 pitch as the turn begins; one sac-opening sound for the sacs
  that open together, a quieter closing as they shut; up to three launch spits 0.1 s apart after
  an opening (an opening's units all leave in one step); a sac shot off bursts with the generic
  part explosion under it, and the death chain bursts every sac with it in its tail-to-head order;
  the iris as the core is exposed; the cable snap as the pod falls free (the cable's hits keep the
  metal hit). The carrier plays no generic phase blast. All at the explosions' level. The klaxon
  keeps the chosen, already final `ui-klaxon-r08-a`. Not listened to: picked by description,
  rating, envelope, band balance and level.
- 2026-10-05: Round 25 closed (user): carrier roar **b**, sac opening **b**, sac closing **b**,
  launch **b**, sac burst **a** (CC-BY 4.0, SilverIllusionist), iris **b** and cable snap **a**
  (CC-BY 3.0, CosmicEmbers); the other variants moved to `concept/rejected/` (their CREDITS.md
  rows follow). The chosen files were already cut from the Freesound originals; the production
  files are written by `tools/art/sfx_originals.py`, which takes round 25's chosen treatment as
  `DERIVED` (`sfx_r25.py`'s `PRODUCTION`: the cut, the slowdown or reversal, the loudest-100-ms
  levelling), into `assets/sfx/` under their concept names with a `SOURCE` comment; `Sfx` and
  `copyPlaceholderSounds` play the chosen ones, the provisional option-a files that are no longer
  used were removed from `assets/sfx/`. The two CC-BY sounds join the credits screen's list.
- 2026-10-05: M4 part H, the SFX pass. **Voice limit**: at most 32 effects at once (`VoiceLimit`
  in `SfxBank`); a play past an effect's own instance limit stops its oldest instance, a play past
  32 steals the oldest voice of the lowest priority at or below its own or is dropped. Libgdx does
  not report a sound's end, so a voice counts for its file's length (read from the Ogg file's last
  page, divided by the pitch). Priorities as in the mixing rules; the groups they do not name were
  placed by the main-agent brief and this pass: the interface with the warnings, the specials with
  the boss sounds, hazards with enemy fire, hits with player fire; loops are never stolen.
  **Wired** (chosen sounds already in `assets/sfx/`): enemies explode on their size rung (`medium`
  for the Brood Pod, Mantis, Scuttler and Spore Bomber; two files a rung, alternated); a set
  piece's part blows with the `medium` rung, a boss's phase end with the `large` one; a set piece's
  death with the `huge` rung (an act boss, and a set piece that is no boss, the Leviathan) or the
  `large` one (a mid-boss, the Gorgon Frigate); an act boss's tail-to-head chain starts on the
  `large` rung, bursts on the `medium` one and ends on the `huge` blast with the `large` rung's boss
  blast (r02-e) 0.05 s later; the Leviathan's break-up layers the `large` and `medium` rungs instead
  of slowed small explosions. A destroyed ground target bursts with the small ladder's r02-b or a and
  crumbles under it: collapse a for a target of at least 1 000 px², rubble b below. The Smart Bomb
  plays its chosen energy blast with the `huge` rung's sub-heavy boom under it at −3 dB (the blast
  swells over 0.5 s; the boom gives the instant flash its punch). The shield's restore chime plays
  once the shield is full again after a break (not after every hit's regeneration). The low-armour
  beep repeats every 1.2 s at or below 30 % armour and every 0.6 s at or below 15 %, while the ship
  flies, 6 dB under the warnings' level, since armour does not regenerate and the beeping can last
  the rest of the level. Pickups by type: salvage medium r01-c, the special charge's own chime, the
  data core r01-b, the overdrive pickup r01-a with the overdrive's start cue. Heavy enemy shots and
  the hangar's shop sounds have their `Sfx` entries but are not played yet (see Implementation).
  The nine synthesized sounds are copied by `copyPlaceholderSounds`. Not played, with the reason:
  the table *Chosen sounds the game does not play*. Gaps for concept round 26: a save-done sound, the
  Coilwyrm's chain-cut tear, the Vrell screech's cue. Not listened to: levels set by rule.
- 2026-10-05: Concept round 26 closed (user: the round accepted as proposed): the SFX pass accepted
  as built: the 32-voice limit with priority stealing and the newly wired chosen sounds (the shop,
  the pickups by type, the explosions by size, the ground targets' crumble, the Smart Bomb, the
  shield's restore chime, the low-armour beeps, the heavy enemy shot), at the levels set by rule.
  The row's two questions had no proposal and were not answered: the Vrell screech's cue and
  whether to make concepts for a save-done sound and the Coilwyrm's chain-cut tear stay open
  questions, tagged `later: M5`. Every item is ticked or tagged later, so the document is `done`;
  `art` stays `chosen`, since the synthesized sounds are still concept copies and the save-done and
  chain-cut sounds have none.
- 2026-10-06: Round 26's open questions decided (user): the **Vrell screech** (c and d, in turn)
  plays as a large Vrell unit enters the screen, the Mantis, the Coilwyrm's head, the Spore Bomber
  and the Scuttler, throttled to one every **3 s** so a wave does not stack them; no simulation event
  marks an enemy's entry, so the game watches each step for a unit of those kinds whose hit box
  first overlaps the play field (by its serial, so it screeches once). It ranks with enemy fire.
  Concepts for the **save-done sound** and the **Coilwyrm's chain-cut tear** go into [concept
  round 27](../../concept-rounds/round-27/README.md) (two synthesized, two recorded options; a of
  each wired until the choice: the save at the hangar's autosave and a slot save, the tear at the
  cut).
- 2026-10-06: Concept round 27 closed (user): save done **b**, the calm chime (latch click, two warm
  bells over a pad; a, the rising data ticks on a bright fifth, rejected); the Coilwyrm's chain-cut
  tear **a**, "rip_tear FLESH!!!!.wav" by aust_paul (CC0; b, "Tearing Flesh" by dereklieu, CC-BY,
  rejected, so it leaves the credits roll); the Vrell screech approved as built. The save sound
  ships as a concept copy like the other synthesized UI blips, the tear rebuilt from its Freesound
  original by `tools/art/sfx_originals.py`. The game now plays no proposed sound.
- 2026-10-06: M5 plan (user decision D1 of part A): the Act 2 weapon sounds are retagged by part:
  the Hornet, the rear guns and the mines' drop and arm with part A, the torpedo with part E
  (Level 11).
- 2026-10-06: M5 part A built the Act 2 weapon sounds from the chosen families: the Hornet's
  `missile` (shot-missile-r03-a) and the mines' drop (shot-mine-r03-a) are played now, so they
  leave the not-played list; rear guns keep the 10 % lower pitch, the mine's drop does not (it is
  not a gun). The arming beep the `mine` family names has no chosen sound: a gap for round 28.
- 2026-10-06: M5 part A, concept round 28: the proximity mine's arming beep gets two synthesized
  proposals (`tools/concept/audio/sfx_r28.py`, round-08 UI family, peak −12 dBFS): a, a rising
  two-blip "armed chirp", and b, a single gliding "sensor ping". Main-agent brief: short (≤ 0.15 s),
  dry and quiet, since a mount drops up to two mines a second and keeps up to six. The simulation
  now marks the step a mine arms (`PROXIMITY_MINE_ARMED`, at the mine; it allocates nothing and the
  replay hashes are unchanged, events are not hashed); option a plays there provisionally
  (`Sfx.MINE_ARM`, two instances, −10 dB, between the hits' −8 and the shots' −14), so the game plays
  one proposed sound until the round closes.
