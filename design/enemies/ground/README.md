---
title: Ground enemies
design: draft
implementation: not-started
art: chosen
updated: 2026-10-01
---

# Ground enemies

## Summary

Stationary and crawling enemies on the `ground` layer: turrets, walkers, bunkers, spawners and
shield pylons, on planetary surfaces, station hulls and asteroid rock. They scroll with the
terrain, never collide with the player, and are what `anti-ground` and `area` weapons are for.

## Roster

| Name | Faction | Layer | Tier | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|---|
| Spine Turret | Vrell | ground | small | Grown turret. `terrain` + rotating `aimed` thorns. The basic ground threat. | turret nest | 02 | idea |
| Polyp Mortar | Vrell | ground | small | `mortar`: lobs acid blobs at the player's position; the impact point is marked 1 s ahead and bursts into a small `ring`. | turret nest | 05 | idea |
| Creeper | Vrell | ground | medium | Six-legged walker. `crawl`s over rooftops and roads, firing a 5-way `fan`. | convoy | 08 | idea |
| Hive Node | Vrell | ground | medium | **Hardened** spawner. `spawn`s Skitters every 4 s until destroyed. Needs `anti-ground` or `area`. | turret nest | 09 | idea |
| Burrower | Vrell | ground | medium | `burrow`: surfaces in sand, fires a `ring`, submerges after 2 s; invulnerable while submerged. | swarm (scattered) | 15 | idea |
| Tendril Anchor | Vrell | ground | small | Pairs of anchors on canyon walls stretch a damaging `link` tendril across the path; destroy one to break the barrier (`beam` burns through). | turret nest (pairs) | 16 | idea |
| SAM Nest | Ascendancy | ground | small | Launches 2 shootable `homing` missiles at the player. | turret nest | 29 | idea |
| Sentinel Tower | Ascendancy | ground | medium | Tall tower with a 360° `laser-sweep`; the sweep arc is shown first. Hardened base. | turret nest | 32 | idea |
| Rail Bunker | Ascendancy | ground | medium | **Hardened**. Charges a screen-long `laser-line` rail shot along the player's column (0.8 s telegraph). | turret nest | 33 | idea |
| Crawler Tank | Ascendancy | ground | medium | Tracked tank that `crawl`s along hulls and roads; has a front turret **and a rear turret**, so it shoots back after you pass. | convoy | 34 | idea |
| Graft Turret | Hybrid | ground | small | An Ascendancy turret overgrown with Vrell tissue. **Regrows** 5 s after destruction unless its tissue root (weak point) is destroyed too. | turret nest | 37 | idea |
| Shield Pylon | Ascendancy | ground | medium | Hardened pylon projecting a dome that makes every enemy inside it invulnerable. Destroy pylons to open fortress sections. | turret nest | 41 | idea |
| Scuttler | Vrell | ground | medium | Crab-like six-legged walker (`walk`, 16 angles) that strides across the terrain on its own heading, turning to face where it goes, and fires a 5-way `fan` in its facing direction. Claws are `armoured` from the front; the glowing back is the weak point. | convoy, pincer (walking in from both sides) | 04 | idea |
| Threadcrawler | Vrell | ground | large | Centipede of 10–16 segments (`chain`) that crawls along canyon floors, walls and hulls following the terrain, legs rippling down its body. Every segment fires a slow `aimed` spore in turn (a travelling wave of shots); the head is `vital`. | snake (solo) | 16 | idea |
| Dust Devil | Vrell | ground → low-air | medium | *(roster fork addition)* A whirling vortex organism: a radially symmetric spinning funnel (`radial`, `spin`) that wanders across Martian plains in a `swirl`, pulling loose pickups and the player's ship slightly toward it and spitting a `spiral` of grit. The core is visible (and hittable) only at the top of each spin cycle. | swarm (2–3 roaming) | 18 | idea |
| Ravager | Vrell | ground → low-air | medium | Four-legged pack hunter, hound/raptor-like body of grown chitin and sinew with a balancing tail. Gallops (`walk`, 8-frame gallop, 16 angles) in packs of 3–5 across streets and plains and **pounces**: a short leap to `low-air` towards the player's ground position (contact damage, shadow detaches during the leap). Animal, not insect. | pack (3–5) | 09 | idea |
| Shellback | Vrell | ground | large | Lumbering four-legged armoured beast, tortoise/armadillo-like, with overlapping shell plates and a spore-mortar vent on its back (`mortar`, area shots). Slow heavy walk (16 angles). Badly damaged it **curls into a ball and rolls** along its path (`spin`), crushing ground targets, then uncurls; belly and vent are the weak points. Animal, not insect. | single / pair | 17 | idea |
| Warden Tank | Unmarked (Ascendancy) → Ascendancy | ground | medium | Tracked tank: hull at 16 angles drives its own heading along roads and conveyors (`walk`), turret at 32 angles tracks the player independently and fires `burst`s. In Act 3 unmarked grey (a hint), from Act 5 in black and gold. Rear hull plate is the weak point. | convoy | 19 | idea |
| Strider | Ascendancy | ground | large | Two-legged walker mech (8-frame walk cycle, 16 angles) that stalks across hulls and tunnel floors, arms with twin cannons (`fan`) and a shoulder `homing` pod. Legs are `destroyable`: destroying one topples it (a telegraphed fall across the ground layer). | solo, pairs | 32 | idea |
| Halo Platform | Ascendancy | ground (fortress mount) | large | A ring of 6 turret segments rotating around a shielded core (`radial` ring + 32-angle turrets). Each turret is `destroyable` and fires as it swings round (`aimed`, `laser-line`); the core's shield drops when half the ring is gone. The ring's rotation speeds up as turrets die. | turret nest (solo centrepiece) | 41 | idea |

