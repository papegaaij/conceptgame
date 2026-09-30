---
title: Art direction
design: draft
implementation: n/a
art: proposed
updated: 2026-09-30
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
| Internal resolution | **640×360** (16:9), scaled by integers: 2× = 720p, 3× = 1080p, 4× = 1440p, 6× = 4K; nearest-neighbour; letterbox for non-16:9 displays |
| Play field | **320×360** at x = 160…479 (portrait, 8:9) |
| Side panels | 160×360 each: left x = 0…159, right x = 480…639 (HUD, see `design/ui`) |
| Look-ahead | the player sits in the lower third; enemies entering from the top get about 250 px of warning |

### Sprite sizes (native pixels)

| Class | Layer | Size |
|---|---|---|
| Player ship (AF-12 Stormhawk) | air | 32×32 |
| Wingman / drones | air | 20–24 |
| Small enemy (darts, drones) | air / low-air | 20–28 |
| Medium enemy (gunships) | air | 32–56 |
| Large enemy (carriers, frigates) | air / ground | 64–128 |
| Boss | any | 160–320 wide, multi-part, may exceed the play field |
| Ground turret / emplacement | ground | 16–24 |
| Vehicles, naval craft | ground / sub | 16–48 |
| Enemy bullets | air | 5–9 (never smaller than 5) |
| Player shots | air | 3×10 typical, beams full height |
| Pickups | air | 12–16 |
| Explosions | air / ground | 16–96, additive |

### Animation rules

- **Banking.** The player ship and the wingman have 5 frames: hard left, left, centre, right,
  hard right (about ±14° and ±28° roll). The concept sheets show 3 (±28°). Frames change over
  about 6 game frames when steering, and return to centre when released.
- **Rotation.** Enemies that turn to face their movement use 16 directions (22.5° steps); turret
  barrels and large enemies use 32 directions. All rotations are separate renders (lighting stays
  fixed at the top-left), never rotated sprites.
- **Organic motion.** Vrell wing beats and pulsing glows at 8–12 fps; engine flicker 15–20 fps.
- **Explosions.** Pre-rendered volumetric fireball sequences of 12–16 frames (additive), plus
  debris chunks and a shockwave ring for large kills. A 1–2 frame white **hit flash** on every
  damaged enemy.
- **Destruction states.** Ground structures and bosses get a damaged and a wrecked frame; wrecks
  stay on the ground layer.

### Palette approach

- Colours are organised as **6-step ramps** (dark background → highlight) per faction, per
  setting, for the UI and for bullets. Round 01 proposes three complete palettes (see Concept
  art); one will be chosen as the master palette.
- Sprites are limited to 24–48 colours each, but there is no global 256-colour limit; the
  "limited" feel comes from the ramps.
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
**ground layer**, whose speed is the level's scroll speed.

| Layer | Contents | Scroll factor | Enemies | Depth cues |
|---|---|---|---|---|
| `deep` | sky, planet surface far below, star fields, nebulae (may have sub-layers, e.g. stars at 0.05 / 0.1 / 0.2) | 0.05–0.35 (default 0.25) | none | strongest haze towards the setting's atmosphere colour, desaturated, low contrast, may be slightly blurred |
| `ground` | terrain, sea surface, city, station hulls, asteroid surfaces, capital-ship hulls | **1.0** | stationary: turrets, bunkers, tanks, surface ships, growths | full detail; catches shadows of everything above it |
| `sub` | under water: sea floor and submerged craft seen through the surface | 0.8–0.95 | submarines, mines, sea creatures (surface to attack) | blue-green tint, caustics, reduced contrast |
| `low-air` | low flyers, traffic, helicopters, low clouds and smoke | 1.1–1.3 (default 1.25) | low flyers | slightly larger than ground scale; small shadow offset (5, 7) |
| `air` | the **play plane**: player, wingman, most enemies, all bullets, pickups | screen space | most enemies | shadow offset (14, 20) onto the ground layer |
| `high-air` | clouds, smoke, drifting debris in front of the player | 1.5–2.0 (default 1.75) | none | larger, slightly blurred; **at most ~40 % opacity** over the play plane; never hides bullets |

- In **space** levels there is no terrain: stations, asteroids and capital ships take the ground
  role at 1.0, and the deep layer becomes several star-field and nebula layers.
- In **under-water** levels (Europa) the sea floor is the ground layer and submerged enemies live
  in `sub`; what can hit them is a gameplay rule (see Open questions).
- **Shadows**: every flyer casts its silhouette down-right onto the ground layer (opacity
  45–55 %, 1 px blur, 85 % scale); in space only onto structures.
- **Perspective on the ground layer**: tall structures (towers, spires, station masts) are drawn
  with a true perspective offset of their roofs away from the screen centre (camera height
  model, see parallax concept B). As they scroll, their walls turn, which gives real depth.
  Everything else is orthographic.

