# Ravager – generator notes and prompts

The chosen concept is [ravager-r05-a](../../concept/prompts.md#ravager-r05-a) (round 05, in the
ground units' `concept/`); its prompt stays valid for the look.

## ravager-final-r31-a

Round 31, production art (M5 part C batch, user decision D9 = a: straight to production from the
chosen concept, Level 09 in the last hour of night). Review sheet (`.png`) and loop (`.gif`) made
from the files in `assets/` by `python3 tools/art/ravager.py --review`; the frames are rendered by
`python3 tools/art/ravager.py` (see `tools/art/README.md`). The size and the stride come from the
Ravager's `data.yaml` when it has them (56×56, 46 px per gallop cycle, the concept's).

- `ravager_0..127` (56×56, 32 colours, `16 angles`): the round-05 model
  (`tools/concept/render/beast_models.ravager`, imported unchanged) at 16 headings × 8 gallop
  frames, indexed heading × 8 + frame; heading k runs k × 22.5° clockwise from straight down
  (heading 0 down the screen, 4 to the left, 12 to the right). The model is drawn at 0.95 and set
  0.1 model units back: the concept's jaw tips and tail tip ran over the sprite's edge. **Night city
  readability** (as the Creeper's in round 30): the rust chitin albedo ×1.25, the dark sinew and tail
  bands mixed 35 % toward the rust, the Creeper's 1 px lavender-white light rim. The material
  patterns turn with the body; every heading is its own render under the fixed key light.
- `ravager-glow_0..127` (56×56, additive, 16 colours, same order): the teal maw's and the eyes'
  emission alone, drawn above the low-air layer.
- `ravager-leap_0..63` (80×80, the gallop's palette): the leap pose (fore legs reaching, hind legs
  thrown back, jaws wide) at 16 headings × 4 lift steps, indexed heading × 4 + step; step s is drawn
  1.11, 1.21, 1.32, 1.43 × the ground size (lift 0.25, 0.5, 0.75, 1), so no lit frame is scaled at
  runtime. `ravager-leap-glow_0..63` (80×80, additive): its maw and eyes, same order.
- `ravager-death_0..9` (64×64, additive, 4 steps a frame): the `small` organic burst: a teal-white
  flash from the maw, a teal bloom, teal ichor flecks with short trails, a rust-brown mist, motes.
- `ravager-tatters_0..9` (64×64, solid, 4 steps a frame): the four legs torn off, the tail in three
  pieces, the two jaws, the skull, chitin flakes off the back and the torso split in two, flung out,
  tumbling and shrivelling; no husk is left.

The pounce's shadow is not baked (the Shadows rule): the game draws it from the leap frame's
silhouette, pushed down-right by (21, 30) px × the lift and scaled back to the ground size.

The sheet shows the 16 headings, the gallop at headings 0, 4 and 10, the glow masks at heading 4,
the leap steps at headings 0 and 6 with their glow, the pounce as drawn (ground, then the four
steps with the shadow), the death pieces and glow, both with the `small` burst, and the frames at
1×. The loop has a pack of four galloping across Level 08's avenues 0.25 s apart at 160 px/s (the
gallop frame following the distance, the glow masks added); the lead pounces at a point (0.75 s,
lift sin(π t)), lands, runs on and is shot. Not an image-generator prompt: this is the brief for
reviewing the production frames.

Outcome (user, 2026-10-07): approved as **final** in round 31, the weak spots as they are.
