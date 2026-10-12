# Lamprey: production review files

## lamprey-final-r34-a

Production art, round 34 (`lamprey-final-r34-a.png`, `.gif`; user decision F8 = a: straight to
production from the chosen concept): not prompted, rendered by `python3 tools/art/lamprey.py` from
the chosen round-06 model (`tools/concept/render/r06_models.lamprey`, unchanged; the concept sheet was
made by `tools/concept/enemies_r06.py`), every pose its own ray-marched render at 8× under the fixed
key light (nothing lit rotated or mirrored), a 1 px lavender-white light rim (0.7) against the dark
storm sea, one 32-colour palette over the body sets. The latched pose is the model's `latched` disc
flattened onto the hull, facing in at each spot; the stunned pose bends the model into a C in its own
frame and dims the teeth; the death sets are a 2D teal light field (the wet pop) and ray-marched pieces
of the model's parts. `assets/pivots/lamprey.json` holds the spots' offsets from the ship's centre
(left −22, 13; right 21, 13; tail 0, 35 px, the mouth 2 px over the Stormhawk's level frame) for the
latch's `spots` in the data (step 4). The sheet and GIF are built from the files in `assets/sprites/`
over a storm-sea stand-in (Level 11's swell tile, darkened); the concept's image prompt is in
[../../concept/prompts.md](../../concept/prompts.md).

## latch-r34-a

The latch, look a "gulp" (`latch-r34-a.png`, `.gif`; the game's names `lamprey-latched_0..11`):
rendered by `tools/art/lamprey.py`. Brief: a latched Lamprey reads as feeding, not as a decal — the
body nearly still (the model's latched pose) swelling and narrowing across a 4-frame swallow (0.4 s a
gulp, the body's width ×1.00/1.07/1.12/1.05), and the ship's own shield ring (`ship-shield`,
additive) pulsing with each gulp at 0.55/0.3/0.12/0 of its strength, as if shield light were drawn in.
The HUD's shield bar flickers 8 steps on, 8 off under `SHIELD DRAIN` (the HUD's spec, the same in both
looks).

## latch-r34-b

The latch, look b "siphon" (`latch-r34-b.png`, `.gif`; review only, `--latch b` writes it under the
game's names plus `lamprey-drain_0..3`): the tail keeps lashing (the swim cycle at latch 0.6), and at
the contact point cyan shield motes spiral into a teal glow in the mouth (32 px, additive, 4 frames at
10 fps). No shield ring.
