# Level 09 – Arcology Fall: concept and production art briefs

## backdrop-final-r31-a

Production art, M5 part C (round 31; user decision D9 = a: straight to production). Not a mockup:
the sheet and GIF of Level 09's backdrop in `assets/backdrop/level-09/` and the rooftop cocoon in
`assets/sprites/cocoon*`, rendered by
[tools/art/backdrop_l09.py](../../../../../tools/art/backdrop_l09.py) (`python3 tools/art/backdrop_l09.py`,
`--props` for the cocoon), Level 08's production kit (`tools/art/backdrop_l08.py`, imported as a
library) re-dressed for the Nova Lagos arcology district in the last hour of the night: the
pre-dawn grade (the navy a little less saturated, the darks lifted toward a cool grey, more in the
east), the sky's reflection greying in the east of the lagoon, banks lit grey from the east,
fewer lit windows. Five ground tile sets (the ruined elevated highway, the plaza district, the
boulevard with tram tracks, the lagoon with the Okonjo Bridge, the arcology district), the targets'
structures from the ground targets (Unity Plaza with its dry fountain and the Unity column, the
mortar roofs, the roof nest, the cocoon's roof, the quay pads, the bridgehead, the Ndidi lobby
plaza), the CDF checkpoint, a fallen highway span, the burnt-out tram, abandoned aircars (Level 08's
production sedan and van, parked dark), a capsized ferry, burnt blocks with fires and smoke plumes,
perspective towers (city, creep-veined, burnt), two smaller arcologies, the Ndidi Arcology hollowed
by creep, a provisional rubble piece, and the Kilo convoy's CDF truck at 16 headings (the model in
[tools/concept/props_r31.py](../../../../../tools/concept/props_r31.py), rendered as Level 08's
traffic). The GIF: the convoy down the bridge (t 102–106). Brief: `a pre-rendered late-90s top-down
city backdrop, Lagos arcology district in the hour before dawn: navy night, sodium lamps, a faint
grey glow in the east, creep-veined towers in perspective, a lagoon crossed by a long bridge, a CDF
truck convoy, smoke and dust`.

Outcome (user, 2026-10-07): approved as **final** in round 31, the weak spots as they are.

## backdrop-proposal.yaml