### Readability rules

1. **Enemy bullets pop on every background**: bright white core, saturated ring, 1 px dark rim.
   Their hues (magenta/pink, orange) are reserved: no background uses them at that saturation.
   Minimum 5 px. Each bullet type has its own shape (orb, needle, ring, beam).
2. **Player shots** are blue / white / cyan and may be semi-transparent; they never share a hue
   with enemy bullets.
3. **Backgrounds recede**: they are darker (about 70–80 % brightness) and less saturated than
   the sprites above them. The concept tools do this with `terrain.recede()`.
4. **Enemies carry a bright accent** (Vrell glow, UTC stripe, Ascendancy red light) so that a
   dark enemy over a dark background is still found at a glance.
5. The **high-air layer** stays below ~40 % opacity where it overlaps the play plane.
6. **Pickups** pulse and have a light outline. The player's hit box is much smaller than the
   sprite (about 6×6 px around the cockpit), which is a gameplay rule in the player docs.

## Concept art

Concept round 01 (prompts: [concept/prompts.md](concept/prompts.md)):

| File | What | Status |
|---|---|---|
| [concept/palette-r01-a.png](concept/palette-r01-a.png) | Palette A "Cold Military Steel": ramps per faction, UI, bullets and setting, with ship and Vrell previews | proposed |
| [concept/palette-r01-b.png](concept/palette-r01-b.png) | Palette B "90s Neon CGI" | proposed |
| [concept/palette-r01-c.png](concept/palette-r01-c.png) | Palette C "Warm Cinematic" | proposed |
| [concept/parallax-r01-a.png](concept/parallax-r01-a.png) | Parallax scene A "Earth orbit": frame at 2× plus layer breakdown (deep Earth, station, play plane, debris) | proposed |
| [concept/parallax-r01-a.gif](concept/parallax-r01-a.gif) | Parallax scene A: 4 s seamless scroll loop at native 320×360 (view at 2× with pixelated scaling) | proposed |
| [concept/parallax-r01-b.png](concept/parallax-r01-b.png) | Parallax scene B "Earth megacity at night": frame at 2× plus layer breakdown (city with perspective towers, traffic, play plane, clouds) | proposed |
| [concept/parallax-r01-b.gif](concept/parallax-r01-b.gif) | Parallax scene B: 4 s seamless scroll loop at native 320×360 | proposed |

The same round also proposes the player ship
([A](../player/ship/concept/player-ship-r01-a.png), [B](../player/ship/concept/player-ship-r01-b.png),
[C](../player/ship/concept/player-ship-r01-c.png)), the HUD
([A](../ui/hud/concept/hud-r01-a.png), [B](../ui/hud/concept/hud-r01-b.png)) and the title logo
([A](../ui/main-menu/concept/logo-r01-a.png), [B](../ui/main-menu/concept/logo-r01-b.png),
[C](../ui/main-menu/concept/logo-r01-c.png), [D](../ui/main-menu/concept/logo-r01-d.png)); those
are listed in their own documents.

## Implementation

- [ ] Renderer draws the play field at 640×360 and scales by integer factors with letterboxing.
- [ ] Layer stack with per-layer scroll factors as in the table, configurable per level.
- [ ] Runtime drop shadows from sprite alpha, offset per layer, masked to shadow-catching layers.
- [ ] Perspective roof projection for tall ground structures.
- [ ] High-air layer opacity capped where it overlaps the play plane.
- [ ] Bullet sprites follow the readability rules (core, ring, dark rim, reserved hues).
- [ ] Hit flash and explosion sequences as described under Animation rules.
- [ ] Production sprite pipeline (render → downsample → 1-bit alpha → sharpen → palette) is
      scripted so every asset is reproducible, like the concept tools.

## Open questions

- **Resolution: 640×360 or 960×540?** 640×360 is the most authentic late-90s look, scales
  cleanly to every common display (2×, 3×, 4×, 6×) and keeps sprite production cheap (a 32 px
  ship), but fine detail is limited. 960×540 gives 1.5× more detail per sprite (a 48 px ship)
  and scales to 1080p (2×) and 4K (4×), but 1440p needs a non-integer 2.67×, sprites cost
  more to produce, and the pixels read less "retro".
- **Master palette**: A (Cold Military Steel), B (90s Neon CGI) or C (Warm Cinematic)?
- **Layer hit rules**: can every weapon hit `ground` and `low-air` targets, or only weapons with
  the `anti-ground` trait (and `anti-sub` for `sub`)? Decided together with the weapon design.
- **Perspective towers**: keep the true-perspective roof projection of parallax B, or use purely
  orthographic pre-rendered tiles for all ground structures?
- **Banking frames**: 5 (proposed) or 3?

## Decisions

- 2026-09-30: Late-90s pre-rendered CGI sprite look; 16:9 screen with a portrait play field and
  HUD side panels (project kick-off).
