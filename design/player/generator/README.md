---
title: Generator
design: approved
implementation: in-progress
art: none
depends-on: [../weapons, ../shields]
updated: 2026-10-02
---

# Generator

## Summary

The generator's output (MW) caps the total power draw of the fitted loadout. Heavy weapons need
a bigger generator. Output that is not used boosts shield regeneration. The model is described
in [player](../README.md#power-budget).

## Design

<!-- data: generators -->
| Model | Output | Price (first draft) | Available |
|---|---|---|---|
| Mk I "Spark" | 8 MW | starter | start |
| Mk II "Arc" | 11 MW | 1 500 | act 1 |
| Mk III "Fusion" | 14 MW | 4 000 | act 2 |
| Mk IV "Tokamak" | 18 MW | 9 000 | act 4 |
| Mk V "Helix" | 22 MW | 18 000 | act 5 (Helix Dynamics tech, captured) |
| Mk VI "Choir Core" | 27 MW | 32 000 | act 7 (Vrell tech) |
<!-- /data -->

- Buying a generator replaces the current one. The old one goes to the inventory and can be
  sold (see [economy](../../systems/economy/README.md)).
- Spare power bonus: +10 % shield regen per spare MW, max +50 %.
- In a level the generator feeds the HUD power gauge (see [HUD](../../ui/hud/README.md)).
  Normally its output is constant, but some enemies can drain it (below).

### Enemy drain effects

Some enemies attack the power budget instead of the hull, first the
[Void Leech](../../enemies/space/README.md) (introduced in L33). Rules (first draft):

- An attached drainer lowers the **effective output** by 2 MW (Void Leech); drains stack up to
  −6 MW. Shaking it off (moving fast) or shooting it restores the output at once.
- While the load is at or below the effective output, nothing changes except a smaller
  spare-power bonus.
- When the load exceeds the effective output: **shield regeneration stops first**; if the
  shortfall is larger than the shield's draw, **weapon fire rate** drops in proportion to
  output ÷ remaining load. Armour, specials and the escort are never affected.
- The HUD power gauge shows the drained part in a warning colour and flashes when the load
  exceeds the output. A heavy loadout on a small generator is the most vulnerable, which is the
  point of L33 (see its threat profile in [act 5](../../campaign/act-5-the-belt/README.md)).

### Overdrive and the power cap

An overdrive pickup (all weapons +1 level for 20 s, see [player](../README.md)) **ignores the
power cap**: the extra draw of the overdrive patterns is not counted, so overdrive never stops
shield regeneration or lowers fire rate. It is a reward, not a risk.

## Implementation

- [x] Generator models and output values in the item data
- [x] Load vs output check in the hangar with a projected-load bar
- [x] Spare-power shield regen bonus

## Open questions

- None open.

## Decisions

- 2026-09-30: Six generator tiers; top two are captured tech tied to the story.
- 2026-09-30: Enemy drain effects defined (Void Leech, L33): output reduction, shield regen
  stops first, then fire rate scales down.
- 2026-10-01: Overdrive ignores the power cap (user accepted the recommendation).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: The generator models and the spare-power bonus moved into [data.yaml](data.yaml) (M2 data files); the model table is rendered from it.
- 2026-10-02: M3 part B2: generators are bought in the hangar (the old one goes to the inventory);
  a smaller one cannot be fitted while the load exceeds its output. The power bar shows the load,
  the projected load of the selected choice (red when it would not fit) and the output.
- 2026-10-02: M4 part A: the spare-power bonus applies in flight (`SimSpecs.regenBonus` on the shield's regen, from `spare_power` in the data) and shows on the HUD's power row.