Not an image: the backdrop block (pieces and placements in script time) and the sections' `tiles`
proposed for Level 09's data.yaml, written by `python3 tools/art/backdrop_l09.py --proposal` from
the ground targets it lists (the README's estimates until the level data exists). The level-data
step merges it and moves the targets; a re-run then follows the data.

## collapse-r31-a

Concept, round 31, variant A "topples across": the Ndidi Arcology tips over its right foot edge
and falls across the play field, drawn with the tower projection (a box of 200 × 180 × h 1.5
rotated about its hinge, Level 09's production roof and wall texture on its faces); its tip's
ground x runs with the kill band (fall angle asin(s) over 3 s); a 1.5 s warning before (the tower
shudders, its shadow sweeps across the band's footprint); at impact a dust burst along the band,
the heavy dust peak ramping out over 6 s and the crushed body left lying across. Generator:
[tools/concept/collapse_r31.py](../../../../../tools/concept/collapse_r31.py) (`a`). Renderer needs:
TowerProjection for a tilted box (8 corners rotated about the hinge, visible faces as textured
quads), the rubble piece at impact, a dust-burst sheet. Brief: `a pre-rendered top-down shot of a
300 m arcology tower toppling across a city plaza before dawn, perspective from above, its creep-
covered roof swinging over, a dust burst along the impact line`.

Outcome (user, 2026-10-07): **rejected** in round 31, as was b, moved to `rejected/` (the heading keeps the file's name): "no separate shadow sweeping right", "buildings don't fall sideways as a whole"; a rework asked for, variant c.

## collapse-r31-b

Concept, round 31, variant B "pancakes into a dust wave": the floors give way and the tower sinks
into itself in 1.2 s (the projection with a shrinking height, the wall's storeys disappearing from
the top), a dust cloud boils out of its foot and a dust wave rolls left to right along the band in
3 s (its front is the kill band), leaving a heap where it stood and debris along the band; the same
warning and dust peak as A. Generator: [tools/concept/collapse_r31.py](../../../../../tools/concept/collapse_r31.py)
(`b`). Renderer needs: TowerProjection with an animated height (wall rows cropped from the top), a
foot-cloud sheet, a dust-wave sheet moved with the band's edge, a heap piece and a debris strip
piece. Brief: `a pre-rendered top-down shot of an arcology tower collapsing straight down into its
own dust cloud, a pyroclastic-like dust wave rolling across the plaza before dawn`.

Outcome (user, 2026-10-07): **rejected** in round 31, as was a, moved to `rejected/` (the heading keeps the file's name): "no sharp dust line", the shadow should move with the building; a rework asked for that combines a and b (a little sway, then straight down, dust in all directions from just before the impact, a large dust explosion at it), variant c.

## collapse-r31-c

Concept, round 31, variant C "leans, then collapses into a dust blast", made after the user's
verdict on A and B (2026-10-07: the tower sways a little, then collapses straight down; its shadow
moves with it; dust in every direction from just before the impact, a big dust explosion at it;
no sharp dust edge) and reworked after the verdict on its first render (2026-10-07: "C is much
better. Reduce the shadow size to about 50% of what it is right now. Also, do not make the building
sway back and forth. Let it lean a bit to the right and then drop in one go. The dust is great.
Massive improvement."). Generator: [tools/concept/collapse_r31.py](../../../../../tools/concept/collapse_r31.py)
(`c`), over Level 09's production backdrop at the hold's 30 px/s, 1×; the sheet shows twelve key
frames with their times and, as a debug overlay only on the sheet, the kill band and the kill ring;
the GIF plays the whole 10.2 s at 10 fps without an overlay. Times below are from the warning's
start (the sheet's and GIF's clip starts 0.8 s earlier).

The look: the Ndidi Arcology leans slowly to the right, one direction only, from upright to 4°
over the warning (slow at first, then faster), bending from its foot rather than tipping as a
whole, with a barely visible shudder; debris and small dust wisps trickle off its walls; its cast
shadow (half the kit's length) stays on it and follows the lean. It then drops straight down into
itself in one go in 1.5 s, the roof sinking and greying with dust, the top storeys of the walls
crushed, the lean kept and relaxing a little, its shadow shrinking with the height toward the
foot; chunks spit out of the crushing floors. 0.35 s before the roof reaches the ground dust boils out of the base in every
direction; at the impact a big radial dust blast rolls out (a lobed, ragged front of soft lit puffs,
no straight edge) and a billow rises over the foot; the dust thins and settles over 6 s and leaves
a dust-coated rubble heap.

What the renderer needs:
- **Timings** (from the warning's start): lean 0–1.5 s (the gameplay warning); drop 1.5–3.0 s; base
  dust from 2.65 s; impact at 3.0 s; the blast's front 1 s (3.0–4.0 s; the kills ride it); the dust
  thinned out by 9.0 s; the level's `heavy` peak in from 2.65 s, full at 3.3 s, out over 6 s
  (smoothstep).
- **Lean** (TowerProjection, no new frames): a per-band x offset growing over the warning, in a
  single direction (to the right, +x): lean(w) = 4°·w^1.6, w = t ÷ 1.5 (0.3° at 0.3 s, 1.8° at
  0.9 s, 4° at 1.5 s); a point z up the tower is moved right by H·sin(lean)·(z ÷ H)² (H = the
  tower's full height; at this concept's scale 400 px, so the roof's offset grows 0 → 28 px before
  the projection): the roof drawn at its usual k with that x offset, each wall band's top and foot
  edges at the offset of their height (the wall bands already exist). Shudder (small, it must not
  read as a sway): ±(0.3 → 0.8) px at the roof in x, half in y, a new random value every 1/20 s,
  scaled by z ÷ H; through the drop 0.8 → 0 px. Draw the wall that faces the
  projection centre: for this tower, left of centre, that is its east wall between the roof's right
  edge and the foot's.
- **Drop**: height h(p) = H·(1 − (0.25p + 0.75p²)), p = drop time ÷ 1.5 (slow start, fast end), to 0
  at the impact; the roof drawn at k(h) = 6 ÷ (6 − h) at the lean's offset (no new motion: the lean
  of the warning's end kept, relaxing linearly to 0.65 × 4° at the impact); the walls from the foot up to h with the wall texture's rows kept from the foot (the
  rows above h cropped); the top 28 px of the walls (scaled like H) darkened and dusty in a noisy
  ragged band (the crushed floors); the roof tinted toward dust grey (118, 112, 110) by up to
  0.5·p^1.5. The tower is hidden at the impact (as `hide` does now).
- **Shadow**: the kit's rule (`drop_shadow`, key light (−0.55, 0.6, 0.75)) at half its length
  (shadow factor 0.5, the user's 2026-10-07 "about 50 %"): a point z up is shadowed 0.5·z ÷ 0.921
  px along (0.676, −0.737), right and down (y up). The shadow is the footprint swept up the bent
  tower: the convex hull of the footprint and the roof outline shifted by (offset(h) + 0.367·h,
  −0.400·h) is close enough. That is 217 px long while the tower stands (434 px before the
  rework); it shrinks with the height to nothing at the impact, receding toward the foot. Draw it as one dark polygon over the
  ground, under the towers, 40 % dark with a few px of soft edge. Production towers cast no shadows
  today; this is the only one (it can be left out if it looks odd next to them).
- **Puff sprites**: 8 dust puffs, 128×128 RGBA (4 billowy, clusters of small balls; 4 hazier), lit
  from the top left (pale grey (158, 151, 146) to navy grey (62, 63, 80)), soft noisy edges; drawn
  scaled 0.08–2.4× (also by k(z) when lifted), alpha per puff peak·min(1, t ÷ fade-in)·(1 − t ÷
  life)^1.25, never rotated. Particles (positions on the ground layer, so they scroll with it;
  seeded):
  - trickle: 46 debris specks (2×2 px), from 35–98 % up the north, east and south walls between 0.15
    and 2.85 s, falling under g = 520 px/s² (400 px in about 1.25 s) and drifting 8–30 px/s outward;
    each lands with a small puff (0.08–0.14× growing 2.4×, alpha 0.4, 1.6 s);
  - wisps: 16 small puffs off the walls during the lean (0.08–0.15×, alpha 0.35, 1.4 s);
  - drop: 60 specks spat out of the crushing top at 40–130 px/s;
  - base dust: 44 puffs from 2.65 s, starting 60–100 % of the way from the foot's centre to its edge,
    moving out 30–150 px (1 − e^(−t/0.7)), 0.25–0.4× growing to 0.8–1.2×, alpha 0.55–0.75, life 3–4.5 s,
    drawn under the walls;
  - blast at the impact: 130 front puffs and 70 behind them, every direction from the footprint's
    edge, reaching 0.62–1.05 (front) or 0.2–0.6 (fill) × 290 px × the lobes 1 + 0.16·sin 3θ + 0.1·sin
    5θ + 0.06·sin 9θ, eased 1 − (1 − t)² over 1 s, then drifting 20–70 px more (1 − e^(−t/1.6)), each
    0–0.22 s late, 0.35–0.6× growing to 0.9–1.7× over 2.4 s, lifted 10 → 40 px (projected), alpha
    0.5–0.7 (fill 0.35–0.5), life 2.6–4.6 s; the billow: 26 puffs over the footprint from 0.1 s before
    the impact, rising 20 → 60–150 px over 4 s (projected, so spreading outward), 0.8–1.1× growing to
    1.7–2.4×, alpha 0.55–0.7, life 3.6–5.8 s; 40 specks thrown out at 150–330 px/s. The blast is
    these particles, not a baked sheet (a sheet would have to scroll with the ground and would repeat
    its edge).
- **Rubble heap**: a new ground piece of 280×250 (it replaces the provisional `arcology-rubble`
  480×220), centred on the foot: a low mound of broken slabs, facade panels with window grids, creep
  chitin, rebar and embers, coated with settled dust, its shadow down and right; it fades in over
  0.2 s at the impact, under the dust.
- **Kills** (sim; on the sheet only as an overlay): the band as now; a ring from the foot's centre,
  radius 100 → 390 px (the band's far corner) over 1 s from the impact, eased 1 − (1 − t)², the same
  as the blast's front: a unit in the band dies when the ring reaches it.

Brief: `a pre-rendered top-down shot of a 300 m arcology tower before dawn, leaning slowly to one
side, then collapsing straight down into itself in one go, a huge billowing dust cloud bursting out in every direction
from its base, settling over the rubble`.

Outcome (user, 2026-10-07): **chosen** in round 31 (row 4), approved with two tweaks (the shadow
halved; a single lean to the right instead of a sway, both in the file as re-rendered) and built in
the game ([collapse-capture-final-r31-a](#collapse-capture-final-r31-a)); hold C lasts until the
dust settles (9.0 s after the warning starts), so the heap stays in sight after a late kill.

## collapse-capture-final-r31-a

Round 31's collapse look c as built in the game (M5 part C, 2026-10-07), a capture, not generated
art (`.png` sheet, `.mp4` with the game's sound), from the uncommitted working tree, the atlases
packed from the current `assets/` by `:desktop:installDist` (an isolated build directory): the lean
and the drop drawn by the tower projection (`TowerProjection.bend`), the cast shadow, the puff
sprites and debris specks of `CollapseLooks` (`tools/art/collapse_l09.py`'s `collapse-puff_0..7`),
the `arcology-heap`, the heavy dust peak and the re-cut collapse sound (its crash at the impact).
The same run as [level-09-capture-final-r31-a](#level-09-capture-final-r31-a): `terran-vanguard
--bench 212 --settings <file> --level 9 --invulnerable --escort rook:mortar:2 --loadout
front=pulse-cannon:3,left=bomb-rack:2,right=bomb-rack:2 --special airstrike:2` at medium on a
private Xvfb display (960×540, `controls.auto-fire=true`), the ship flown by the same XTest key plan
(onto Node C1 and C2 in hold C); video `ffmpeg -f x11grab -copyts`, sound OpenAL Soft's `wave`
driver, aligned by the file's end (it starts 4.29 s before t = 0; the crash's onset in the mix,
t 190.7, lands on the impact seen on screen). Node C2 died at t 187.6, so the warning (W) started at
about t 187.7, the drop at W + 1.5 and the impact at W + 3.0 (t 190.7); hold C ended with the
blast at W + 4.0. The `.mp4` is t 185.5–201.0 (15.5 s), the whole screen with the HUD, the sound
3 dB down so the AAC encode does not clip (H.264 CRF 20, AAC 160 kb/s). The sheet: ten play-field
frames at 1× from W − 0.5 (C2 dying) to W + 9.5: the lean with its cast shadow, 4° at W + 1.45, the
drop (the roof smaller and lower), the base dust, the blast at the impact, rolling out over the
bottom of the screen, the blast's end, the dust settling.

What it shows: the C nodes died late (the tower's foot about 60 px above the bottom edge at the
impact), so the blast rolls out mostly over the bottom of the screen and below it; once the hold ends
at W + 4.0 the scroll eases back to 150 px/s and the foot leaves the screen in about a second, under
the still-thick dust, so the rubble heap is not seen in this run (the concept kept the hold's
30 px/s through the 6 s settle). A second run with the Airstrike fired at t 180.3 did not kill the
nodes sooner. The towers' walls as fixed the same day (the centre-facing east or west wall) show in
the other frames of the run.

Outcome (user, 2026-10-07): accepted in round 31 with look c; after it, hold C lasts until the
dust has settled (the heap no longer scrolls away under the dust after a late kill).

## level-09-capture-final-r31-a

Round 31, a capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound),
taken in M5 part C from the uncommitted working tree of 2026-10-07, the atlases packed from the
current `assets/` by `:desktop:installDist` (an isolated build directory). `terran-vanguard --bench
250 --settings <file> --level 9 --invulnerable --escort rook:mortar:2 --loadout
front=pulse-cannon:3,left=bomb-rack:2,right=bomb-rack:2 --special airstrike:2` at medium (the
balance plan's L09 fit: Bomb Racks and Rook's Mortar; the Airstrike was never fired) on a private
Xvfb display (960×540; settings: a 960×540 window at 0,0, `controls.auto-fire=true`). Video:
`ffmpeg -f x11grab -draw_mouse 0 -framerate 30 -copyts` (wall-clock timestamps); sound: OpenAL
Soft's `wave` driver (`ALSOFT_CONF` with `drivers = wave`) writing the game's mix to a file, nothing
played aloud, aligned by the file's end (it starts 4.08 s before t = 0) and checked against the
"Firestorm" stems (the music found at t 0.08 by a matched filter); the mix peaks at 1.000, none over.
Level time t = 0 at the game's `[display] window` log line (the bench clock). `--invulnerable` also
keeps a node that leaves the screen from failing the level (it only marks the node lost on the
tracker), so a first run without input served as a reconnaissance: Rook's mortar alone killed A1,
A2, B1 and B2, the C nodes left the screen alive and the arcology did not fall. The capture run
flies the ship by XTest key events on that display (a small Python driver, dead reckoning at
270 px/s, a wall reset at t 172): it sits at its start (Rook kills hold A's and hold B's nodes, the
bridge Ravagers die in the pulse cannon's lane), meets the creep cocoon at t 94.3–98.6 and follows
it down under the bomb racks (open at t 97.2, crate collected at t 97.6), and in hold C flies onto
Node C1 at t 180.6 and Node C2 at t 184.0. The run: hold A t 38.5–49.5 (A1 dies t 42.4, A2 t 49.5,
both by Rook's mortar), hold B about t 129.5–139 (B1 dies about t 136, B2 about t 139, Rook's
mortar), hold C from t 180 (C1 dies t 183.0, C2 t 187.5),
the collapse warning t 187.6, the fall t 189.1–192.1 (the placeholder dust-wall front, `NeutralFall`,
until round 31's pick), hold C ends with it; 180 / 253 kills, secret 1 / 1 (+160), secondary met
(+101), A, 1,781 credits. The video is the whole run from t −1 to the debrief (233 s). The sheet's
panels: hold A with the mortar's shell on A1, A1's iris open, A1's death, a Ravager pounce mid-air,
the cocoon under the bombs, the crate, the bridge with the Kilo trucks, the collapse warning, the
fall, the dust and rubble, the dust still at its peak at t 205, the debrief.

Outcome (user, 2026-10-07): accepted in round 31 as taken (the fixes after it as built).

## level-09-readability-r31-a

Round 31, a capture of the game, not generated art: 128 px crops of
[level-09-capture-final-r31-a](#level-09-capture-final-r31-a)'s run at 1:1 and at 2× (nearest
neighbour) at the game's 960×540: Node A1 with its iris open (t 41.6), a galloping Ravager (t 71.4),
a Ravager in its leap with its shadow (t 71.7), the creep cocoon on its roof (t 95.0) and a Kilo
truck on the Okonjo Bridge (t 110.0).

Outcome (user, 2026-10-07): accepted in round 31.
