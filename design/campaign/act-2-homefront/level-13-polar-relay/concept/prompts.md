# Level 13 – concept and production notes

## whiteout-r35-a

The whiteout peak (t=176–188), look a "rolling banks" (round 35, M5 part G step G2; decision G9 = a:
an a/b for the whiteout, pending the user's review). Rendered by
[tools/art/whiteout_l13.py](../../../../../tools/art/whiteout_l13.py) (`python3 tools/art/whiteout_l13.py a`),
not prompted. Brief (the README's *The halt* section 4 and *Whiteout (G7 = a)*): two layers on the real
clock (the scroll is halted): the **banks** on `low-air` roll in from both side edges (reaching ≈ 128 px
in from each edge at the peak, opaque at the edge, the centre round the relay clear) and cover the
ground units there — the Skimmers and Scuttlers come out of the white — while the Stingers, bullets,
Rook, the player and the edge warnings draw above them; the **snow** on `high-air` (≤ 40 %). Look a:
soft billowing banks lit from the top-left (pale crests, blue-grey undersides), their inner edge
undulating and rolling slowly inward and down; fine flakes drifting down with a light wind (the
arctic scene's fog, thickened). Stepped translucency (5 levels). The still shows the halted frame at the
peak with the layer breakdown and the ramp (cover 0 / 0.33 / 0.66 / 1); the loop is 4 s of the peak.
The ground is the stand-in plate of `tools/art/l13_props.py` (`HaltPlate`), the relay variant a's
proposal (`tools/art/relay.py`), the Skimmer's round-35 frames, the production Scuttler, Stinger,
orb, Rook and player sprites; the edge warnings are amber chevron stand-ins.

## whiteout-r35-b

The whiteout peak, look b "blizzard curtain", same generator (`python3 tools/art/whiteout_l13.py b`):
driving spindrift — a near-opaque white sheet at each side edge (≈ 104 px) with streamers torn off it
reaching ≈ 176 px in, streaking with the wind from the upper right (down and to the left, 70 px/s); the
snow as short slanted streaks, more of them toward the edges.

## l13-props-final-r35-a

Production art, one version each (round 35, M5 part G step G2; decision G9 = a, pending the user's
review; no concept: designed to the READMEs and the chosen arctic scene's light), rendered by
[tools/art/l13_props.py](../../../../../tools/art/l13_props.py) (`python3 tools/art/l13_props.py`; the
frames go to `assets/` only with `--assets`, in the content step's time). Not prompted.

- **CDF supply drone** (`supply-drone_0..3`, `supply-drone-open_0..3`, 40×40, nose up; `supply-drone-lights_0..1`
  additive): an H-frame of two pale-grey rails with a CDF-blue stripe, four ducted fans at the rail ends
  (translucent rotor-blur discs with two blade streaks, four phases), an avionics pod with a dark lens at
  the nose, a cargo bay between the rails with four amber clamps (closed; swung open after the release);
  red and green navigation lights and a white belly strobe. The game draws the armour patch pickup in
  the bay (0.75 scale) until the release, so the patch the player catches is the one it carried. The
  loop shows the README's arc: lift-off from the relay's helipad (scale 0.7 → 1 over 0.8 s, its shadow
  falling away), a slow ≈ 4 s arc over the relay, the release at the arc's top, the patch drifting down.
- **Abandoned research hut** (`research-hut_0..3`, 48×40; `research-hut-glow_0..7` additive): a
  rust-orange arched prefab (a Quonset with ribs) drifted with snow on its windward side, grey end
  walls and a door, a square roof hatch with an amber beacon, a guyed radio mast with a small dish,
  two olive fuel drums and a wooden sledge; states intact, one hit (the hatch scorched), two hits (the
  hatch dented ajar), opened (the hatch blown off, the dark hold); rule 7's light rim; the beacon
  blinks slowly until the first hit, then flickers (Level 12's canister's GroundGlow timing).

The review ground is the halted frame's stand-in plate (`HaltPlate` in the same script: the chosen
arctic scene's ice, water and foam, `tools/concept/scenes_r09.py`'s `ArcticScene` re-cut round the
relay's floe at (240, 250), the leads cut along the review's lanes); the production backdrop is step
G5's `tools/art/backdrop_l13.py`.

## backdrop-final-r35-a

Production art, straight from the chosen arctic scene (round 35, M5 part G step G5; decision G9 = a,
pending the user's review), rendered by [tools/art/backdrop_l13.py](../../../../../tools/art/backdrop_l13.py)
(`python3 tools/art/backdrop_l13.py`; `--check` for the lanes on water (each unit's run as the sim
flies it, per wave: its lane's centre and its hull on the weave, ≥ 95 % each), the relay's and the hut's
floes, the seam, the wraps, the motion budget and the fog over the ice (it must not darken it); `--proposal` writes
[backdrop-proposal.yaml](backdrop-proposal.yaml), the block merged into the level's data). Not prompted:
the code takes [scene-arctic-r09-a](../../../../art-direction/concept/scene-arctic-r09-a.png)'s ice
(`tools/concept/scenes_r09.py`: the ICE9 ramp, hillshade and rim, the teal submerged margins, the long
cool shadows, the waterline foam) and its water's colour stops, and the whiteout from
[whiteout-r35-a](#whiteout-r35-a) (`tools/art/whiteout_l13.py`, look a).

- **The ground** (`pack-ice-1..4`, four 480×1,536 pieces, one render from ground −144 to the halt's top
  edge at 6,000): white-blue floes on translucent dark water (alpha 3: the shadow stencil), the leads cut
  along every Skimmer unit's lane (the simulation's Catmull-Rom curves laid on the ground at each unit's
  own entry, so a convoy's units 1.5 s apart lie on three leads in the approach; 58 px either side, a
  rough edge that never narrows below 57 px: the 40 px weave plus the hull's 15 px half width, checked
  across the hull's full width), two meandering approach leads joining the halt's west and east leads and the introduction's
  lead, the relay's floe at (240, 250) in the halt and the research hut's floe (t=12, x=380); brash in
  the leads' outer bays (beyond the hulls' reach) and rubble where a Scuttler's path crosses a lead; open water in the top rows.
- **The exit** (`open-floes`, 480×960): looser floes and open sea, open water where it begins.
- **The water** (`arctic-water`, `deep`, 480×960, drifting 3 px/s left): the dark arctic swell (r09's
  stops, 7 tones), a wandering, damped swell rather than stripes; it tiles both ways.
- **Loose ice** (`loose-ice`, 96×112, 8 headings): seven drifting floe and brash clusters and, as
  heading 2 (east), the Polyp Mortars' floe (a flat floe 64×84 the two mortars stand on).
- **Fog** (`fog-wisps` ~10 %, `fog-banks` ~20 % at the edges): pale fog, every tone at or above the
  ice's lit snow (lighter towards its dense core, lit from the top-left), soft blurred edges, 16 alpha
  steps; it lightens the ice it covers and lies as a pale veil over the water, and the **whiteout**
  (480×540, one period of look a's banks at the peak, both side edges, the centre clear).


## level-13-capture-final-r35-a

Round 35, a capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound), taken on
2026-10-12 (step G7) from a snapshot of the uncommitted working tree with the step's two render fixes
applied (the Skimmer drawn as a boat, the relay's rising damage smoke; see the README's *Decisions*), the
atlases packed from the current `assets/` by `:desktop:installDist` (an isolated build directory).
`terran-vanguard --bench 256 --settings <file> --level 13 --difficulty medium --escort rook:autocannon:1
--loadout front=pulse-cannon:4,left=micro-missile-pod:2,right=micro-missile-pod:2,rear=tail-gun:1
--special airstrike:2` (the balance plan's L13 visit) on a private Xvfb display (960×540; settings: a
960×540 window at 0,0, `controls.auto-fire=true`), **without `--invulnerable`** (with it a lost relay
does not fail the mission). Video: `ffmpeg -f x11grab -draw_mouse 0 -framerate 30 -copyts`; sound:
OpenAL Soft's `wave` driver (`ALSOFT_DRIVERS=wave`) writing the game's mix to a file, nothing played
aloud, aligned at the game's `device reopen` log line. **t** is seconds since the game's `[display]
window` log line. The ship is flown by XTest key events from a small Python driver (dead reckoning at
270 px/s): a sweep along the bottom, the hut's hatch at x 380 (t 11–16), then guarding at 170 px above
the bottom edge under the relay with a sweep between x 150 and 330, parking under the mortar floe (t 66–80)
and beside each Scuttler pincer (t 104–116, 166–178), under the relay for the Stingers (t 156, 209) and
the two drones' patches (t 161, 201); Airstrikes at t 105.5 and 165.

**193 / 208 kills (93 %)**, armour damage 53 of 60, **relay 54 %** (the secondary met, + 152 credits),
**secret 1 / 1** (its crate missed: the SECRETS credit row reads 0), **A**, 2,776 credits. The video is
the whole run from t −1 to 255.8 (the debrief is up from t ≈ 251, after Okafor's t = 233.5 line and
Rook's level-end line; H.264 CRF 27, AAC 128 kb/s, the sound 3 dB down, 17.5 MB). The sheet: sixteen
frames with the left HUD panel, thirteen of this run (the approach with a Skimmer and its wake, the halt
easing in, the first cross, a mortar lob's marker on the relay with its hit flash, the Airstrike's ground
bursts on the ice, the walkers' fans, the Stingers' dive, the drone's arc and its patch, the whiteout's
peak, the Needler circle in the fog, grid online, the debrief) and three of shorter runs: easy's 6 s
whiteout peak (t 182; easy, `--invulnerable`), the damage smoke below 50 % (no firing, `--debug-speed 2`,
the ship lost at t ≈ 155 before the relay) and the destroyed relay's smoke column (t 109; medium,
`--invulnerable`, no firing: the relay falls at t ≈ 107, the mission runs on as `--invulnerable` keeps
a lost relay from failing it).

## level-13-capture-fixes-r35-a

Round 35, a capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound): a short
supplement to [level-13-capture-final-r35-a](#level-13-capture-final-r35-a) (kept as it is) showing the
fixes made after it (the backdrop's Skimmer leads and pale fog, the end's chord and jingle, the mortars'
wreck drift; see the README's *Decisions*). Taken on 2026-10-12 (step G9) from a snapshot of the
uncommitted working tree, `:desktop:installDist` in an isolated build directory, on a private Xvfb display
(960×540, a 960×540 window at 0,0), the sound from OpenAL Soft's `wave` driver aligned at the game's
`device reopen` log line, the video `ffmpeg -f x11grab -draw_mouse 0 -framerate 30 -copyts`; **t** is
seconds since the game's `[display] window` log line. Two runs, both `--invulnerable` (to reach the end
for certain):

- **guard run**: the capture's command and driver (`--bench 256 --level 13 --difficulty medium --escort
  rook:autocannon:1 --loadout front=pulse-cannon:4,left=micro-missile-pod:2,right=micro-missile-pod:2,rear=tail-gun:1
  --special airstrike:2`, `controls.auto-fire=true`, Airstrikes at t 105.5 and 165) plus `--invulnerable`;
- **no-firing run**: `--bench 32 --level 13 --difficulty medium --invulnerable`, `controls.auto-fire=false`,
  the ship parked at the bottom centre, so the t=20 Skimmers live through their whole lead (in the guard
  run they die within a second of entering).

The video (45 s, H.264 CRF 24, AAC 128 kb/s, the sound 3 dB down, a caption and the level time burnt in)
is five cuts: the Skimmers t 19.5–29 (no-firing run), the mortars' floe 73.5–82, a fog bank 139–146, the
whiteout 177–186 and the end 215–226 (the guard run). The sheet: (1) the Skimmers at t 20.75–25.25 and
2× insets at 24.5, 26.0, 26.75 and 27.5; (2) the mortars' floe at 1:1 from 76.8 to 81.0 (both mortars
shot at t ≈ 77 in this run, their burst on the floe and drifting with it, nothing left at the screen's
left edge); (3) the fog bank over the north-west lead at t 145 at 2×, the capture's frame against now;
(4) the whiteout at t 176, 182 (peak) and 186 at 0.5×; (5) the mix's level (RMS per 50 ms) from t 214 to
226 with each sound placed by matching its file against the mix (8 kHz): grid online's chord at
218.05–220.65 (match 0.72), the jingle from 220.65 (0.95), the mix dipping to ≈ −25 dBFS at t 220.4 between
them.

## level-13-capture-leads-r35-a

Round 35, a supplementary capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound) after G5c's
wider leads, taken on 2026-10-12 (step CF) from a snapshot of the uncommitted working tree (10:40), built by `:desktop:installDist` into an isolated build directory, on a private Xvfb display (960×540; settings: a 960×540 window at 0,0), the sound from OpenAL Soft's `wave` driver (`ALSOFT_DRIVERS=wave`, nothing played aloud) aligned at the game's `device reopen` log line, the video `ffmpeg -f x11grab -draw_mouse 0 -framerate 30 -copyts`; **t** is seconds since the game's `[display] window` log line. The clip: H.264 CRF 26, AAC 128 kb/s, the sound 3 dB down, a caption and the level time (0.1 s) burnt in (`drawtext`), the cuts joined with the concat demuxer. One run: `--bench 56 --level 13 --difficulty medium --invulnerable`,
`controls.auto-fire=false`, the ship parked at the bottom centre (no keys), so the t=20 Skimmers live through their whole
leads (the recipe of the no-firing run of [level-13-capture-fixes-r35-a](#level-13-capture-fixes-r35-a)). The video
(22 s, 1.5 MB) is two cuts: t 18–30 (the Skimmers) and 40–50 (the halt frame). The sheet: (1) the play field at 0.5×,
t 21–27; (2) 2× insets of the eight hull positions nearest to ice, picked by a scan of t 18.5–30.5 at 10 fps (hull pixels
within 6 px of white): in each the white is the boat's own wake, the nearest floe edge ≈ 15 px off (t 21.6); no hull
over ice; (3) the halt frame at 1:1 (t 40, 46, 52): the relay's floe in brash-strewn water, small floes along both side
edges, the walkers closing.
