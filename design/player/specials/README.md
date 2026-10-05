---
title: Special abilities
design: approved
implementation: done
art: chosen
depends-on: [../../campaign, ../../world]
updated: 2026-10-05
---

# Special abilities

## Summary

The special slot holds one ability, fired with its own button. **Charge-based** specials use up
charges that are bought in the hangar and occasionally found in levels. **Cooldown-based**
specials are expensive to buy but recharge on their own. Some specials depend on the setting:
no airstrikes under the Europa ice. This gives the hangar intel another thing to warn about.

## Design

| Special | Type | Effect | Cost | Unlock | Setting limits | Design |
|---|---|---|---|---|---|---|
| Airstrike | charges, max 4 (1 free at unlock) | Two CDF bombers sweep up the screen: heavy damage (300) to `ground` and `low-air`, 60 to `air` | 300 / charge | L04 (CDF bomber support assigned after L03) | Not under ice/water; not beyond the gate | draft |
| Smart Bomb | max 3 charges (1 free at unlock) | Flash: clears all enemy bullets, 120 damage to everything on screen | 400 / charge | L06 | none | draft |
| Decoy Flares | max 6 charges | Homing missiles and seekers retarget to flares for 4 s | 150 / charge | L27 (with the first homing projectiles) | none | draft |
| EMP Burst | max 3 charges | Stuns machines 3 s and strips enemy shields; Vrell (biomechanical) stunned 1.5 s | 350 / charge | L15 | none | idea |
| Sonar Pulse | max 4 charges | Reveals the `sub` layer and makes it hittable by all weapons for 6 s | 250 / charge | L22 | Water levels only | idea |
| Orbital Lance | max 2 charges | 3 s vertical beam that follows the ship's X, 400 DPS on all layers except `sub` | 600 / charge | L17 | Needs satellite cover: not under ice, not beyond the gate | idea |
| Shield Overcharge | cooldown 45 s | Fills the shield and doubles its capacity for 8 s | 5 000 once | L19 | none | idea |
| Time Dilation | cooldown 60 s | Everything except the player runs at 50 % speed for 5 s | 8 000 once | L31 | none | idea |
| Vrell Swarm Call | cooldown 75 s | Captured Choir tech: summons 6 friendly Vrell drones for 10 s | 10 000 once | L45 | none | idea |

Rules:
- Only one special type can be equipped. Unused charges of other types stay in the inventory.
- Charges persist between levels. Charges used in a failed attempt are restored on retry.
- The special button does nothing if the special is unavailable in this setting. The HUD icon
  is greyed out and the hangar intel warns in advance.

### Acts 1–2 specials in detail

First-draft balancing values. Damage of the Airstrike already includes the ×2 `anti-ground`
bonus. "Boss part" = any hittable part of a mid-boss or act boss.

#### Airstrike (L04)

The numbers the game reads are in [data.yaml](data.yaml) (`airstrike` block); the table below
describes them.