## Design

- Ground targets use the [layer rules](../README.md): all weapons hit them, `anti-ground`
  does ×2, and **hardened** targets take only 25% from weapons without `anti-ground` or `area`.
- Destroyed ground targets leave wreckage (a crater or burnt husk) on the ground layer for the
  rest of the level, a staple of late-90s shooters.
- Ground targets pay more credits than air enemies of similar toughness, so an `anti-ground`
  loadout is rewarded on surface levels.
- Not everything on the ground stands still: walkers (Scuttler, Creeper, Strider), tanks
  (Crawler Tank, Warden Tank) and crawlers (Threadcrawler) move on their own heading with 16/32
  angle sprites and walk cycles; they still never collide with the player.

## Concept art

Concept [round 03](../../concept-rounds/round-03/README.md). Two Vrell design languages are proposed: **A "Sleek chitin"** (smooth, elongated, glossy violet chitin with thin glowing teal seams, pink eye as weak point) and **B "Armoured brood"** (bulky segmented carapace plates, claws and spikes, glow only between plates and in eye clusters). The key Act 1 enemies are shown in both; the others in A. Prompts: [concept/prompts.md](concept/prompts.md); generator `tools/concept/enemies_r03.py`.

| File | What | Status |
|---|---|---|
| [concept/spine-turret-r03-a.png](concept/spine-turret-r03-a.png) | Spine Turret, language A: smooth bulb with petal collar and one thorn barrel, rooted on a station hull; aim frames | superseded by the r04 colour pass |
| [concept/rejected/spine-turret-r03-b.png](concept/rejected/spine-turret-r03-b.png) | Spine Turret, language B: plated barnacle with a triple-spike barrel cluster | rejected — other variant preferred |
| [concept/polyp-mortar-r03-a.png](concept/polyp-mortar-r03-a.png) | Polyp Mortar (A): acid mouth ringed by tentacles; impact markers ahead of the player on lunar regolith | superseded by the r04 colour pass |
| [concept/rail-bunker-r03-a.png](concept/rail-bunker-r03-a.png) | Rail Bunker (Ascendancy ground turret): octagonal hardened bunker, twin-rail cannon with gold coils, red telegraph line | superseded by the r04 colour pass |

