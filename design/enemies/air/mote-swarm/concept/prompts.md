# Mote Swarm – generator notes and prompts

The chosen concept is [mote-swarm-r05-a](../../concept/prompts.md#mote-swarm-r05-a) (round 05, in
the air units' `concept/`); its prompt stays valid for the look.

## mote-swarm-final-r32-a

Round 32, production art (M5 part D batch, user decision D10 = a: straight to production from the
chosen concept). Review sheet (`.png`) and loop (`.gif`) made from the files in `assets/` by
`python3 tools/art/mote_swarm.py --review`; the frames are rendered by
`python3 tools/art/mote_swarm.py` (see `tools/art/README.md`).

- `mote-swarm_0..47` (16×16, 24 colours, `16 angles`): the round-05 model
  (`tools/concept/render/archetype_models.mote`, imported unchanged) at 16 headings × the concept's
  3 glow-flicker levels (0.8, 1.1, 1.5), indexed heading × 3 + frame; heading k flies k × 22.5°
  clockwise from straight down with one of the three bone spikes leading, so a flock reads as
  aligned while the body stays radial. A faint 1 px light rim (0.5) for the dawn sprawl.
- `mote-swarm-death_0..7` (24×24, additive, the `tiny` timing: 2 steps a frame, 4 steps after the
  pop): the ember puff, a crimson-white pop, a crimson bloom and orange embers flung out and dying.
- `mote-swarm-tatters_0..7` (24×24, solid, 2 steps a frame): the three dark fins and the husk
  split in two, flung out, tumbling, shrivelling.

The sheet shows the 16 headings, the flicker at headings 0 and 8, a row at 1× on the dawn stand-in
(see the Wraith's notes), the death pieces and puff with `explosion-tiny`, and the frames at 1×.
The loop flies a 20-mote boids flock (separation 18 px, alignment, cohesion toward a leader route;
the review's own simulation, not the game's): it swirls in front of the ship, leaves the bottom-left
edge, the warning blinks, it loops back up through the ship's lane at 260 px/s, four motes are shot
down. Not an image-generator prompt: this is the brief for reviewing the production frames.
