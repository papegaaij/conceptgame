---
title: Wingmen and drones
design: review
implementation: not-started
art: chosen
depends-on: [../weapons, ../../story]
updated: 2026-10-01
---

# Wingmen and drones

## Summary

Extra guns that fly themselves. The AI wingman Lt. Kenji "Rook" Tanaka flies his own ship in
the **escort slot**. Drones are cheaper helpers: light drones use a wing mount, and a heavy
drone can take the escort slot instead of Rook.

## Design

### Escort slot (recommended answer to "wing slot or separate slot?")

A separate **escort slot**, unlocked at L08 when Rook is assigned as Lancer's wingman (a story
beat in [act 2](../../campaign/act-2-homefront/README.md); Rook has been in Aegis Wing for years
and flies in the wider formation before that). It holds either Rook or one heavy drone. It draws no power
from the Stormhawk's generator: the escort brings its own. Its costs are hiring/upgrade prices
and repairs.

Why a separate slot: if Rook took a wing mount, players would compare a whole pilot to a
missile pod, and he would rarely be worth it. A separate slot keeps both choices interesting and
makes his availability a story lever: Rook is shot down at the end of L26 and is missing for
L27–L29 (see [Rook](../../story/characters/rook/README.md)). During those levels the escort slot
can only hold a heavy drone; Rook is freed at Ceres Hub in L29 and is back from L30. His
upgrades are kept.

### Rook (AI wingman)

| Property | Value (first draft) |
|---|---|
| Hire | Free when he is assigned (L08); upgrades are paid |
| Ship | An AF-12 Stormhawk built on the variant-C airframe (the blended manta from the [ship concepts](../ship/README.md)) in Rook's own colours; 42×42 sprite, slightly smaller than the player's 48×48 so the player's ship always reads first; armour 80, no shield |
| Position | Formation slot beside and slightly behind the player (left or right, hangar setting) |
| Behaviour | Keeps formation, shoots what the player shoots, dodges bullets with a delay, targets enemies closing from the sides |
| Weapon | One gun from his own list (see *Rook's guns* below), upgradable L1–L5 at 60 % of the player's prices |
| Downed | At 0 armour he ejects and returns next level; his repair costs 10 cr/point after the level |
| Radio | Banters on the radio; warns about threats from the rear ("Six o'clock, Lancer!") |

### Rook's AI (first-draft parameters)

Coordinates are relative to the player's centre, +y pointing down the screen; the side (left or
right) is the hangar setting and mirrors the x values.

**Ship**

| Property | Value |
|---|---|
| Armour | 80, no shield; takes enemy bullets and contact damage like the player |
| Hitbox | 11×11 px |
| Movement | Top speed 250 px/s, full speed in 0.2 s; never closer than 40 px to the player; keeps the 12 px play-field margin |
| Collision with an `air` enemy | Rook takes the enemy's full contact damage and deals 20 to it |

**Formations** — he switches automatically and glides to the new slot over 0.6 s.

| Formation | Offset | When |
|---|---|---|
| Wing (default) | (64, +28) | No other condition |
| Wide | (120, +10) | A `sides` wave is active, or an enemy is within 160 px of him horizontally |
| Trail | (40, +90) | A `rear` wave is active: pursuers overtake him first and enter his firing cone sooner |

**Reactions**

| Property | Value |
|---|---|
| Reaction delay | 0.25 s for target switches, formation changes and dodges |
| Dodging | Every 0.1 s he predicts enemy bullets 0.4 s ahead; if one would pass within 14 px, he sidesteps up to 48 px at right angles to it, then returns to his slot. Because of the delay he avoids about 70 % of single bullets and fewer in dense patterns |
| Firing cone | 30° ahead of his ship, range 360 px; his craft never turns (banking frames only), so he fires up the screen |
| Fire | Fires while the player is firing and a target is in his cone; never fires at nothing |
| Target priority | (1) an enemy the player damaged in the last 1.0 s; (2) an enemy within 160 px of him horizontally (flank threat); (3) the nearest enemy — all within his cone. Layers follow his gun's traits (Missiles can hit `high-air`; Mortar only `ground`) |

**Rook's guns** — each uses a player weapon's per-level table, scaled; prices are 60 % of the
player weapon's base price, upgrades follow the [weapons](../weapons/README.md#common-rules)
formula on that price.

| Gun | Based on | Scale | DPS L1 → L5 | Price | Available |
|---|---|---|---|---|---|
| Autocannon | [Autocannon Pod](../weapons/autocannon-pod/README.md) | × 1.5 | 12 → 37 | free (fitted when he joins) | L08 |
| Scatter | [Scatter Vulcan](../weapons/scatter-vulcan/README.md) | × 0.6 (volley) | 11 → 39 | 720 | L08 |
| Missiles | [Micro-missile Pod](../weapons/micro-missile-pod/README.md) | × 1.5 | 12 → 42 | 480 | L08 |
| Mortar | [Hammer Mortar](../weapons/hammer-mortar/README.md) | × 0.6 | 15 → 54 | 900 | L08 |

**Shot down**

- At 0 armour he ejects: the pod pops out and drifts off-screen while the ship explodes (`medium`
  explosion). He is out for the rest of the level. This never fails the mission and costs no score.
- He returns next level at full armour; repairs cost 10 cr per point of armour lost (free on easy)
  and are charged in the hangar after the level.