Concept [round 04](../../concept-rounds/round-04/README.md) — colour pass on the chosen enemies with the [role colours](../../README.md#role-colours) (chitin base = role family, glow = kind of threat; Ascendancy black & gold with a per-unit accent and a thin gold/red rim light). Same models and sheet layout; generator `tools/concept/enemies_r04.py`.

| File | What | Status |
|---|---|---|
| [concept/spine-turret-r04-a.png](concept/spine-turret-r04-a.png) | Spine Turret — mauve-grey slate chitin, violet glow (rooted, aimed shots); yellow needles | chosen |
| [concept/polyp-mortar-r04-a.png](concept/polyp-mortar-r04-a.png) | Polyp Mortar — slate chitin, lime acid (rooted, area denial) | chosen |
| [concept/rail-bunker-r04-a.png](concept/rail-bunker-r04-a.png) | Rail Bunker — black & gold with gunmetal accent and rim light | chosen |

Concept [round 05](../../concept-rounds/round-05/README.md) — new archetypes, each a PNG sheet plus a GIF in the 480×540 play field; generator `tools/concept/enemies_r05.py`.

| File | What | Status |
|---|---|---|
| [concept/scuttler-r05-a.png](concept/scuttler-r05-a.png) | Scuttler — Vrell six-legged walker (slate/lime): tripod gait, 16 headings × 6 walk phases, turns to its path, spits acid (sheet) | chosen |
| [concept/scuttler-r05-a.gif](concept/scuttler-r05-a.gif) | Scuttler — Vrell six-legged walker (slate/lime): tripod gait, 16 headings × 6 walk phases, turns to its path, spits acid (motion) | chosen |
| [concept/warden-tank-r05-a.png](concept/warden-tank-r05-a.png) | Warden Tank — Ascendancy tank (white accent): hull 16 headings following a road, turret 32 headings tracking the player, tread marks (sheet) | chosen — Ascendancy only (too mechanical for the Vrell) |
| [concept/warden-tank-r05-a.gif](concept/warden-tank-r05-a.gif) | Warden Tank — Ascendancy tank (white accent): hull 16 headings following a road, turret 32 headings tracking the player, tread marks (motion) | chosen — Ascendancy only (too mechanical for the Vrell) |
| [concept/strider-r05-a.png](concept/strider-r05-a.png) | Strider — Ascendancy 120 px biped mech (red accent): 8-frame walk cycle, torso at 32 headings twisting to fire shoulder cannons (sheet) | chosen — Ascendancy only (too mechanical for the Vrell) |
| [concept/strider-r05-a.gif](concept/strider-r05-a.gif) | Strider — Ascendancy 120 px biped mech (red accent): 8-frame walk cycle, torso at 32 headings twisting to fire shoulder cannons (motion) | chosen — Ascendancy only (too mechanical for the Vrell) |
| [concept/ravager-r05-a.png](concept/ravager-r05-a.png) | Ravager — Vrell pack hunter (rust/teal), 56 px: 16 headings, 8-frame gallop, pounce from ground to apex (sheet); generator `tools/concept/enemies_r05b.py` | chosen |
| [concept/ravager-r05-a.gif](concept/ravager-r05-a.gif) | Ravager — packs sweeping across lunar regolith, two hunters pouncing at the player's ground position (motion) | chosen |
| [concept/shellback-r05-a.png](concept/shellback-r05-a.png) | Shellback — armoured Vrell beast (slate/lime), 112 px: 16 headings, 6-frame heavy walk, curl sequence (sheet) | chosen |
| [concept/shellback-r05-a.gif](concept/shellback-r05-a.gif) | Shellback — lobs spore blobs with marked impacts, curls into a ball and rolls over boulders on a Mars plateau, uncurls (motion) | chosen |

## Implementation

- [ ] Each enemy promoted to its own directory with a stat block before it is implemented.
- [ ] Ground targets scroll with the terrain; destroyed states persist as wreckage.

## Open questions

- Should the regrow timer of the Graft Turret be shown (a pulsing root)? Proposal: yes.

## Decisions

- 2026-09-30: Roster of 12 ground enemies drafted.
- 2026-09-30: Concept round 03: Spine Turret (both Vrell languages), Polyp Mortar and the Ascendancy Rail Bunker drafted as sprites.
- 2026-09-30: Concept round 04: colour pass on ground enemies with the role colours (r04 proposals).
- 2026-09-30: Enemy variety pass: size tier column added to every row; new units Scuttler, Threadcrawler, Warden Tank, Strider, Halo Platform, and Dust Devil (roster fork addition).
- 2026-10-01: Ravager (L09) and Shellback (L17) added: animal-like Vrell ground walkers, after the user found the Warden Tank and Strider too mechanical for the Vrell. Both mechanical units stay Ascendancy; the Warden Tank keeps its unmarked Act 3 hint at L19.
- 2026-10-01: Concept round 05: Ravager and Shellback concepts chosen ("very nice").
