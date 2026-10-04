# Gorgon Frigate – generator notes and prompts

## gorgon-frigate-final-r21-a

Round 21, production art (M4 part E, the Level 05 batch). Review sheet (`.png`) and loop (`.gif`) are made from the final files in `assets/` by `tools/art/gorgon_frigate.py --review`; the frames are rendered by `tools/art/gorgon_frigate.py` (see `tools/art/README.md`). The model is the chosen round-06 concept ([gorgon-frigate-r06-a](../../concept/gorgon-frigate-r06-a.png), `tools/concept/render/boss_models.py`): the bone/violet/lime materials, the ribbed dome with its scalloped rim, the crown petals over the lime core and the tendril veil; the neck segment and the cobra head are the concept's `gorgon_neck` and `gorgon_head`, unchanged. The bell is re-laid to the data: wider than long (hit box 200×120), the three neck sockets on its front edge at the chains' anchors, the core at its offset, 100 px per model unit.

The game composes the frigate from its sprites (`BossLooks`), nothing lit turned at runtime:

- **Bell** (240×200, 4 frames): the crown closed over the sunken core (phases 1–2), two opening stages and open with the core raised (phase 3, the frame stepping over the 1 s crown opening); its centre at `bell.origin` of `assets/pivots/gorgon-frigate.json`. The **core glow** (72×72, additive) pulses over the open core and goes white on a hit.
- **Neck pieces** 1–5 (50 to 40 px, tapering from the bell to the head) and the **head** (60×60) and its **stump** (48×48, the torn, charred neck end welling violet ichor, drawn once the head is destroyed): each a set of 15 headings, the data's 32 angles within ±78.75° of straight down (11.25° steps clockwise from down, frame = step + 7), each its own render under the fixed key light. A neck's anchor turns at most 55° (enraged) off a rest line at most 9.5° off straight down, so no piece goes beyond the set; the game rounds a piece's heading to the nearest frame and clamps it.
- **Death**: `gorgon-frigate-petals` (160×160, 6 frames at 10 fps, solid), the five crown petals torn off the core, charred at their bases, tumbling outwards and falling away (smaller) with ichor droplets trailing; `gorgon-frigate-ichor` (96×96, 10 frames, additive), a lime flash and violet droplets flung out and fading, which the game plays with every part's burst and the large one at the centre.

One 48-colour palette over every solid frame of the boss. The sheet draws the frigate as the game composes it (at rest, necks swinging, phase 3), the data's hit boxes over it, then every set. The loop: the necks swinging with lag, the heads destroyed one by one (the last neck lashing wider), the crown opening over the glowing core, then the petals and the ichor.

Not an image-generator prompt: the brief for reviewing the production frames. The concept's prompt stays valid for the look.

## gorgon-frigate-death-final-r21-b

Round 21 item 11, the redo of the death in `gorgon-frigate-final-r21-a` ("needs more explosions and
pieces; now it just disappears"). Review sheet (`.png`) and the whole death (`.gif`, 30 fps) made
from the final files in `assets/` by `tools/art/gorgon_frigate_death.py --review`; the sprites and
the `death` table of `assets/pivots/gorgon-frigate.json` by `tools/art/gorgon_frigate_death.py`
(the Leviathan's mechanism, `tools/art/leviathan_death.py`).

- `gorgon-frigate-chunk-1..5` (3 frames each): the bell with its crown open, the petals gone (the
  `gorgon-frigate-petals` sprite tears them off at the swap) and the core burst into wet violet
  ichor, cut into five wedges round the core along jagged seams (right flank, back right and back
  left with the tendril veil, left flank, front; each flank and the front with a socket); the dome
  along a cut charred, the torn flesh glowing violet along the broken edge and in veins down the
  cut face. Frame 0 in place; frames 1–2 the wedge tilting outwards (up to 0.32 rad) and turning
  (up to 0.2 rad), each ray-marched at 4× under the bell's own key light, 48 colours over the set.
- `gorgon-frigate-chain-1..3` (3 frames each): a neck with its torn stump (left, centre, right),
  composed from the pre-rendered neck and stump heading frames: at rest, then turning (up to
  0.55 rad) and curling as it falls away.
- Timeline (game steps of 1/60 s): the part chain at 0/6/12/18; 13 large and 6 medium blasts and
  two ichor clouds from step 24, peaking at the swap (54); then the chunks drift apart, sink to
  0.85×, darken by 60 % and fade from 55 % of the break-up to step 160, under 8 trailing blasts that
  follow them.

Not an image-generator prompt: the brief for reviewing the production frames.
