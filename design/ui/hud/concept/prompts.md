# HUD – concept prompts

Prompts and briefs for polished versions of the round 01 HUD mockups. The mockups are generated
by [tools/concept/hud.py](../../../../tools/concept/hud.py), which reuses parallax scene A from
[tools/concept/parallax.py](../../../../tools/concept/parallax.py) as the play field.

Shared layout (both variants): 16:9 screen, 640x360 native. Portrait play field 320x360 in the
centre; side panels of 160x360. **Left panel**: score, credits, act/level with progress to the
boss, radio portrait with caller name and subtitle, wingman status. **Right panel**: armour,
shield, generator load (MW used / available), the four weapon slots (front, rear, left wing,
right wing) with upgrade level pips, special ability with charges.

Common negative prompt: `modern flat UI, material design, thin sans-serif, mobile game UI,
photo, watermark, blurry text, cluttered, 3D perspective panels, curved screen`

## hud-r01-a

Variant A, **classic metallic bevelled panels**.

Prompt: `1990s PC shoot-em-up game screen, 16:9, central vertical play field showing a space
station above Earth, left and right side panels made of brushed gunmetal with bevelled edges,
rivets and yellow-black hazard stripes, recessed green LCD readouts for score and credits,
amber text, segmented red armour bar, blue shield bar, yellow generator bar, weapon slot boxes
with level pips, radio video feed with a green-tinted officer portrait and scanlines, crisp
pixel font, late-90s DOS/Windows 95 game UI, pre-rendered metal textures`

Artist notes: think Raptor / Tyrian side panels. Everything is a physical object: plates, wells,
screws. Text uses a chunky bitmap font. Readability first; the play field is never covered.

## hud-r01-b

Variant B, **dark glass neon cockpit**.

Prompt: `late 1990s sci-fi shoot-em-up game screen, 16:9, central vertical play field, side
panels of dark translucent blue glass with chamfered corners and glowing cyan neon outlines,
magenta and amber accents, large white score digits, gradient armour/shield/generator gauges
with tick marks, weapon systems list with chamfered level pips, incoming transmission box with
cyan-tinted pilot portrait and voice waveform, pixel font, subtle scanlines, bloom glow,
Wing Commander / Colony Wars cockpit display aesthetic`

Artist notes: a cockpit display rather than a machine casing. Neon lines glow; labels sit in the
frame gaps. Keep glow on the panels only – nothing bleeds into the play field.

## hud-r02-a

Round 02: the chosen **HUD A** (metallic bevelled panels) at **960×540** with 240 px side
panels, in **palette B**, showing the level 07 state (rear gun and escort slot still empty).
Element list as in the HUD document: mission, score, credits, chain with multiplier and window
bar, radio portrait and subtitle, progress with boss marker; armour and shield bars with
numbers, power gauge (load vs output, spare and regen bonus), four weapon slots with level
pips, overdrive, special charges, escort. Generator:
[tools/concept/hud_r02.py](../../../../tools/concept/hud_r02.py) (play field: parallax scene A
of round 02).

Prompt: `1998 PC shoot-em-up game screen, 16:9, central vertical play field over an orbital
station above Earth, left and right side panels of brushed violet-blue chrome (#4E5AA0 to
#8A96D0) with bevelled edges, rivets and yellow-black hazard stripes, recessed near-black LCD
wells with bright green (#00FF66) and yellow (#FFFF00) pixel text, segmented orange-red armour
bar, cyan shield bar, yellow/green power gauge, four weapon slot boxes with level pips, radio
video feed with a green scanline officer portrait, crisp chunky bitmap font, pre-rendered metal
textures, saturated 90s neon CGI palette`

Negative prompt: the common negative prompt above, plus `glass panels, neon outlines, flat
dark UI` (that was the rejected variant B).

## hud-r08-a

Round 08 refresh of HUD A, unchanged in style, with the current element set (generator [tools/concept/ui_r08.py](../../../../tools/concept/ui_r08.py), `python3 tools/concept/ui_r08.py hud`): mission 11 *Atlantic Convoy* during the Harbour Kraken fight (play field is a frame of the chosen kraken-r07 animation), style-B radio portrait of Rook with signal bars and a message queue, overdrive timer bar, escort box with Rook's craft and armour, boss bar with name and weak point at the top of the play field, an edge warning at the left edge and a floating credit number.

Prompt: `late 1990s PC vertical shoot'em up in-game screen, 16:9, central portrait play field showing a giant octopus wrapped round an offshore platform at night with bullets, left and right bevelled brushed blue-violet metal side panels with rivets and recessed LCD wells: score, credits, chain, a pixel-art comm-screen portrait of a grinning pilot with signal bars and green subtitle text, armour and shield bars, power pips, four weapon slots with level pips, a magenta overdrive bar, airstrike icons, an escort box with a small wingman ship and its armour bar; red boss health bar across the top of the play field, flashing amber arrows at the left edge, crisp pixels`

Negative prompt: the common negative prompt above, plus `glass panels, neon outlines, flat dark UI`.


## edge-warnings-r09-a

Round 09. Mockup generated by [tools/concept/vfx_r09.py](../../../../tools/concept/vfx_r09.py) `python3 tools/concept/vfx_r09.py warnings`: side and rear
edge warnings (edge bar, three chevrons fading inwards, "! LEFT" / "! REAR", 4 Hz flash cycle),
sensor-suite threat arrows (size and brightness by distance, ringed for homing missiles), the
wave banner and the boss warning banner (hazard-striped band, WARNING, boss name), each shown in
a 1× play-field panel.

