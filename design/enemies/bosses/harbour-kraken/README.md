---
title: Harbour Kraken
design: draft
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Harbour Kraken

## Summary

The Act 2 mid-boss: a rust cephalopod wrapped around an offshore UTC platform. Its arms slam telegraphed lanes while the head lurks below; when it surfaces crown-first, its lime eyes open as weak points and it fires fans.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `sub` (submerged parts) and `ground` (naval surface) when surfaced |
| Size tier | `huge` (mid-boss) |
| Size | mantle and head ≈ 190 px; eight arms of 13 segments; platform 220×150 px |
| Parts | head/mantle (`vital`; eyes are weak points), 2 slam arms (`destroyable`), 4 gripping arms (`armoured`, scenery), 2 idle arms (submerged scenery) |
| Orientation | arm segments 32 angles; head fixed |
| HP | head 2 900; slam arm 500 each; total 3 900 (easy ×0.75 / hard ×1.3) |
| Armour / shield | submerged parts: only `anti-sub` (layer rule); surfaced head: mantle ×0.5, eyes ×2 |
| Speed | stationary (the scroll stops at the platform) |
| Movement | see phases; surfacing and diving follow the water rules (crown first, top-down) |
| Attack | see phases |
| Formations | solo (Driftjelly fields around the arena) |
| Weak points | lime eyes (×2), only while surfaced |
| Effective traits | `anti-sub` (submerged head), `anti-ground` (surfaced parts ×2), `forward` |
| Credits | slam arms 50 each, head 200 → 300 (≈ 15 % of the L11 budget, 1 967) |
| Death | `huge` water-surface variant: the head sinks in a churning foam ring, arms slide off the platform, spray, credit shower |
| First level / used in | L11 (mid-boss) |
| Difficulty hooks | hard: lane telegraph 0.8 s; phase 3 slams three lanes |

### Phases

| Phase | Ends at | Behaviour |
|---|---|---|
| 1 — Slams | until 3 slams or 20 s | Head submerged. A lane churns and flashes for 1.0 s (red dashed telegraph), then a slam arm rises base-to-tip and slams it (damage `heavy` = 10 on contact with the arm; a line of 6 splash bullets, 120 px/s, fans out). The arm lies awash 1.5 s (hittable) and sinks. |
| 2 — Head up | head 2 900 → 40 % | Cycle: the head surfaces crown-first (2 s, swell and foam), eyes open for 8 s; the beak glows crimson 0.5 s before each 7-orb `fan` (spread 70°, 140 px/s, `medium` = 6) every 2 s; then it dives and one slam follows. |
| 3 — Two lanes | head 40 % → 0 | Head stays up; two lanes are telegraphed and slammed at once every 4 s between fans. Severed slam arms remove their lanes. |

### Arena

Atlantic convoy (L11): the scroll halts at the platform; convoy ships must stay out of the slammed lanes (secondary objective). Target duration 45–75 s at medium.

### Behaviour

- Every arm visibly continues under water to the mantle (water rules); the Kraken never pops in.
- A player with the optional Torpedo Pod (L11) can damage the submerged head in phase 1 — a reward for buying `anti-sub` early.

### Concept art

Chosen concept: [harbour-kraken-r07-a.png](../concept/harbour-kraken-r07-a.png), [harbour-kraken-r07-a.gif](../concept/harbour-kraken-r07-a.gif) (listed in the [bosses](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Water-surface interaction per the art-direction water rules
- [ ] Lane telegraph → slam → awash → sink cycle
- [ ] Surfacing/diving head with eye windows and fans
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the bosses roster to a full spec for the Acts 1–2 wrap-up.
