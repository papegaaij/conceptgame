# Brood Carrier – generator notes and prompts

## brood-carrier-final-r25-a

Round 25, production art (M4 part G, the Level 07 batch, step A1; straight to production by part F's
D8 rule: the concept was chosen in round 04). Review sheet (`.png`) and loop (`.gif`) are made from
the final files in `assets/` by `tools/art/brood_carrier.py --review`; the frames are rendered by
`tools/art/brood_carrier.py` (see `tools/art/README.md`). The model is the chosen round-04 concept
([brood-carrier-r04-a](../../concept/brood-carrier-r04-a.png), `tools/concept/enemies_r04.py`, models in
`tools/concept/render/enemy_models.py` `brood_carrier` with `brood_carrier_scheme_mats`): the seven
teal-black hull segments, the dorsal plates, the flight membranes, the head with its eye clusters,
outer mandibles and limbs, the trailing tendrils, 288 px across 2.3 model units as the concept
sprite. It is re-laid to the data: the bay craters, the core collar and the turret socket sit at
the `part_list` offsets, so every part sprite drawn at its offset covers the hull's own part.
Production detail: raised bay craters the lime sacs swell out of, a raised collar round the plate
iris, a thin teal seam along the spine between the plates, bone spikes between the dorsal plates, a
socket on the head for the separate mandible turret.

The game composes the carrier from its sprites (`HullBossLooks`), nothing lit turned at runtime
(user decision D1 of part G):

- **Hull** (`brood-carrier-hull`, 9 frames): 0 nose-down (288×626; phase 1 on `high-air`, drawn at
  1.25× with the shadow cast from its alpha at runtime), 1–7 the 90° turn in place in even steps of
  11.25°, the head swinging to the right, each a render of the turned model on a canvas centred on
  the hull's centre, 8 broadside (626×288; phases 2–3). The turn frames carry the parts (closed
  sacs, closed iris, the turret along the head); at the two ends the part sprites lie over the hull.
- **Bay sacs** (`brood-carrier-sac-down`, `-sac-side`, 40×40, 5 frames each): closed (the glossy dark
  sac sunk in its crater, faint lime veins), swelling twice, open (bright lime, the weak point),
  burst (the ruptured shell charred, lime ichor welling in the pit); the side set is the same sac
  rendered broadside (its long axis along the hull). Each is the inside of the crater (a disc), so
  the rim stays the hull's.
- **Plate iris** (`brood-carrier-iris`, 64×64, 5 frames): six plates closed over the core, sliding
  back under the collar over the 1 s opening, open over the lime core; one set for both poses. The
  **core glow** (64×64, additive) pulses over it while the core is exposed.
- **Mandible turret** (`brood-carrier-turret`, 40×40, 17 frames): the maw with its inner mandible
  pair at 11.25° steps from −90° to +90° of straight down (`headings` in
  `assets/pivots/brood-carrier.json`), aimed at the ship by the game.

One 48-colour palette over every solid frame (12 of the colours cut from the lime and teal glows).
The sheet shows the carrier as the game composes it (nose-down closed and open, broadside closed
and in phase 3), the data's hit boxes and offsets over both poses, the nine hull frames, then
every part set. The loop: the pairs opening head to tail nose-down, the turn, the broadside
windows with two sacs burst and the turret tracking, the iris opening over the pulsing core.

Not an image-generator prompt: the brief for reviewing the production frames. The concept's prompt
([bosses prompts](../../concept/prompts.md#brood-carrier-r04-a)) stays valid for the look.

## brood-carrier-death-final-r25-a

Round 25, the carrier's break-up at its death, planned from the start (rounds 16 and 21: "now it
just disappears"). Review sheet (`.png`) and the whole death (`.gif`, 30 fps) made from the final
files in `assets/` by `tools/art/brood_carrier_death.py --review`; the sprites and the `death`
table of `assets/pivots/brood-carrier.json` by `tools/art/brood_carrier_death.py` (the Leviathan's
and the frigate's mechanism, `SetPieceDeath`).

- `brood-carrier-chunk-1..8` (3 frames each): the wreck broadside (where it always dies: phase 3),
  every sac burst, the iris open over the burst core, cut along jagged seams into the tail, the aft
  section, the middle split along the spine (upper, lower), the fore section, the head and two torn
  flank pieces with a membrane each; the chitin along a cut charred, the torn flesh glowing lime
  along the broken edge and in veins down the cut face. Frame 0 in place; frames 1–2 the piece
  tilting and turning about its own centre, each ray-marched at 4× under the hull's key light.
- `brood-carrier-ichor` (96×96, 10 frames, additive): a lime flash and lime and teal droplets flung
  out and fading, which the game plays with every part's burst in the chain and at its end.
- Timeline (game steps of 1/60 s): the game's chain from the tail (step 0) to the head (step 180)
  with a medium burst and ichor at every part and twelve along the hull; over it a wave of large
  and medium blasts running with the chain's front, and a cluster over the five seams and the
  flanks at its end, where the body is replaced by the chunks (the swap, step 180, under the screen
  flash); then the chunks drift slowly apart for 12 s, sinking to 0.8×, darkening by 60 % and fading
  from 60 % of the way, under trailing blasts and ichor that follow them.

Not an image-generator prompt: the brief for reviewing the production frames.

## brood-carrier-capture-final-r25-a

Round 25, a capture of the game, not generated art (see the sheet's caption in the Concept art
table of the [README](../README.md#concept-art) for the command line and what each frame shows).
