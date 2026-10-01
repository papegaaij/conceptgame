---
title: Art direction
design: approved
implementation: n/a
art: chosen
updated: 2026-10-01
---

# Art direction

## Summary

The game looks like a late-90s shooter built from **pre-rendered CGI sprites**: 3D models
rendered to crisp, palette-limited 2D sprites with metallic highlights, as in Raptor: Call of the
Shadows and Tyrian 2000. This document fixes the screen geometry, sprite sizes, animation rules,
palette approach, the **parallax layer model** and the readability rules that every art asset
and every level must follow.

## Design

### The pre-rendered look

- **Modelling.** Hard-surface models for the UTC and the Ascendancy, organic sculpted models for
  the Vrell. Silhouettes first: every sprite must be recognisable as a solid shape at native
  size. Detail comes from panel lines, greebles and material contrast, not from texture noise.
- **Camera.** Orthographic, looking straight down (top-down). No perspective on sprites. Tall
  ground structures are the exception, see *Perspective on the ground layer* below.
- **Lighting.** One warm **point key light at the top-left, above the model** (not a sun: a point
  light gives the characteristic gradient across flat hull plates), a weak cool fill from the
  bottom-right, soft ambient with ambient occlusion. Shadows fall **down-right**.
- **Materials.** Metals get a glossy Blinn-Phong highlight and a classic 90s chrome
  environment reflection (bright sky, dark horizon band, warm ground). Glass is dark and very
  glossy. Engines, cockpit lights and Vrell organs are emissive.
- **Render pipeline** (production and concept tools follow the same steps):
  1. Render at 4–8× native size with alpha.
  2. Box-filter down to native size (colour weighted by coverage).
  3. Hard 1-bit alpha at 50 % coverage (sprite colour key look, no soft edges).
  4. Mild unsharp mask for crispness.
  5. Median-cut palette: 24 colours for small sprites, 32–48 for large ones; no dithering on
     sprites (ordered dithering allowed on large gradients in backgrounds).
  6. Shadows are generated at runtime from the sprite alpha, never baked into the sprite.

  The concept generators in [tools/concept/](../../tools/concept/README.md) implement this with
  an SDF ray-marcher instead of polygon models.

### Resolution and screen geometry

| Item | Value |
|---|---|
| Internal resolution | **960×540** (16:9). Integer scaling with nearest-neighbour where it fits: 2× = 1080p, 4× = 4K. 1440p (2.67×) and 720p (1.33×) default to **letterboxed integer scaling** (crisp, black borders); **sharp-bilinear** (integer pre-scale, then bilinear to fit) is an option; letterbox for non-16:9 displays |
| Play field | **480×540** at x = 240…719 (portrait, 8:9) |
| Side panels | 240×540 each: left x = 0…239, right x = 720…959 (HUD, see `design/ui`) |
| Look-ahead | the player sits in the lower third; enemies entering from the top get about 375 px of warning |

The concept generators take these values from one place: `tools/concept/render/config.py`.

### Sprite sizes (native pixels)