- On a retry he starts again with his level-start state, like the player.

**Radio barks** (text + radio blip, at least 8 s apart; when several fire at once the highest
priority wins; 3–4 line variants each, to be written in [Rook](../../story/characters/rook/README.md)):

| Priority | Trigger | Example |
|---|---|---|
| 1 | Boss warning | "Here comes the big one." |
| 2 | A `rear` wave enters within 1.5 s | "Six o'clock, Lancer!" |
| 3 | A `sides` wave enters | "Contacts on your flank!" |
| 4 | Player armour below 30 % | "You're smoking, Lancer — ease off!" |
| 5 | Rook's armour below 30 % | "Taking a beating over here!" |
| 6 | Rook ejects | "Punching out — give 'em hell!" |
| 7 | 10 kills within 5 s | "Nice shooting!" |
| 8 | An overdrive pickup appears | "Power-up, two o'clock!" |

### Heavy drone "Warden" (escort alternative, L22)

| Property | Value |
|---|---|
| Ship | Armour 120, no shield, top speed 200 px/s, hitbox 14×14 px |
| Formation | Wing slot (56, +20), no automatic formation changes |
| Weapon | Slow cannon straight ahead: 1 shell/s, 14 damage (14 DPS), not upgradable |
| Draws fire | While within 150 px of the player, 30 % of enemy aimed shots that would target the player target the Warden instead |
| Destroyed | Out for the rest of the level; rebuilt for the next level, repairs 8 cr per point |
| Radio | None (a drone) |

### Drones

| Name | Slot | Summary | Draw | Price | Unlock | Design |
|---|---|---|---|---|---|---|
| Light drone | wing mount | Orbits the ship at 36 px, fires a small forward gun, blocks bullets (breaks after 8 hits, rebuilds in 6 s). Sold as the Light Drone Bay in [weapons](../weapons/README.md) | 2 → 3 | 2 000 | L15 | idea |
| Rear-guard drone | wing mount | Trails behind, fires backwards (`rear`) | 2 → 3 | 2 400 | L20 | idea |
| Heavy drone "Warden" | escort | Tough (armour 120), slow-firing cannon, draws enemy fire | 0 | 6 000 | L22 | idea |
| Hunter drone | escort | Leaves formation to chase homing targets, fragile | 0 | 7 500 | L29 | idea |

Unlock `Lnn` = in the shop from the hangar visit before level *nn*. The Warden arrives before
Rook's absence (L27–L29) so the escort slot never has to stay empty.

## Concept art

Round 02 — see [round 02](../../concept-rounds/round-02/README.md). AI-generator prompts: [concept/prompts.md](concept/prompts.md). Generated by
`tools/concept/ships_r02.py`. Rook flies the variant-C airframe from
[ship](../ship/README.md) concept round 01, at 40×40.

| File | What | Status |
|---|---|---|
| [concept/rook-craft-r02-a.png](concept/rook-craft-r02-a.png) | Rook scheme A "Ember": dark slate hull, orange/yellow accents, warm engines — darker and warmer than the player | chosen |
| [concept/rejected/rook-craft-r02-b.png](concept/rejected/rook-craft-r02-b.png) | Rook scheme B "Jade": pale lime hull, deep green panels, green engines — same brightness as the player, different hue | rejected — A preferred |

Concept [round 09](../../concept-rounds/round-09/README.md) — generator `tools/concept/vfx_r09.py`.

| File | What | Status |
|---|---|---|
| [concept/rook-craft-r09-a.png](concept/rook-craft-r09-a.png) | Rook (Ember) 5 banking frames at 4×, 1× strip beside the Stormhawk with greyscale check, in-game view | chosen |

## Implementation

- [ ] Escort slot unlocked by a story flag
- [ ] Rook AI as in *Rook's AI*: formations Wing / Wide / Trail, reaction delay, dodging, targeting priority, firing cone, eject and return
- [ ] Rook's four guns derived from the player weapon tables
- [ ] Warden heavy drone: formation, cannon, draw-fire rule
- [ ] Drone behaviours: orbit, trail, block, rebuild
- [ ] Wingman radio lines triggered by events (rear threat, low armour, kill streaks)

## Open questions

- Should Rook ever be killed for good in the story? The current story keeps him alive (missing
  L27–L29 only); see [Rook](../../story/characters/rook/README.md).
- While Rook is missing, the draft allows a heavy drone in the escort slot. The alternative is
  to lock the slot completely for those levels, which makes his absence hit harder.

## Decisions

- 2026-09-30: Draft uses a separate escort slot (pending user confirmation).
- 2026-09-30: Rook's timeline aligned with story and campaign: escort slot at L08, missing
  L27–L29 (heavy drone only), back from L30. Drone unlock levels added.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Rook flies the variant-C (blended manta) airframe from ship concept round 01, in his own colours; it replaces the placeholder "F-9 Kestrel".
- 2026-09-30: Concept round 02: Rook's craft scheme **A "Ember"** (dark slate, orange/yellow accents) chosen; B "Jade" rejected.
- 2026-10-01: The separate escort slot for Rook is confirmed by the user. Rook's craft uses 5 banking frames like the player.
- 2026-10-01: Rook's AI specified (formations, reactions, targeting, guns derived from player weapons, eject/return, radio bark triggers); Warden heavy drone specified.
- 2026-10-01: Concept round 09: Rook's 5 banking frames chosen.
