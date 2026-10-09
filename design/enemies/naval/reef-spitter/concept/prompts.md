# Reef Spitter – generator notes and prompts

The chosen concept is [reef-spitter-r06-a](../../concept/prompts.md#reef-spitter-r06-a) (round 06, in
the naval units' `concept/`); its prompt stays valid for the look.

## reef-spitter-final-r33-a

Round 33, production art (M5 part E batch, user decision E9 = a: straight to production from the
chosen concept, with the round-07 water rules: waterline cut and foam collar; water drawn as the
hybrid of E4 = c). Review sheet (`.png`) and loop (`.gif`) made from the files in `assets/` by
`python3 tools/art/reef_spitter.py --review`; the frames are rendered by
`python3 tools/art/reef_spitter.py` (see `tools/art/README.md`). The sea and the game's `sub` pass
are stand-ins in the review (see the Driftjelly's notes).

- `reef-spitter_0..31` (36×36, 40 colours, `32 angles`): the round-06 barnacle gun
  (`tools/concept/render/r06_models.reef_gun`, imported unchanged) at 32 headings, k × 11.25°
  clockwise from straight down; the violet barrel root and mouths emissive.
- `reef-spitter-recoil_0..31` (36×36): the recoil frame, the three mouths pulled back and their glow
  flared; shown 6 steps after a fan. The muzzle points per heading are in
  `assets/pivots/reef-spitter.json`.
- `reef-spitter-raft-sub` (96×96): the plain kelp raft (`r06_models.reef_raft`, 84 px across), drawn
  through the `sub` pass as the raft's under-water part (lower lobes, seaweed fringe).
- `reef-spitter-raft_0..7` (96×96, 12 steps a frame): the raft above the waterline with its foam
  collar, bobbing (the water plane ±0.03 units, a 3° tilt round a turning axis); the gun's point per
  frame in the pivot file.
- `reef-spitter-sink_0..9` (96×96, 6 steps a frame = 1 s): the gunless raft going down, one side
  first, the collar closing in, bubbles, a last swirl of foam; over the fading `-raft-sub`.
- `reef-spitter-death_0..9` (64×64, additive) and `reef-spitter-tatters_0..9` (64×64, solid), 4 steps
  a frame: the gun's `small` violet burst, and its six plates, three mouths and split cone flung out
  and darkening into the water.