| Class | Layer | Size |
|---|---|---|
| Player ship (AF-12 Stormhawk) | air | 48×48 |
| Wingman (Rook's craft) | air | 40×40 (smaller than the player, who stays dominant) |
| Drones | air | 30–36 |
| Tiny enemy (swarmers, seeds, motes) | air / low-air | 16–24 |
| Small enemy (darts, drones) | air / low-air | 28–48 |
| Medium enemy (gunships, walkers) | air / ground | 56–110 |
| Large enemy (carriers, frigates, mechs, segment chains) | air / ground / sub | 120–250 (a chain's total length; its segments are 24–48) |
| Huge enemy (leviathans, rotating platforms) | high-air / air / space | ≥ 1/3 of the play field (≥ 160 wide or ≥ 180 tall), may exceed it |
| Boss | any | 240–480 wide, multi-part, may exceed the play field |
| Ground turret / emplacement | ground | 24–36 |
| Vehicles, naval craft | ground / sub | 24–72 |
| Station / structure parts | ground / far | trusses 26–34 wide, modules 40–120, solar arrays up to 150 long |
| Enemy bullets | air | 8–13 (never smaller than 8) |
| Player shots | air | 5×16 typical, beams full height |
| Pickups | air | 18–24 |
| Explosions | air / ground | 24–144, additive |

All sizes are 1.5× the round 01 values (640×360). The enemy rows match the **size tiers** in
[enemies](../enemies/README.md#size-tiers).

### Animation rules

- **Banking.** The player ship and the wingman have 5 frames: hard left, left, centre, right,
  hard right (about ±14° and ±28° roll). The concept sheets show 3 (±28°). Frames change over
  about 6 game frames when steering, and return to centre when released.
- **Rotation.** Enemies that turn to face their movement are **pre-rendered at 16 angles**
  (22.5° steps); large or slow enemies and turret barrels use **32 angles** (11.25°). The game
  shows the frame nearest to the current heading; a turn plays through the in-between frames,
  never snapping more than one step per game frame. All rotations are separate renders from the
  3D model with the key light fixed at the top-left, so highlights and shadows stay put while the
  body turns; sprites are never rotated at runtime. Designs for turning enemies avoid a
  lighting-dependent "top": they must read from every angle.
- **Smooth slow rotation** (user feedback on round 06: the Halo Platform's rotation was "very
  jagged"). 16/32 angles only work for small sprites or quick turns. The angle step must move
  the sprite's outer edge by at most ~2 px per frame change: for a part at radius *r* px that is
  roughly 2π·r / 2 frames — a 150 px ring needs about **256 angles** (or a radially symmetric
  design so one symmetry step covers it: a 6-fold ring then needs ~43 unique frames). Large
  slowly rotating structures therefore get dense angle sets or a symmetric design; their
  rotation speed is chosen so a frame change never jumps more than one step.
- **Independent parts.** Turrets, heads and weapon arms on vehicles and walkers are separate
  sprites with their own angle set (e.g. a tank hull at 16 angles, its turret at 32), drawn on a
  pivot point defined per frame of the body.
- **Segment chains** (serpents, centipedes, drone trains): rendered **per segment type, not per
  pose** — head, body segment (one or two variants), tail, each at 16 angles (32 for large
  chains). At runtime every segment picks its angle from the path it is following, so any curve
  or loop comes for free. Segments overlap by 20–30 % of their length; a joint piece is only
  needed when segments would gap in tight turns. Body segments may carry a 2–4 frame ripple
  (legs, fins, glow pulse) offset per segment so the wave travels down the chain.
- **Articulated parts** (a leviathan's tail and fins, claws, ring sections): each part is its own
  sprite with a small angle range (typically ±30° in 8–12 steps) around a pivot, animated
  procedurally (sine sweeps, lagged follow-through). Destroyable parts also have a damaged frame
  and a wreck/stump frame.
- **Walk cycles.** Walkers use 8 frames per cycle for 2 legs (Strider) and 6–8 for 4–6 legs
  (Scuttler, Creeper), rendered at each of their 16 facing angles; centipede legs ripple as a
  2–4 frame segment animation. Feet plant on the ground layer (their speed matches the scroll
  plus their own speed), and heavy walkers leave footprints or dust on the ground layer.
- **Spinners.** Radially symmetric designs (4-, 5-, 6- or 8-fold) need only as many frames as
  one symmetry step: a 6-fold spinner at 32 steps per turn needs 6 unique frames. Spin speed is
  shown by frame rate (4–30 fps), with an optional motion-blur frame set above ~20 fps. The key
  light is fixed, so a spinner's highlight stays top-left while its body turns under it. Spinners
  with a single weak point off-axis use the full angle set instead.
- **Organic motion.** Vrell wing beats and pulsing glows at 8–12 fps; engine flicker 15–20 fps.
- **Water** (user feedback on round 06: the Kraken's and Driftjelly's water rings read as
  plain drawn circles, and the Kraken "just appears"). Wherever something meets the water
  surface:
  - **Waterline:** an irregular, animated foam collar where a body or limb breaks the surface —
    broken, uneven white/cyan foam that wobbles and sheds bits, never a clean outline.
  - **Ripples and wakes:** expanding ripple *trains* (several uneven rings that distort,
    break up and fade, with light/dark wave bands), splashes with spray particles, V-wakes
    behind moving parts. No single-pixel geometric circles.
  - **Below the surface:** submerged parts stay visible through the water — tinted towards the
    water colour, darker, blurred and slightly wavy — and connect continuously to the parts
    above it (an arm that dives in can be followed under water to the body).
  - **Surfacing / diving:** the highest parts break the surface first, then the rest follows
    from top to bottom (and the reverse when diving), with a swell and foam ring growing as it
    rises and water streaming off afterwards. Nothing pops in.
  - **Under water** scenes (Europa) must read as under water at a glance: swaying vegetation
    (kelp, sea grass, anemones), rising air-bubble streams, drifting particles, caustic light
    patterns and light shafts, and fish.

- **Explosions.** Pre-rendered volumetric fireball sequences of 12–16 frames (additive), plus
  debris chunks and a shockwave ring for large kills. A 1–2 frame white **hit flash** on every
  damaged enemy.
- **Destruction states.** Ground structures and bosses get a damaged and a wrecked frame; wrecks
  stay on the ground layer.

### Palette approach

- Colours are organised as **6-step ramps** (dark background → highlight) per faction, per
  setting, for the UI and for bullets. The reference palette is **B "90s Neon CGI"**
  ([concept/palette-r01-b.png](concept/palette-r01-b.png)): saturated, glossy, violet-shadowed
  chrome. Its hex ramps also live in `tools/concept/render/palette.py`, which all generators use.
  Key ramps: UTC hull `121632 2A3068 4E5AA0 8A96D0 C8D0F4 FFFFFF`, UTC accents
  `0050FF 00A8FF 7FF0FF FF2A6A FF7A2A FFE04A`, enemy shots `300030 C000C0 FF40FF FFC0FF FFFFFF
  FFFF40`, player shots `002060 0060FF 00C0FF A0FFFF FFFFFF 40FF80`.
- Sprites are limited to 24–48 colours each, but there is no global 256-colour limit; the
  "limited" feel comes from the ramps.
- Because palette B is saturated everywhere, backgrounds must work harder to recede: they use
  the darker half of their setting ramp, are **posterized to 12–32 colours** (like palette-limited
  90s tile art; it also keeps them calm) and are hazed towards the setting colour with depth.
- **Faction visual language**
  - **UTC / CDF** (humanity): clean grey/white hard-surface hulls, blue and orange accents,
    panel lines, blue-white engines.
  - **Vrell** (aliens): grown chitin in violets and teals, no straight lines, wet specular,
    bioluminescent glows (teal and pink) that mark weak points.
  - **Jovian Ascendancy** (traitors): angular faceted hulls in black and gold, red sensor lights.
- **Settings** each get a ramp and a mood: Earth orbit (deep blues), Earth surface (greens,
  city nights in sodium orange), Mars (rust and ochre), Europa under the ice (teal-black
  abyss with caustics), asteroid belt (neutral rock greys), Jovian (banded ochres and storm
  browns), alien space beyond the gate (violets, magenta nebulae). The settings themselves are
  described in [design/world](../world/README.md).

### Parallax layer model

The play field is a stack of layers, drawn back to front. Scroll factors are relative to the
**ground layer**, whose speed is the level's scroll speed. Revised after round 01 (orbit "too
empty, ground scrolls too slowly"; city "too crowded"): a new `far` layer fills the gap between
deep and ground, the near layers are faster so speed is felt, and the ground itself scrolls
faster.

| Layer | Contents | Scroll factor | Enemies | Depth cues |
|---|---|---|---|---|
| `deep` | sky, planet surface far below, star fields, nebulae (may have sub-layers, e.g. Earth 0.12 with haze wisps 0.2) | 0.05–0.2 (default 0.12) | none | strongest haze towards the setting's atmosphere colour, desaturated, darker, low contrast, may be slightly blurred |
| `far` | distant structure seen through gaps in the ground: a sister station, a canyon floor, lower city levels, cloud decks | 0.4–0.7 (default 0.5) | none (background set pieces only) | 40–60 % haze towards the setting colour, smaller scale, posterized |
| `ground` | terrain, sea surface, city, station hulls, asteroid surfaces, capital-ship hulls | **1.0** | stationary: turrets, bunkers, tanks, surface ships, growths | full detail; catches shadows of everything above it |
| `sub` | under water: sea floor and submerged craft seen through the surface | 0.8–0.9 | submarines, mines, sea creatures (surface to attack) | blue-green tint, caustics, reduced contrast |
| `low-air` | low flyers, traffic, drifting wreckage, dust plumes, low clouds and smoke | 1.3–1.5 (default 1.35) | low flyers | slightly larger than ground scale; shadow offset (9, 13) |
| `air` | the **play plane**: player, wingman, most enemies, all bullets, pickups | screen space | most enemies | shadow offset (21, 30) onto the ground layer |
| `high-air` | clouds, smoke, debris and ice streaks in front of the player; rare huge overhead passes (Leviathan, Brood Carrier) | 2.0–2.5 (default 2.2) | rare huge set-piece enemies only | weather and decoration: larger, blurred or drawn as motion streaks, **at most ~40 % opacity** over the play plane, never hides bullets; enemies: fully opaque, scaled per the perspective rule, cast shadows |

**Ground scroll speed** (960×540): calm levels 120–140 px/s (about a quarter screen per second),
normal 150–170 px/s, fast or chase levels 190–240 px/s. The fastest visible layer then moves
2–2.5× that, which is what sells the speed. Round 02 uses 190 (orbit), 140 (city) and
160 px/s (canyon).

**Density** (round 02: all three approaches liked): density follows the level's pace — calm
levels like scene B, normal levels like scene C, fast or chase levels like scene A. Always: one
dominant ground feature that defines clear lanes (a canyon, a station spine, avenues); two or
three mid-size set pieces per screen; background detail kept calm by posterized textures and
sparse light points; in a typical frame outside boss fights about 4–6 enemies and at most ~15
enemy bullets.

**Decoration** (user feedback on round 02: "the levels could use a bit more decoration"):
every scene gets a clearly visible **atmosphere layer** and, where the setting allows,
**organic detail**.
- Atmosphere: heavier cloud banks, mist or dust streaks. Dense, opaque banks go on `low-air`
  (below the play plane, so they never hide bullets); thinner wisps and streaks on `high-air`
  keep the ~40 % opacity cap. Per setting: clouds and haze (Earth, Earth orbit, Jupiter), dust
  storms and streaks (Mars, Luna regolith plumes), mist and fog (megacity, arctic), silt and
  bubbles (Europa), nebula wisps (space, Vrell space).
- Organic detail: vegetation on ground layers — parks, tree lines, fields and jungle on Earth,
  greenhouse domes and lichen fields around Mars colonies, kelp and coral-like growth under
  Europa's ice, Vrell biomass (creep, spore fields) on infested areas.
- Decoration never competes with gameplay: it stays lower in contrast and saturation than
  enemies and bullets.
- **Motion budget** (user feedback on round 09: the storm and, less so, the ocean were "heavy on
  the eyes — too much is moving at the same time"). At most **two** strongly animated
  background elements at once (e.g. waves and rain — not waves, rain, scud and background all at
  full strength); everything else moves slowly or not at all. Repetitive fine motion (rain,
  wave crests, wind streaks) stays low in contrast and amplitude, and motion must be smooth —
  no element may jump by more than ~2 px between frames. Intensity peaks (storm, lightning)
  are short, as with the atmosphere-intensity rule.
- **Atmosphere intensity varies through a level** (user feedback on round 03): the round-03
  scenes show the *heavy* end and must not be used like that for a whole level. Each level
  section gets an intensity — `clear` (no banks, a few wisps), `light` (~10–15 % bank
  coverage), `medium` (~20–25 %) or `heavy` (25–40 %, the round-03 look). Most of a level is
  clear to medium; heavy is a short peak (a storm front, a fog bank before an ambush), and
  transitions ramp over several seconds rather than switching. Vegetation and other organic
  detail follow the terrain, not this curve.
- Rules learned in round 03:
  - **Coverage**: low-air banks cover roughly 25–40 % of the screen and leave the lanes of the
    level (avenues, canyon, spine gaps) readable most of the time.
  - **Tones**: mix the setting ramp towards a neutral (grey for clouds and fog, ochre for Mars
    dust) and keep the brightest bank tone clearly below the player's hull in value. Light
    tints must not land in a reserved hue: sodium orange over violet fog turns pink, so fog
    uses a greyed base with an amber glow.
  - **Shape**: clouds are billowy (lighting from a blurred density, lit towards the top-left,
    shadowed bottom-right); fog and dust banks are stretched 2–4× along the wind so they read
    as rolling banks rather than blobs.
  - **Depth**: a bank shadows the ground below it (offset (10, 14), 25–40 %), and flyers cast a
    second shadow onto bank tops at a smaller offset (12, 17) as well as onto the ground.
  - **Limited colour**: banks are posterized to about 12 colours and their alpha stepped to about
    6 levels, like 90s translucency tables; thin veils and wisps need finer steps (about 10) or
    they break into hard-edged patches. This also keeps scrolling layers cheap.
  - **Vegetation**: low-value greens (night parks and trees at about 55–60 % brightness;
    Mars lichen as a dark olive with ragged, speckled edges); trees are small pre-rendered
    canopy sprites; tree rows stop at crossings.
  - When a scene is reduced to a small palette (e.g. a GIF preview), reserve the bullet, shot
    and Vrell glow colours and give key sprites a share of the palette.

- In **space** levels there is no terrain: stations, asteroids and capital ships take the ground
  role at 1.0, and the deep layer becomes several star-field and nebula layers.
- In **under-water** levels (Europa) the sea floor is the ground layer and submerged enemies live
  in `sub`; what can hit them is a gameplay rule (see Open questions).
- **Shadows**: every flyer casts its silhouette down-right onto the ground layer (opacity
  45–55 %, 1–1.5 px blur, 85 % scale); in space only onto structures. Tall parts of the ground
  layer (modules on a station) also cast a short shadow (6, 8) onto the parts below them.
- **Perspective between layers**: anything that connects two layers is drawn in true
  perspective around the screen centre, where a layer with scroll factor *k* is also drawn at
  scale *k*. Tall structures (towers, spires, masts) push their roofs away from the centre
  (camera height model, see parallax B); canyon walls and cliffs run from the rim at scale 1.0
  down to the floor at the `far` scale (0.6 in parallax C). As they scroll, walls turn, which
  gives real depth. Everything else is orthographic.
- **Kit-bashed structures**: stations, colonies and similar structures are assembled from a kit
  of pre-rendered parts (trusses, modules, arrays, domes) rather than rendered as one image, as
  90s tile sets were. The concept tools do this with `tools/concept/render/station.py`.

### Readability rules

1. **Enemy bullets pop on every background**: bright white core, saturated ring, 1 px dark rim.
   Their hues (magenta/pink, orange) are reserved: no background uses them at that saturation.
   Minimum 8 px. Each bullet type has its own shape (orb, needle, ring, beam). In palette B
   the orbs are magenta `FF40FF` and the needles yellow `FFFF40`, both on a `300030` rim.
2. **Player shots** are blue / white / cyan and may be semi-transparent; they never share a hue
   with enemy bullets.
3. **Backgrounds recede**: they are darker (about 70–80 % brightness) and less saturated than
   the sprites above them. The concept tools do this with `terrain.recede()`.
4. **Enemies carry a bright accent** (Vrell glow, UTC stripe, Ascendancy red light) so that a
   dark enemy over a dark background is still found at a glance.
5. **High-air weather and decoration** stay below ~40 % opacity where they overlap the play
   plane. Enemies on `high-air` (the Leviathan's overhead pass, the Brood Carrier's pass) are
   the exception: fully opaque, scaled per the perspective rule, and they cast shadows.
6. **Pickups** pulse and have a light outline. The player's hit box is much smaller than the
   sprite (about 6×6 px around the cockpit), which is a gameplay rule in the player docs.

## Concept art

Concept round 01 (prompts: [concept/prompts.md](concept/prompts.md)):

| File | What | Status |
|---|---|---|
| [concept/rejected/palette-r01-a.png](concept/rejected/palette-r01-a.png) | Palette A "Cold Military Steel": ramps per faction, UI, bullets and setting, with ship and Vrell previews | rejected — B chosen |
| [concept/palette-r01-b.png](concept/palette-r01-b.png) | Palette B "90s Neon CGI" | chosen |
| [concept/rejected/palette-r01-c.png](concept/rejected/palette-r01-c.png) | Palette C "Warm Cinematic" | rejected — B chosen |
| [concept/rejected/parallax-r01-a.png](concept/rejected/parallax-r01-a.png) | Parallax scene A "Earth orbit": frame at 2× plus layer breakdown (deep Earth, station, play plane, debris) | rejected — too empty, ground scrolls too slowly |
| [concept/rejected/parallax-r01-a.gif](concept/rejected/parallax-r01-a.gif) | Parallax scene A: 4 s seamless scroll loop at native 320×360 (view at 2× with pixelated scaling) | rejected — too empty, ground scrolls too slowly |
| [concept/rejected/parallax-r01-b.png](concept/rejected/parallax-r01-b.png) | Parallax scene B "Earth megacity at night": frame at 2× plus layer breakdown (city with perspective towers, traffic, play plane, clouds) | rejected — too crowded and busy |
| [concept/rejected/parallax-r01-b.gif](concept/rejected/parallax-r01-b.gif) | Parallax scene B: 4 s seamless scroll loop at native 320×360 | rejected — too crowded and busy |

Round 01 also covered the [player ship](../player/ship/README.md), the
[HUD](../ui/hud/README.md) and the [title logo](../ui/main-menu/README.md); outcomes are
recorded in those documents.

Concept round 02, at 960×540 in palette B with ship A (prompts:
[concept/prompts.md](concept/prompts.md); generator `tools/concept/parallax_r02.py`). Each GIF
is a seamless 4 s loop (80 frames at 20 fps) at native 480×540:

| File | What | Status |
|---|---|---|
| [concept/parallax-r02-a.png](concept/parallax-r02-a.png) | Parallax A "Earth orbit, fuller and faster": play field at 1× plus a breakdown of 6 layers (Earth, sister station, main station kit-bashed from parts, wreckage, play plane, streaks); ground 190 px/s | chosen — approach; needs more decoration (round 03) |
| [concept/parallax-r02-a.gif](concept/parallax-r02-a.gif) | Parallax A: scroll loop | chosen — approach; needs more decoration (round 03) |
| [concept/parallax-r02-b.png](concept/parallax-r02-b.png) | Parallax B "Night megacity, calm": lower-contrast towers, two avenues as lanes, parks, sparse lights, 3 darts and one turret; ground 140 px/s | chosen — approach; needs more decoration (round 03) |
| [concept/parallax-r02-b.gif](concept/parallax-r02-b.gif) | Parallax B: scroll loop | chosen — approach; needs more decoration (round 03) |
| [concept/parallax-r02-c.png](concept/parallax-r02-c.png) | Parallax C "Mars canyon, balanced": canyon floor as a far layer with perspective strata walls, plateau with a colony outpost and Vrell pods, dust plumes and streaks; ground 160 px/s | chosen — approach; needs more decoration (round 03) |
| [concept/parallax-r02-c.gif](concept/parallax-r02-c.gif) | Parallax C: scroll loop | chosen — approach; needs more decoration (round 03) |

Concept round 03, decoration pass on the round 02 scenes (prompts:
[concept/prompts.md](concept/prompts.md); generator `tools/concept/parallax_r03.py`, which
subclasses the round 02 scenes). The GIF loops use reduced palettes (72–128 colours) to stay
under ~8 MB; the PNG sheets show full colour:

| File | What | Status |
|---|---|---|
| [concept/parallax-r03-a.png](concept/parallax-r03-a.png) | Parallax A + decoration: a cyclone and cloud fronts on the deep Earth, pale cloud decks on low-air drifting between the station and the play plane (shadowing the station, catching flyer shadows), haze wisps and ice streaks on high-air | chosen — shows the heavy end of the atmosphere range |
| [concept/parallax-r03-a.gif](concept/parallax-r03-a.gif) | Parallax A + decoration: scroll loop | chosen — shows the heavy end of the atmosphere range |
| [concept/parallax-r03-b.png](concept/parallax-r03-b.png) | Parallax B + decoration: tree-lined avenues, more and richer parks (paths, ponds), rooftop gardens, rolling fog banks on low-air lit amber by the avenue lamps; still calm | chosen — shows the heavy end of the atmosphere range |
| [concept/parallax-r03-b.gif](concept/parallax-r03-b.gif) | Parallax B + decoration: scroll loop | chosen — shows the heavy end of the atmosphere range |
| [concept/parallax-r03-c.png](concept/parallax-r03-c.png) | Parallax C + decoration: ochre dust-storm banks on low-air, dust veil and heavier streaks on high-air, greenhouse tunnels, algae ponds and lichen fields around the colony | chosen — shows the heavy end of the atmosphere range |
| [concept/parallax-r03-c.gif](concept/parallax-r03-c.gif) | Parallax C + decoration: scroll loop | chosen — shows the heavy end of the atmosphere range |

Concept [round 06](../concept-rounds/round-06/README.md) — five more settings at `medium` atmosphere intensity; generator `tools/concept/scenes_r06.py`. The GIFs posterize the base terrain to stay under 8 MB; the PNG sheets show full colour.

| File | What | Status |
|---|---|---|
| [concept/scene-luna-r06-a.png](concept/scene-luna-r06-a.png) | Luna — regolith with earthshine-tinted crater shadows, Tranquility Base, mass-driver rail, Vrell nest crater, regolith plumes (sheet + layer breakdown) | chosen |
| [concept/scene-luna-r06-a.gif](concept/scene-luna-r06-a.gif) | Luna: seamless scroll loop | chosen |
| [concept/rejected/scene-europa-r06-a.png](concept/rejected/scene-europa-r06-a.png) | Europa under water — Thera Deep domes and kelp farms on the sea floor, Vrell coral, silt and fish on the sub layer, light shafts, headlight cone (sheet + layer breakdown) | rejected — not clear enough that it is under water |
| [concept/rejected/scene-europa-r06-a.gif](concept/rejected/scene-europa-r06-a.gif) | Europa under water: seamless scroll loop | rejected — not clear enough that it is under water |
| [concept/scene-belt-r06-a.png](concept/scene-belt-r06-a.png) | Asteroid belt — refinery pit and conveyors on a large rock, Helix block with Rail Bunker, tumbling rocks and haulers, ricocheting Buzzsaw (sheet + layer breakdown) | chosen |
| [concept/scene-belt-r06-a.gif](concept/scene-belt-r06-a.gif) | Asteroid belt: seamless scroll loop | chosen |
| [concept/scene-jovian-r06-a.png](concept/scene-jovian-r06-a.png) | Jupiter — Aurelia's Art Deco decks over cloud bands and a storm vortex, anti-grav ring and balloons, lightning flash (sheet + layer breakdown) | chosen |
| [concept/scene-jovian-r06-a.gif](concept/scene-jovian-r06-a.gif) | Jupiter: seamless scroll loop | chosen |
| [concept/scene-vrell-space-r06-a.png](concept/scene-vrell-space-r06-a.png) | Vrell space — violet/teal nebula, hive surface with glowing veins and spawning pits, spore sacs and tendrils, Coilwyrm on a figure-8 (sheet + layer breakdown) | chosen |
| [concept/scene-vrell-space-r06-a.gif](concept/scene-vrell-space-r06-a.gif) | Vrell space: seamless scroll loop | chosen |

Concept [round 07](../concept-rounds/round-07/README.md) — Europa redone to read as under water (new Water rules); generator `tools/concept/scenes_r07.py`.

| File | What | Status |
|---|---|---|
| [concept/scene-europa-r07-a.png](concept/scene-europa-r07-a.png) | Europa A "kelp forest" — swaying kelp stands and sea grass, anemones and fan corals, caustic light on the floor, rising bubble streams, fish schools, marine snow, light shafts (sheet + layer breakdown) | chosen |
| [concept/scene-europa-r07-a.gif](concept/scene-europa-r07-a.gif) | Europa A: seamless scroll loop | chosen |
| [concept/rejected/scene-europa-r07-b.png](concept/rejected/scene-europa-r07-b.png) | Europa B "open water" — brighter sandy floor, wider meadows, fewer kelp stands, stronger light shafts and caustics, larger fish school (sheet) | rejected — A preferred |
| [concept/rejected/scene-europa-r07-b.gif](concept/rejected/scene-europa-r07-b.gif) | Europa B: seamless scroll loop | rejected — A preferred |

Concept [round 08](../concept-rounds/round-08/README.md) — Earth open ocean (L11) under the Water rules; generator `tools/concept/scenes_r08.py` (storm, arctic, Geneva and Luna far side are implemented there but not rendered yet).

| File | What | Status |
|---|---|---|
| [concept/scene-ocean-r08-a.png](concept/scene-ocean-r08-a.png) | Open ocean (L11) — convoy holding station on an overcast sea, wakes and foam in the surface, surfaced and submerged Driftjellies, Reef Spitter rafts, a dark shape below, sea mist (sheet + layer breakdown) | superseded by r09 |
| [concept/scene-ocean-r08-a.gif](concept/scene-ocean-r08-a.gif) | Open ocean: seamless scroll loop | superseded by r09 |

Concept [round 09](../concept-rounds/round-09/README.md) — Acts 1–2 scenes at medium atmosphere (storm peaks heavy), generator `tools/concept/scenes_r09.py`; explosions, generator `tools/concept/vfx_r09.py`.

| File | What | Status |
|---|---|---|
| [concept/scene-ocean-r09-a.png](concept/scene-ocean-r09-a.png) | Open ocean (L11), finished: broken-up wakes, streaky prop-wash, swell-colour depth, darker mist, larger shape under the waves (sheet) | superseded by r10 |
| [concept/scene-ocean-r09-a.gif](concept/scene-ocean-r09-a.gif) | Open ocean (L11), finished: broken-up wakes, streaky prop-wash, swell-colour depth, darker mist, larger shape under the waves (motion) | superseded by r10 |
| [concept/scene-storm-r09-a.png](concept/scene-storm-r09-a.png) | Ocean storm (L12), heavy peak: dark swell, torn whitecaps, wind streaks, rain, scud, a lightning flash (sheet) | superseded by r10 |
| [concept/scene-storm-r09-a.gif](concept/scene-storm-r09-a.gif) | Ocean storm (L12), heavy peak: dark swell, torn whitecaps, wind streaks, rain, scud, a lightning flash (motion) | superseded by r10 |
| [concept/scene-arctic-r09-a.png](concept/scene-arctic-r09-a.png) | Arctic floes (L13): open leads, teal submerged ice, waterline foam, Skimmers with V-wakes, the relay, thin fog (sheet) | chosen |
| [concept/scene-arctic-r09-a.gif](concept/scene-arctic-r09-a.gif) | Arctic floes (L13): open leads, teal submerged ice, waterline foam, Skimmers with V-wakes, the relay, thin fog (motion) | chosen |
| [concept/scene-geneva-r09-a.png](concept/scene-geneva-r09-a.png) | Geneva under the Vrell canopy (L14): old-town blocks, sodium lamps, lake, rotunda, roots and creep, canopy shadow, Hive Node (sheet) | superseded by r10 |
| [concept/scene-geneva-r09-a.gif](concept/scene-geneva-r09-a.gif) | Geneva under the Vrell canopy (L14): old-town blocks, sodium lamps, lake, rotunda, roots and creep, canopy shadow, Hive Node (motion) | superseded by r10 |
| [concept/scene-luna-farside-r09-a.png](concept/scene-luna-farside-r09-a.png) | Luna far side (L06): dark regolith lit by flares, dome lights, Vrell glow and the headlight; Mantis and Coilwyrm (sheet) | chosen |
| [concept/scene-luna-farside-r09-a.gif](concept/scene-luna-farside-r09-a.gif) | Luna far side (L06): dark regolith lit by flares, dome lights, Vrell glow and the headlight; Mantis and Coilwyrm (motion) | chosen |
| [concept/explosions-r09-a.png](concept/explosions-r09-a.png) | Explosions: size ladder 24–144 px, Vrell organic vs Ascendancy metal, water-surface and under-water, hit flash, shield hit and break (sheet) | chosen |
| [concept/explosions-r09-a.gif](concept/explosions-r09-a.gif) | Explosions: size ladder 24–144 px, Vrell organic vs Ascendancy metal, water-surface and under-water, hit flash, shield hit and break (motion) | chosen |

Concept [round 10](../concept-rounds/round-10/README.md) — revisions under the motion-budget rule; generator `tools/concept/scenes_r10.py`.

| File | What | Status |
|---|---|---|
| [concept/scene-geneva-r10-a.png](concept/scene-geneva-r10-a.png) | Geneva (L14) rebuilt as a city from above: street network with lamps and traffic, perimeter blocks of row houses with ridged roofs, lake with the Jet d'Eau, Rhône bridges, Saint-Pierre, the rotunda, parks; Vrell overgrowth layered on top (sheet) | chosen |
| [concept/scene-geneva-r10-a.gif](concept/scene-geneva-r10-a.gif) | Geneva: seamless loop | chosen |
| [concept/scene-storm-r10-a.png](concept/scene-storm-r10-a.png) | Storm (L12) within the motion budget: sea and rain as the only strong motion, slower scroll and swell, faint motion-blurred rain, swaying scud, one lightning flash; 25 fps (sheet) | chosen |
| [concept/scene-storm-r10-a.gif](concept/scene-storm-r10-a.gif) | Storm: seamless loop | chosen |
| [concept/scene-ocean-r10-a.png](concept/scene-ocean-r10-a.png) | Ocean (L11) within the motion budget: sea and wakes as the strong motion, slower swell, lower contrast, fainter whitecaps and wisps; 25 fps (sheet) | chosen |
| [concept/scene-ocean-r10-a.gif](concept/scene-ocean-r10-a.gif) | Ocean: seamless loop | chosen |

## Implementation

- [ ] Renderer draws the screen at 960×540 and scales by integer factors with letterboxing
      (sharp-bilinear for 1440p and 720p).
- [ ] Layer stack (deep, far, ground, sub, low-air, air, high-air) with per-layer scroll
      factors as in the table and a ground scroll speed, configurable per level.
- [ ] Perspective geometry between layers (canyon walls, cliffs) as well as tall structures.
- [ ] Runtime drop shadows from sprite alpha, offset per layer, masked to shadow-catching layers.
- [ ] Perspective roof projection for tall ground structures.
- [ ] High-air weather and decoration opacity capped where it overlaps the play plane; high-air
  enemies drawn opaque, perspective-scaled, with shadows.
- [ ] Bullet sprites follow the readability rules (core, ring, dark rim, reserved hues).
- [ ] Hit flash and explosion sequences as described under Animation rules.
- [ ] Production sprite pipeline (render → downsample → 1-bit alpha → sharpen → palette) is
      scripted so every asset is reproducible, like the concept tools.

## Open questions

- **Perspective towers**: keep the true-perspective roof projection of parallax B, or use purely
  orthographic pre-rendered tiles for all ground structures?

## Decisions

- 2026-09-30: Late-90s pre-rendered CGI sprite look; 16:9 screen with a portrait play field and
  HUD side panels (project kick-off).
- 2026-09-30: Concept round 01: palette **B "90s Neon CGI"** chosen; A and C rejected.
- 2026-09-30: Concept round 01: both parallax scenes rejected — A too empty with the ground scrolling too slowly, B too crowded and busy. Redo in round 02.
- 2026-09-30: Resolution **960×540** chosen (open question resolved): play field 480×540 at
  x = 240, side panels 240×540, player ship 48×48; all sprite sizes scale 1.5×. Palette B is the
  reference palette for all art from round 02 on.
- 2026-09-30: Layer model revised for round 02 (draft, under review with the round 02 parallax
  scenes): new `far` layer, deep 0.12, low-air 1.35, high-air 2.2, shadow offsets scaled to
  960×540, ground speed guideline, density guideline, perspective walls between layers.
- 2026-09-30: Concept round 02: all three parallax approaches liked — density follows level pace (B calm, C normal, A fast). The revised layer model (far layer, faster foreground factors, speed guideline) is adopted with them.
- 2026-09-30: Concept round 02 feedback: scenes need more decoration — heavier clouds / mist / dust streaks and vegetation. Decoration guideline added; the three scenes get a decoration pass in round 03.
- 2026-09-30: Concept round 03: decoration pass chosen for all three scenes; it represents the heavy end. Atmosphere intensity must vary through a level (clear / light / medium / heavy per section, heavy only as a short peak).
- 2026-09-30: Enemy variety (user feedback after round 04): turning enemies are pre-rendered at
  16 angles (32 for large/slow ones and turrets) and the nearest frame is shown; sprite-size
  table extended with tiny and huge tiers; animation rules added for independent parts, segment
  chains (per segment type, not per pose), articulated parts, walk cycles and radially
  symmetric spinners.
- 2026-10-01: Concept round 06: Luna, belt, Jupiter and Vrell-space scenes chosen; Europa rejected (not recognisably under water) — redo with vegetation, bubbles and caustics. New rules: smooth slow rotation (dense angle sets for large rotating structures) and water-surface interaction (waterline foam, ripple trains, visible submerged parts, top-down surfacing).
- 2026-10-01: Menus and other out-of-game screens use the glass-over-scene style of main menu A; the bevelled metal style is reserved for the in-game HUD (user decision, round 06).
- 2026-10-01: Concept round 07: Europa A "kelp forest" chosen; B "open water" rejected.
- 2026-10-01: Scaling: integer scaling with letterboxing by default, sharp-bilinear as an option. Banking: 5 frames for the player ship and wingman. Fitted wing pods are drawn on the ship sprite. Layer hit rules settled in [enemies](../enemies/README.md#layer-rules).
- 2026-10-01: Concept round 08: open-ocean scene deferred — it must be finished first (round 09, with the other Earth scenes).
- 2026-10-01: Concept round 09: arctic and Luna far side scenes and the explosions chosen; Geneva to be redone (not recognisable as a city); storm and ocean to be calmed and smoothed. New rule: motion budget for scene animation.
- 2026-10-01: Concept round 10: Geneva, storm and ocean revisions chosen ("much better now").
- 2026-10-01: High-air drawing rule (user decision): the ~40 % opacity cap applies only to weather and decoration. Enemies on `high-air` (Leviathan overhead pass, Brood Carrier pass) are drawn fully opaque, scaled per the perspective rule, and cast shadows.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
- 2026-10-01: M1 rendering: the play plane draws over the Earth orbit layers (deep 0.12, far 0.45, ground 1.0, low-air 1.4, high-air 2.4 at ≤ 40 % opacity, additive) with the ground at Level 01's 130 px/s. Placeholders are the 0.4× layer breakdown of `parallax-r03-a.png`, enlarged with linear filtering and mirrored every second tile so they scroll without a seam; bolts and explosions are drawn additively; the hit flash is a GLES 2 shader that blends a sprite towards a colour. Positions are interpolated between simulation steps and rounded to whole pixels.
