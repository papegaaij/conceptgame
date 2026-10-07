# Hive Node – generator notes and prompts

The chosen concept is [hive-node-r06-a](../../concept/prompts.md#hive-node-r06-a) (round 06, in the
ground units' `concept/`); its prompt stays valid for the look.

## hive-node-final-r31-a

Round 31, production art (M5 part C batch, user decision D9 = a: straight to production from the
chosen concept, Level 09 in the last hour of night). Review sheet (`.png`) and loop (`.gif`) made
from the files in `assets/` by `python3 tools/art/hive_node.py --review`; the frames are rendered by
`python3 tools/art/hive_node.py` (see `tools/art/README.md`). The size is the stat block's 76×76.

- `hive-node_0..23` (76×76, 40 colours, one heading: radial, never turns): the round-06 model
  (`tools/concept/render/r06_models.hive_node`, imported unchanged) at 6 iris states × 4 pulse
  frames, indexed iris × 4 + pulse; iris 0 shut, 1–4 opening (0.2–0.8), 5 open; the pulse a
  breathing loop of the seven lobes (swell 0, 0.4, 1, 0.6). **Night city readability** (as the
  Creeper's in round 30): the teal-black lobes and sac albedo ×1.45, the tendrils and iris petals
  mixed 40 % toward the lifted lobes, the Creeper's 1 px lavender-white light rim (`ded2f8`, 80 %
  facing the top-left key light, 45 % away). The throat glows at 30 % while shut and full when open.
- `hive-node-glow_0..23` (76×76, additive, premultiplied on black, 16 colours, same order): the
  throat's, the spawn polyps' and the sac veins' emission alone, plus a soft teal halo over the iris
  that grows with its opening (σ 5–13 px, 55 % at full): the iris glows while it opens, the spawn's
  telegraph. Drawn above the low-air layer like the walkers' glows.
- `hive-node-creep_0..8` (192×192, solid, 24 colours, ground): the creep patch under the node (the
  round-06 creep's look: a teal-black mat, glossy blobs lit from the top left, nine root tendrils
  with teal veins, reaching ~86 px). Frame 0 alive; 1–8 the wither after the node's death, 15 steps
  a frame (2 s): blobs shrink, dry to grey-brown and crumble away from the rim, the tendrils pull
  back, the veins go out; 8 is the dry crust left.
- `hive-node-death_0..15` (128×128, additive, 32 colours, 4 steps a frame): the iris bursting: a
  teal-white flash, teal ichor globules with short trails, a teal-black spore mist that billows low
  and thins, motes; played with the `large` burst.
- `hive-node-tatters_0..15` (128×128, solid, 32 colours, 4 steps a frame): the seven bone plates
  with their spikes flung out tumbling, the five iris petals, the seven lobes slumping where they
  were and shrivelling; ray-marched per frame from the model's geometry and lifted materials.
- `hive-node-stump` (76×76, single, 24 colours): the collapsed mound left as its remains: the sac
  slumped flat and torn open, the lobes deflated, three plate stubs of seven, the tendrils, scorched
  (albedo ×0.7), no glow, the rim at 50 %.

The sheet shows the iris states, the pulse loop shut and open, the glow masks, the iris on Level 08's
avenues with its glow, the creep's wither, the stump, the death pieces and glow and both with the
`large` burst over the stump and the withering creep, and the frames at 1×. The loop has the node
in its creep on Level 08's avenues opening twice, 4 s apart (0.5 s opening, 0.5 s open, 0.25 s
closing), two Skitters (with their drop shadows) bursting out in an arc each time; then it dies,
the creep withers and the stump stays. Not an image-generator prompt: this is the brief for
reviewing the production frames.

Outcome (user, 2026-10-07): approved as **final** in round 31, the weak spots as they are.
