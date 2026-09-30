---
title: Ground enemies
design: draft
implementation: not-started
art: proposed
updated: 2026-09-30
---

# Ground enemies

## Summary

Stationary and crawling enemies on the `ground` layer: turrets, walkers, bunkers, spawners and
shield pylons, on planetary surfaces, station hulls and asteroid rock. They scroll with the
terrain, never collide with the player, and are what `anti-ground` and `area` weapons are for.

## Roster

| Name | Faction | Layer | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|
| Spine Turret | Vrell | ground | Grown turret. `terrain` + rotating `aimed` thorns. The basic ground threat. | turret nest | 02 | idea |
| Polyp Mortar | Vrell | ground | `mortar`: lobs acid blobs at the player's position; the impact point is marked 1 s ahead and bursts into a small `ring`. | turret nest | 05 | idea |
| Creeper | Vrell | ground | Six-legged walker. `crawl`s over rooftops and roads, firing a 5-way `fan`. | convoy | 08 | idea |
| Hive Node | Vrell | ground | **Hardened** spawner. `spawn`s Skitters every 4 s until destroyed. Needs `anti-ground` or `area`. | turret nest | 09 | idea |
| Burrower | Vrell | ground | `burrow`: surfaces in sand, fires a `ring`, submerges after 2 s; invulnerable while submerged. | swarm (scattered) | 15 | idea |
| Tendril Anchor | Vrell | ground | Pairs of anchors on canyon walls stretch a damaging `link` tendril across the path; destroy one to break the barrier (`beam` burns through). | turret nest (pairs) | 16 | idea |
| SAM Nest | Ascendancy | ground | Launches 2 shootable `homing` missiles at the player. | turret nest | 29 | idea |
| Sentinel Tower | Ascendancy | ground | Tall tower with a 360° `laser-sweep`; the sweep arc is shown first. Hardened base. | turret nest | 32 | idea |
| Rail Bunker | Ascendancy | ground | **Hardened**. Charges a screen-long `laser-line` rail shot along the player's column (0.8 s telegraph). | turret nest | 33 | idea |
| Crawler Tank | Ascendancy | ground | Tracked tank that `crawl`s along hulls and roads; has a front turret **and a rear turret**, so it shoots back after you pass. | convoy | 34 | idea |
| Graft Turret | Hybrid | ground | An Ascendancy turret overgrown with Vrell tissue. **Regrows** 5 s after destruction unless its tissue root (weak point) is destroyed too. | turret nest | 37 | idea |
| Shield Pylon | Ascendancy | ground | Hardened pylon projecting a dome that makes every enemy inside it invulnerable. Destroy pylons to open fortress sections. | turret nest | 41 | idea |

## Design

- Ground targets use the [layer rules](../README.md): all weapons hit them, `anti-ground`
  does ×2, and **hardened** targets take only 25% from weapons without `anti-ground` or `area`.
- Destroyed ground targets leave wreckage (a crater or burnt husk) on the ground layer for the
  rest of the level, a staple of late-90s shooters.
- Ground targets pay more credits than air enemies of similar toughness, so an `anti-ground`
  loadout is rewarded on surface levels.

## Concept art

Concept [round 03](../../concept-rounds/round-03/README.md). Two Vrell design languages are proposed: **A "Sleek chitin"** (smooth, elongated, glossy violet chitin with thin glowing teal seams, pink eye as weak point) and **B "Armoured brood"** (bulky segmented carapace plates, claws and spikes, glow only between plates and in eye clusters). The key Act 1 enemies are shown in both; the others in A. Prompts: [concept/prompts.md](concept/prompts.md); generator `tools/concept/enemies_r03.py`.

| File | What | Status |
|---|---|---|
| [concept/spine-turret-r03-a.png](concept/spine-turret-r03-a.png) | Spine Turret, language A: smooth bulb with petal collar and one thorn barrel, rooted on a station hull; aim frames | proposed |
| [concept/spine-turret-r03-b.png](concept/spine-turret-r03-b.png) | Spine Turret, language B: plated barnacle with a triple-spike barrel cluster | proposed |
| [concept/polyp-mortar-r03-a.png](concept/polyp-mortar-r03-a.png) | Polyp Mortar (A): acid mouth ringed by tentacles; impact markers ahead of the player on lunar regolith | proposed |
| [concept/rail-bunker-r03-a.png](concept/rail-bunker-r03-a.png) | Rail Bunker (Ascendancy ground turret): octagonal hardened bunker, twin-rail cannon with gold coils, red telegraph line | proposed |

## Implementation

- [ ] Each enemy promoted to its own directory with a stat block before it is implemented.
- [ ] Ground targets scroll with the terrain; destroyed states persist as wreckage.

## Open questions

- Should the regrow timer of the Graft Turret be shown (a pulsing root)? Proposal: yes.

## Decisions

- 2026-09-30: Roster of 12 ground enemies drafted.
- 2026-09-30: Concept round 03: Spine Turret (both Vrell languages), Polyp Mortar and the Ascendancy Rail Bunker drafted as sprites.
