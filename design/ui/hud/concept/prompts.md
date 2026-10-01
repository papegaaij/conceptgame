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
