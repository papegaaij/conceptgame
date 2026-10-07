# Creeper – generator notes and prompts

The chosen concept is [creeper-r06-a](../../concept/prompts.md#creeper-r06-a) (round 06, in the
ground units' `concept/`); its prompt stays valid for the look.

## creeper-final-r30-a

Round 30, production art (M5 part B batch, user decision D6 = a: straight to production from the
chosen concept). Review sheet (`.png`) and loop (`.gif`) made from the files in `assets/` by
`python3 tools/art/creeper.py --review`; the frames are rendered by `python3 tools/art/creeper.py`
(see `tools/art/README.md`). The sizes and the stride come from the Creeper's
[data.yaml](../data.yaml).

- `creeper_0..95` (60×60, 32 colours, `16 angles`): the round-06 model
  (`tools/concept/render/r06_models.creeper`, geometry copied so the legs' swing follows the stride)
  at 16 headings × 6 walk frames, indexed heading × 6 + frame; heading k walks k × 22.5° clockwise
  from straight down (heading 0 down the screen, 4 to the left, 12 to the right). One diagonal-gait
  cycle carries it 22 px, so the planted feet stay put: a foot swing of ±0.21 model units instead
  of the concept's ±0.18. Body and tail sway in the concept's S-curve with the gait. The material
  patterns turn with the body (model space, as in round 06); every heading is its own render under
  the fixed key light. **Readability on the night city** (2026-10-07, after the Level 08 capture
  `level-08-capture-final-r30-a`: dark grey-brown on dark navy): the slate chitin's albedo ×1.4,
  the legs and frill mixed 40 % from the dark chitin toward the slate, three violet glow pores
  along the back's crest (spine nodes 2–4, ~2.6 px across) and a 1 px lavender-white light rim
  (`ded2f8`, cool, not the loot targets' warm white) on the silhouette's outermost
  pixels, 80 % where they face the top-left key light and 45 % where they face away (`light_rim`).
- `creeper-husk_0..15` (60×60, 24 colours): the legless body at each heading for its remains: head,
  frill, body and tail with short hip stubs, scorched (albedo ×0.8 of the lifted body), the gland's
  and the back pores' glow almost out, the light rim at 55 %.
- `creeper-glow_0..95` (60×60, additive, premultiplied on black, 15 colours, same frame order): the
  five gland pores', the three back pores' and the eyes' emission alone, rendered from the same
  scene with every other material black; drawn above the low-air layer, so the gland and the back
  show through Level 08's fog and smoke banks.
- `creeper-death_0..15` (96×96, additive, 4 steps a frame): the gland bursting violet: a
  violet-white flash, violet ichor globules with short trails, a dark plum mist that billows low and
  thins, a few motes (the Scuttler's organic burst in the Creeper's colours).
- `creeper-tatters_0..15` (96×96, solid, 32 colours, 4 steps a frame): the six legs torn off at the
  hips with their pale toe pads, the fan frill split into four shards and six chitin chips off the
  back (four with a bone stud), flung out, tumbling, settling and shrivelling; ray-marched per
  frame from the walker's geometry and materials (the lifted colours), posed at heading 0.

The sheet shows the 16 headings, the walk cycle at headings 0, 4 and 10, the husks, the glow masks
at heading 4, the death pieces and glow, both together with the `medium` burst over the husk, and
the frames at 1×. The loop has a convoy of three walking a night street that turns, 1.5 s apart at
35 px/s (the walk frame following the distance), their glow masks added; the lead dies on the
lower straight and leaves its husk. Not an image-generator prompt: this is the brief for reviewing
the production frames.

Outcome (user, 2026-10-07): approved as **final** in round 30, with the readability lift.
