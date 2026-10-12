# Siege Spire – generator notes and prompts

The chosen concept is [siege-spire-r06-a](../../concept/prompts.md#siege-spire-r06-a) (round 06, in the
bosses' `concept/`); its prompt stays valid for the look.

## siege-spire-final-r36-a

Round 36, production art (M5 part H step H2; H8 = a: straight to production from the chosen concept;
H1 = a, H4 = a: one anchored boss without lanes, all six roots on the screen from the start, the
phase-3 lift; the user is on vacation, so all of it is pending the user's review). Rendered by
[tools/art/siege_spire.py](../../../../../tools/art/siege_spire.py) (`python3 tools/art/siege_spire.py`;
the frames go to `assets/` only with `--assets`, in the content step's time: no data claims them yet,
and `packAtlases` fails on sprites no data claims). Not prompted: the round-06 models
(`tools/concept/render/boss_models.spire`, `root_pod`) at the concept's 104.8 px per unit, rendered at
4× with the concept canvas's key light. The review's ground, bullets, booms and player are the concept's
stand-ins (`tools/concept/bosses_r06.py`); the Wraiths are the production `wraith-cloak` shimmer.

- **Split for independent deaths**: the trunk (trunk, spines, ridges, claw crown, the creep mound out to
  0.62 units) and the six roots (each a 60° sector beyond 0.62 units, below the spines, with its piece of
  the mound) are cut from the one model by SDF masks, so they meet without a seam; a dead root is the
  concept's withered tissue with its mound sector withered too.
- `siege-spire_0..2` (180×240): the trunk with the crown shut, half open and open (the lime maw exposed,
  phase 2); the roof pushed 63 px up the screen (the tall-structure rule).
- `siege-spire-root_0..11`: roots 0–5 alive / dead (canvases 100–134 px); roots 1 and 4 carry the
  mortars.
- `siege-spire-pod_0..31` (46×46): the root turret pod, 32 headings; `siege-spire-mortar_0..2` shut,
  opening, open (the lob); `siege-spire-husk_0..1` a dead turret and mortar (glow out).
- `siege-spire-stump` (154×154): the concept's withered base with the socket the spire tore out of (a
  pit with a dimly glowing floor, torn slate flaps round its rim); the dead roots stay round it.
- `siege-spire-lift_0..5` (204–238 px): the free spire tearing free, scale 1.0 → 1.17 of the airborne
  frames, its torn stubs thrashing; `siege-spire-free_0..15` (244×244): airborne at 1.2, its maw open so
  the lime core (the ×2 weak spot) shows, 16 headings over its 60° symmetry (3.75° steps).
- `siege-spire-emitter_0..3` (20×20, additive): a petal emitter's crimson charge for the sweep's
  telegraph, drawn at the six points per heading in `pivots/siege-spire.json` (the review's pivot check);
  `siege-spire-maw-glow_0..3` (72×72, additive): the maw's lime bloom on a launch or a fan;
  `siege-spire-debris_0..7` (16×16): slate shards for the tear-free shockwave.
- Atlas: 91 regions, about 41 % of one 2048² page by area (a shelf pack fills 918 of 2048 rows), so the
  whole boss and its death chain (step H5) fit one page.
- Not here: the death chain (`siege_spire_death.py`, step H5, below), the shockwave's dust ring (the root
  burst's dust), the canopy's withering (`daylight_l14.py`, `backdrop_l14.py`), the intel silhouette
  (`intel.py`).

## siege-spire-death-final-r36-a

Production art, M5 part H (round 36; H8 = a, no a/b for the death, pending the user's review). Not a
mockup: the sheet and GIF of `assets/sprites/siege-spire-death_0..7`, rendered by
[tools/art/siege_spire_death.py](../../../../../tools/art/siege_spire_death.py) (`python3
tools/art/siege_spire_death.py --assets`, after `siege_spire.py --assets`) from the production free
spire's heading 0 (`siege-spire-free_0`, the pose it dies in), 2D like the other deaths made from their
own sprites: frames 0-3 (0-1.5 s of the 3 s chain) the core's lime light cracking out along the six petals
and branching, brighter frame by frame; frames 4-7 (1.5-3 s) the six petal chunks (60° sectors round the
core) flying apart, the core's light gone, the tissue charring, their edges burning down to embers
(`loot_targets.crumbled`) until nothing is left; 244×244, one 48-colour palette, drawn as the free frame is
(not additive). The game's chained `huge` explosions play over it; the stump stays on the ground.