| Property | Value |
|---|---|
| Charges | Bought at 300 cr each, at most 4 carried; one call uses one charge. **1 free charge** when the Airstrike unlocks (hangar visit before L04), so the player can try it without paying |
| Call | Press special → radio line ("Hammer flight, inbound!", Hammer Lead in the generic CDF portrait, text + blip). After **0.6 s** two CDF bombers (56×64 px) enter at the bottom edge at the player's x at the call − 64 px and + 64 px (clamped to the play field) and fly straight up at 600 px/s (0.9 s across the screen) |
| Bombs | Each bomber drops a bomb every 36 px of travel, the first at 18 px (15 each); a bomb bursts 0.25 s after release on the ground point under its release, which the scroll carries down meanwhile (ground coordinates); blast radius 32 px. The strike covers a corridor about 190 px wide |
| Damage | Per blast: 100 to `ground` and `low-air` targets (hardened included), 20 to `air` targets. One strike deals at most **300** to a ground/low-air target and **60** to an air target. `high-air`, `sub` and allies are not hit. Blasts come from above: they ignore frontal armour (the Scuttler's) |
| Rewards | Kills pay their normal bounty and keep the chain multiplier going |
| Bosses | At most 150 per boss part per strike (weak-point multipliers do not apply) |
| Defence | No invulnerability; enemy bullets are not cleared |
| Repeat | A new strike can be called once the bombers have left the screen (≈ 1.6 s after the call) |
| Limits | Not under the ice or under water, not beyond the gate (see the table above) |
| Audio / VFX | `bomb` family with the full whistle plus a jet flyby (SFX list); bombers, bomb carpet and blasts on the round-09 specials sheet |

#### Smart Bomb (L06)

| Property | Value |
|---|---|
| Charges | 400 cr each, at most 3. **1 free charge** when the Smart Bomb unlocks (hangar visit before L06), fitted only into an **empty** special slot (a fitted Airstrike stays) with a hangar notice, like the Airstrike's |
| Effect | Instant. A white flash (0.1 s at 80 % opacity, fading over 0.25 s; reduced by the flash-reduction option) and a shockwave ring that expands from the ship to cover the whole play field in **0.35 s** |
| Bullets | All enemy bullets on screen are removed at once; bullets spawned while the ring expands are removed as it passes them; spore mines pop as the ring passes them, as if shot |
| Damage | **120** once to every enemy on screen, on every layer (`air`, `low-air`, `ground` incl. hardened, `high-air`, `sub`) |
| Bosses | 60 to each boss part |
| Defence | Player invulnerable for **1.0 s** from activation (ship blinks) |
| Repeat | 1.5 s between bombs |
| Rewards | Kills pay their normal bounty and keep the chain multiplier going |
| Audio / VFX | `huge` explosion rung layered with a whoosh; flash and ring on the round-09 specials sheet |

#### Decoy Flares (L27)

Specified for Acts 1–2, but no enemy of Acts 1–3 fires a homing projectile: the first are the
Depth Hunter's torpedoes ([naval](../../enemies/naval/README.md), L27) and the SAM Nest's missiles
([ground](../../enemies/ground/README.md), L29). The unlock therefore moved from L07 to L27, the
hangar visit before the first of them (user decision D4 of M4 part G), so the shop never sells a
special with nothing to decoy.

| Property | Value |
|---|---|
| Charges | 150 cr each, at most 6 |
| Launch | 4 flares from the rear muzzle in a 120° fan backwards, starting at 300 px/s and slowing to a stop over 0.6 s; each burns for **4 s** |
| Effect | While any flare burns, every enemy homing projectile and seeker behaviour targets the nearest flare instead of the player, including homing shots launched during those 4 s. A homing projectile that reaches a flare detonates on it (the flare keeps burning) |
| Not affected | Aimed and straight bullets, lasers, contact attacks |
| Bosses | Same rules for boss homing attacks |
| Damage / defence | None |
| Repeat | 1.0 s between launches |
| Audio / VFX | `micromissile` family pitched up; flares on the round-09 specials sheet |

#### Common input rules

- The special button is buffered for 0.1 s: a press while the strike still flies calls the next one
  if the bombers leave within 0.1 s. With no charge left, no special fitted, a press that the buffer
  could not place (or in a setting where the special is unavailable) it plays the "special denied"
  sound and flashes the HUD special row.
- Charges are counted in flight: a special charge pickup adds one up to the most carried (it is only
  dropped when a special is fitted). The level reports the charges used and found, and the campaign
  applies them only for a won level.
- Charges used in a failed attempt are restored on retry (see above).

### One special at a time

The special slot holds **one** special; there is no swap button. Specials are changed in the
hangar.

## Concept art

Concept [round 09](../../concept-rounds/round-09/README.md) — generator `tools/concept/vfx_r09.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/specials-r09-a.png](concept/specials-r09-a.png) | Airstrike (CDF bombers, bomb carpet), Smart Bomb (flash, ring, bullets popping), Decoy Flares (homing shots retarget) (sheet) | chosen |
| [concept/specials-r09-a.gif](concept/specials-r09-a.gif) | Airstrike (CDF bombers, bomb carpet), Smart Bomb (flash, ring, bullets popping), Decoy Flares (homing shots retarget) (motion) | chosen |
| [concept/airstrike-bomber-final-r17-a.png](concept/airstrike-bomber-final-r17-a.png) | Production art, round 17 (`tools/art/airstrike_bomber.py`): the Airstrike's CDF bomber, 56×64 facing up, 4 engine-flame frames, and its soft ground shadow (sheet) | chosen |
| [concept/airstrike-bomber-final-r17-a.gif](concept/airstrike-bomber-final-r17-a.gif) | Production art, round 17: three bombers crossing a Luna road with their shadows (motion) | chosen |
| [concept/smart-bomb-final-r26-a.png](concept/smart-bomb-final-r26-a.png) | Production art, round 26 (`tools/art/smart_bomb.py`): the Smart Bomb's ring profile (`smart-bomb-ring`, 72×8, additive, drawn as a band of quads round the circle) and the burst at the bomb point (`smart-bomb-burst_0..11`, 128×128, additive, 30 fps), and the whole effect over a dimmed Level 01 field every 4th step (sheet) | proposed |
| [concept/smart-bomb-final-r26-a.gif](concept/smart-bomb-final-r26-a.gif) | Production art, round 26: the flash, burst and ring clearing a field of Needlers and bullets (motion) | proposed |

## Implementation

- [x] Special slot and charge counting: the special button (`Command.SPECIAL`) with its 0.1 s
  buffer and the denied sound, charges used and found reported by the level and applied for a won
  level only (a retry restores them), the special charge pickup (`--special airstrike:2` fits it
  for testing)
- [ ] Cooldown timers — **later: Act 3** (the Shield Overcharge at L19, the first cooldown special)
- [x] Airstrike as specified in *Acts 1–2 specials in detail* (M4 part D: the production bomber
  sprite with its engine flicker and ground shadow, the Bomb Rack's bomb, the blasts
  `explosion-medium`; the boss-part cap applies once bosses exist)
- [x] Smart Bomb as specified in *Acts 1–2 specials in detail* (M4 part F: its numbers in the
  `smart_bomb` block of data.yaml; `--special smart-bomb:2` fits it for testing; M4 part H: the
  production ring and burst of `tools/art/smart_bomb.py` over the spec's white field flash, proposed
  in round 26; its chosen sound by the SFX pass)
- [x] One free Smart Bomb charge granted once, at the unlock before L06: fitted only into an empty
  special slot, with the hangar notice (`free_charges: 1` in data.yaml)
- [ ] Decoy Flares — **later: Act 4** (unlock at L27 with the first homing projectiles; user decision D4 of M4 part G)
- [x] One free Airstrike charge granted once, at the unlock before L04
- [ ] Setting restrictions read from the level data — **later: Act 4** (the first restricted setting is under the Europa ice from L22; no Acts 1–3 level restricts a special)
- [x] HUD special row: icon, name, charges; greyed while the strike flies or with no charge; flashes
  when denied
- [ ] HUD: cooldown ring — **later: Act 3** (with the cooldown timers) — and the unavailable-in-this-setting state — **later: Act 4** (with the setting restrictions)

## Decisions

- 2026-09-30: Two special types (charge-based and cooldown-based); setting limits are a
  deliberate part of level preparation.
- 2026-09-30: Unlocks expressed as levels (`Lnn` = in the shop from the hangar visit before
  level *nn*). The Airstrike arrives at L04, matching the campaign's first-special anchor.
  Underwater limits follow [europa](../../world/europa/README.md#under-water-rules).
- 2026-10-01: One special equipped at a time (user accepted the recommendation).
- 2026-10-01: Airstrike, Smart Bomb and Decoy Flares specified in full for Acts 1–2 (timing, area, damage caps, boss rules, invulnerability, repeat delay).
- 2026-10-01: Concept round 09: specials chosen.
- 2026-10-01: Free first Airstrike charge (user decision): 1 charge is granted when the Airstrike unlocks before L04. The balance plan in `balance-plan.yaml` now buys 1 charge at L04 instead of 2 (same 2 charges carried).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: The charge price, charge limit and unlock of the three Acts 1–2 specials moved into [data.yaml](data.yaml) (M2 data files), where `tools/balance.py` reads them; the specials table stays hand-written for now (mostly prose).
- 2026-10-02: The "2 charges max 4" in the roster against the 1 free charge is left as it is for
  now (user decision during M2); it is an open question for M4, when the Airstrike is built.
- 2026-10-03: Airstrike charges (user decision): 1 free Airstrike charge, given once at the hangar
  visit before Level 04 (a save flag); if the special slot is empty the Airstrike is fitted into it,
  with a hangar notice. The roster cell is now "charges, max 4 (1 free at unlock)", which closes
  the open question.
- 2026-10-03: Bombs, shells and the Airstrike ignore the Scuttler's frontal armour, because they
  come from above (user decision). Their damage carries a from-above flag that the armour check
  reads once the Scuttler is built.
- 2026-10-03: M4 part D conventions (main-agent choice): `Command.SPECIAL` is the last command bit,
  so existing recordings keep their bits; the existing bindings (X, left Ctrl, gamepad B); a 0.1 s
  input buffer; with no charge the denied sound and a flashing HUD row; a new call only once the
  bombers have left the screen; the level reports the charges used and found and the campaign
  applies them only for a won level; the blasts land in ground coordinates (the release point
  carried down by the scroll during the 0.25 s fall); kills pay their bounty and keep the chain;
  a strike hits `ground` and `low-air` (100 per blast, at most 300 per strike) and `air` (20 per
  blast, at most 60), not `high-air`, `sub` or allies; the HUD special box is a right-panel row in
  the weapon rows' style, `SPECIAL  AIRSTRIKE ×2` with the 16 px hangar icon.
- 2026-10-03: The "Hammer flight, inbound!" call queues as an event line (main-agent choice): the
  radio has no interrupting priority yet (the HUD's *priority interrupts* item), so it plays as
  soon as the radio is free and is dropped if it would come later than the stale limit, rather
  than as a timed line that would play however late.
- 2026-10-03: The Airstrike's numbers moved into [data.yaml](data.yaml) (`airstrike` block, with
  the bomber's 56×64 size and the call's radio line), the free charge as `free_charges` and the
  input buffer as `input_buffer`. The Audio / VFX rows cited the round-08 specials sheet; the
  concept is round 09.
- 2026-10-03: M4 part D built the special slot and the Airstrike (see Implementation).
- 2026-10-03: Concept round 17 (user decision): the Airstrike's CDF bomber production art approved as **final** ([round 17](../../concept-rounds/round-17/README.md)).
- 2026-10-04: The Smart Bomb gets **one free charge** at its unlock (hangar visit before L06),
  fitted only into an empty special slot, with a hangar notice, like the Airstrike (user decision
  D5 of M4 part F), so the special is tried at all. The balance plan and the tests keep the
  Airstrike. Rejected: no free charge (at 400 cr, 41 % of budget(6), it would likely never be
  bought in Act 1). It sets the precedent for the Decoy Flares at L07.
- 2026-10-04: M4 part F built the Smart Bomb (main-agent choices): its ring grows from where the
  ship was when it went off; every enemy bullet and mortar blob goes at once, bullets the ring
  passes later go too; each enemy and boss part takes its damage once, when the ring reaches its
  centre; the 1.0 s invulnerability is the ship's mercy time (it blinks); it has no radio call;
  `free_charges: 1` moved into data.yaml.
- 2026-10-04: M4 part F step 3 (main-agent fix): the ring pops the spore mines it passes as if
  they were shot (their burst, their credits), like the bullets it clears.
- 2026-10-05: Decoy Flares (user decision D4 of M4 part G): the unlock moves from L07 to the act
  of the first homing enemy, so they leave the L07 intel and part G. Main-agent reading: the first
  homing projectiles are the Depth Hunter's (L27, Act 4), so the unlock is L27, the level itself
  rather than the act's opener, since the flares are useless before it; whether the Lamprey's
  chase at L12 counts was left open. Rejected: building them at L07 (a special that does
  nothing for 20 levels) and giving the carrier homing seekers to decoy (changes an approved boss).
- 2026-10-05: The user confirmed the Decoy Flares unlock at L27 (the first homing projectile);
  the Lamprey's chase at L12 does not count, so the flares stay out of M5.
- 2026-10-05: M4 part H docs reconciliation: the cooldown timers and ring are tagged for Act 3
  (Shield Overcharge, L19), the setting restrictions and the greyed state for Act 4 (Europa, from
  L22); every other item is ticked, so the document is `done`.
- 2026-10-05: M4 part H (round 26 batch): the Smart Bomb's production art from the chosen round-09
  sheet, `tools/art/smart_bomb.py`: the ring's cross-section (`smart-bomb-ring`, the sheet's halo,
  white core and trailing cyan band) drawn by `FarsideLooks.drawSmartBomb` as 72 quads round the
  circle with the profile across the band, so it stays crisp at every radius and dims over the last
  30 % of its run; a burst at the bomb point (`smart-bomb-burst`, 12 frames at 30 fps); the white
  field flash stays the spec's fill. The strokes remain the fallback without the files. Review
  files proposed for round 26; not drawn yet (kept for later): the bullets popping with a sparkle
  as the ring passes them, which the round-09 sheet shows.