**Prompt:** HUD warning overlays for a late-1990s vertical shoot'em up, 480×540 play field,
bitmap pixel font: a flashing amber bar on the left edge with three amber chevrons fading
inwards and the text "! LEFT", a matching rear warning at the bottom edge, small amber
arrowheads at the screen edges pointing at off-screen threats, a thin amber-framed banner
"WARNING - HOSTILES FROM THE REAR", and a full-width boss warning band with scrolling
yellow-and-black hazard stripes, a bevelled metal edge, a large red "WARNING" and an amber
boss name underneath; crisp, high contrast, arcade style.

**Negative prompt:** glass or neon-outline HUD style (rejected in round 01), smooth vector UI,
magenta warning colours, cluttered icons, photographic textures, watermark.


## edge-warnings-r11-a

Round 11. Mockup generated by [tools/concept/ui_r11.py](../../../../tools/concept/ui_r11.py)
`python3 tools/concept/ui_r11.py a`: the round-09 edge warning at about 3× the size — a 5 px
amber bar 300 px long with a blurred glow, three 20 px chevrons fading inwards, "! LEFT" at 2×;
it grows from the centre of the edge over 0.2 s, then pulses smoothly between 45 % and 100 %
on the 0.267 s flash cycle instead of blinking on and off. Over a still Level 01 frame.

**Prompt:** HUD warning overlay for a late-1990s vertical shoot'em up, 480×540 play field,
bitmap pixel font: a long glowing amber bar along the left edge, three large amber chevrons
pointing out of the screen fading inwards, the text "! LEFT" in a large pixel font, pulsing;
crisp, high contrast, arcade style.

**Negative prompt:** glass or neon-outline HUD style, smooth vector UI, magenta or orange
bullet colours, red, photographic textures, watermark.

## edge-warnings-r11-b

Round 11, [tools/concept/ui_r11.py](../../../../tools/concept/ui_r11.py) `b`: a steady 4 px
amber bar (260 px) and three rows of 13 px chevrons that run in from the edge at 120 px/s (2 px
per 60 Hz game frame) over 96 px, three in flight per row, fading as they move inwards; 2× label.

**Prompt:** HUD warning overlay for a late-1990s vertical shoot'em up: amber chevrons streaming
in from the left edge of the play field towards the centre like a marquee, a thin amber bar on
the edge, "! LEFT" in a pixel font; crisp, arcade style.

**Negative prompt:** as edge-warnings-r11-a.

## edge-warnings-r11-c

Round 11, [tools/concept/ui_r11.py](../../../../tools/concept/ui_r11.py) `c`: the whole warned
edge of the play field lights up — a 40 px amber gradient band in 6 alpha steps (90s
translucency), breathing between 25 % and 55 % on the flash cycle, a 2 px edge line with hazard
ticks every 24 px, three 12 px chevrons and the 2× label at mid-height.

**Prompt:** HUD warning overlay for a late-1990s vertical shoot'em up: the left edge of the play
field glows with a translucent amber band and a hazard-ticked edge line, small amber chevrons
and "! LEFT" at mid-height, pulsing; peripheral-vision alert, crisp pixel art.

**Negative prompt:** as edge-warnings-r11-a.

## hud-final-r13-a

Production art, UI batch part U1 (round 13). Not a mockup: the review sheet of the HUD's final
metal parts, built by [tools/art/hud.py](../../../../tools/art/hud.py) (`python3 tools/art/hud.py`,
`--review` rebuilds only the sheet) from the files it wrote into `assets/sprites/hud/`: the
240×540 side-panel plates (SDF ray-marched at 4×, a 3 px chamfer on a dark chassis, four domed
corner rivets in recessed washers, brushed streaks and mottling, mapped through palette B's UTC
HULL ramp and ordered-dithered on the face), the 120×22 label plate, the 32×32 LCD well and the
76×76 portrait well (nine-patches, 2 px chamfer, the lip's shadow on the glass), the phosphor fill
cell and the readout glow (2D light fields), and those pieces as the game draws them.

Brief: as hud-r02-a and hud-r08-a, `brushed violet-blue chrome (#4E5AA0 to #8A96D0) side panels
with bevelled edges and corner rivets, recessed near-black LCD wells, segmented phosphor bars`,
lit from the top-left, every pixel from the generator.

## hud-capture-final-r13-a

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard
--bench 8 --settings <file> --invulnerable --debug-speed 3` under `xvfb-run -s "-screen 0 960x540x24"`
(settings in a temporary directory, so the saves are too: `display.mode=window`, a 960×540 window
at 0,0, `audio.master=0`), recorded with `ffmpeg -f x11grab -draw_mouse 0 -framerate 2`; the frame
at game time 12 s (Rook on the radio, the control prompts, the kill tracker), uncropped.

## escapes-tracker-capture-r16-a

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard --bench <s> --settings <file> --invulnerable --level 3` under `xvfb-run -s "-screen 0 960x540x24"` (settings: a 960×540 window at 0,0, `audio.master=0`), recorded with `ffmpeg -f x11grab -draw_mouse 0` at 20 fps and encoded with ffmpeg's `palettegen` (128 colours, `stats_mode=diff`) and `paletteuse` (Bayer dither, scale 5); level time ≈ the recording's time − 1.5 s. Run with `--bench 140 --loadout front=scatter-vulcan:3` and `controls.auto-fire=true`; the
arrow key Right held 2.5 s from 16 s after the start (an XTEST key event), so the ship kills the
first Spore Bomber at about t=15.3 and then sits at the right edge, letting the 42 s line through.
Crop: the left panel and the play field (720×540, 1×), 12 fps; two cuts joined: t≈14–17 (the
`LOW-AIR` prompt leaving at the first kill, `0 / 10` → `1 / 10`) and t≈54–57.5 (`2 / 10` →
`FAILED` with the red flash). The `DONE` case is not shown: it needs all ten bombers killed.
