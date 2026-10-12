# Level 12 – concept and production notes

## eye-r34-a

The eye of the storm, look a "billowing wall, cool shaft" (round 34; user decision F8 = a: the eye's
look a/b, the rest of the backdrop straight to production). Rendered by
[tools/art/eye_l12.py](../../../../../tools/art/eye_l12.py) (`python3 tools/art/eye_l12.py a`), not
prompted. Brief: section 5 is the calm in the storm — the rain stops, the sea lies dark and calm, and
the storm's cloud wall stands all round the play field, turning slowly clockwise (≈ 3°/s, the README;
its direction is still an open question), with a shaft of pale light falling on the sea. Two
screen-fixed pieces the game draws over the scrolling sea: the **wall ring** (760 px RGBA, centred on
the play field, turned by the game) and the **shaft** (480 × 540 additive). The ring is a height field
of billows on a wall rising over ~100 px from a lobed edge (radius ≈ 228 px), lit by a light from the
eye's centre and above, so it is lit the same way on every side and may be turned without the light
turning with it; stepped alpha (quarters), 28 colours. Look a: round billow crowns, a gentle spiral in
the texture, slate base, silver-white lit faces; a cool silver-white pool up and left (toward the break
in the cloud) with a faint haze column toward it. The sea is a stand-in (Level 11's swell tile darkened
and calmed); step 5's `backdrop_l12.py` makes the eye's own sea and imports `wall_ring` and `shaft`.

## eye-r34-b

The eye, look b "banded spiral wall, warm shaft", same generator (`python3 tools/art/eye_l12.py b`):
the wall's texture stretched along a stronger spiral into bands, two thin feeder arms curling into the
eye, a darker, more ominous palette with pale-gold lit faces; a broad warm pale-gold pool in the centre
with slow sun glints on the swell (each fading in and out over 1.2–1.8 s, within the motion budget).

## backdrop-final-r34-a

Production art, M5 part F (round 34; user decision F8 = a, straight to production), not prompted: the
Level 12 backdrop rendered by [tools/art/backdrop_l12.py](../../../../../tools/art/backdrop_l12.py)
(`python3 tools/art/backdrop_l12.py`; `--proposal` writes [backdrop-proposal.yaml](backdrop-proposal.yaml),
`--check` the checks). Source look: the chosen storm scene
[scene-storm-r10-a](../../../../art-direction/concept/scene-storm-r10-a.png) (its colour stops, wave
components, light gain, torn crests, wind streaks and dark scud tones), built on Level 11's ocean kit
(the sea split into the long swell on `deep` and the short chop on `ground`). Brief: the sea and the
game's rain stay the only strong motion (nothing in the backdrop is animated or drifts); the sections
differ by bank cover (light ~12 % at the edges, medium ~22 %, heavy ~35 % with the centre lane kept
clear) and haze toward the rain's grey (the wall's sea drops in contrast); the cloud banks are opaque in
their cores so the game can draw `low-air` bodies under them (F1 = a) and light them from inside for a
flash's 0.5 s reveal; two tall banks on the right carry the drifting canister through its time on screen
(F2 = a; placed from the data's canister). The eye (section 5) has its own calm chop, no haze and two
seam pieces fading the storm's chop in and out exactly at the tile change; its cloud wall and light
shaft are [eye_l12.py](../../../../../tools/art/eye_l12.py)'s look a (the round's audition decides;
`EYE_LOOK = "b"` re-renders), drawn by the game as screen-fixed looks: the wall a separate 760 px image
it turns clockwise at 3°/s about the play field's centre (lit radially, so any turn keeps its light), the
shaft added over the sea; the breakout's light an additive glow at the top edge from t=190. The review's
rain, flash, units and gust arrow are stand-ins for the game (step 6), with
[l12_props.py](../../../../../tools/art/l12_props.py)'s sprites.

## l12-props-final-r34-a

Production art, M5 part F (round 34; F8 = a, straight to production), not prompted: Level 12's props and
weather looks rendered by [tools/art/l12_props.py](../../../../../tools/art/l12_props.py)
(`python3 tools/art/l12_props.py`). Brief: the **weather sonde** (F7 = b, on the play plane, 24 px body)
is a pale round instrument buoy with a slate collar, sensor ports and a cool white strobe (no spikes: it
must never read as a mine), a light outline (pickups' rule 6), a blink loop; its collect pop a ring with
data sparks. The **supply canister** is Level 11's CDF supply pod (olive drum, amber hazard ends, beacon)
under an olive-and-white drogue chute trailing up-screen on its lines, with rule 7's light rim and a
damage state (a scraped drum, a torn gore), its spent frame split open with the chute collapsed; its
amber beacon blink is the faint glint drawn over the bank (F2 = a). The weather looks are additive:
rain streaks slanting with the wind from the left (drawn at 40 %), a pale blue-white gust arrow and wind
streaks unlike the amber edge warning, forked lightning bolts (white core, blue glow; at most 0.1 s,
never more than 3 flashes a second), a cloud lit from inside for the reveal and the faint glow of a
hidden low-air body.

## level-12-capture-final-r34-a

Round 34, a capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound), retaken
on 2026-10-12 after the fixes of 2026-10-11 (the shake-off prompt in the play field, denser rain, the 2×
gust arrow, the reveal drawing `low-air` bodies over the banks, the gust and shake-off at the warnings'
level with the theme's −6 dB duck), from a snapshot of the uncommitted working tree, the atlases packed
from the current `assets/` by `:desktop:installDist` (an isolated build directory). `terran-vanguard
--bench 232 --settings <file> --level 12 --escort rook:autocannon:1 --loadout
front=pulse-cannon:4,left=micro-missile-pod,right=micro-missile-pod,rear=tail-gun:1 --special
airstrike:3` at medium (the balance plan's L12 visit: both wing pods sold for two Micro-missile Pods, the
third Airstrike charge, Rook's owned Autocannon again; no Proximity Mines in the fit) on a private Xvfb
display (960×540; settings: a 960×540 window at 0,0, `controls.auto-fire=true`). **Without
`--invulnerable`**: that option skips the ship's collisions, the latch included, so a run with it is
never latched. Video: `ffmpeg -f x11grab -draw_mouse 0 -framerate 30 -copyts`; sound: OpenAL Soft's
`wave` driver (`ALSOFT_DRIVERS=wave`, its `file` in an `ALSOFT_CONF`) writing the game's mix to a file,
nothing played aloud, aligned at the game's `device reopen` log line (9.51 s before t = 0). **t** is
seconds since the game's `[display] window` log line (the lightning flashes show at their data times
+0.05–0.1 s). The ship is flown by XTest key events from a small Python driver (dead reckoning at
270 px/s from x 330, the data's wind push added to the estimate): a slow sweep between x 150 and 330
(180–330 from t 70), a wall reset (Left 2 s) before each set piece, then parks: sonde 1 at x 400 from
t 59.7 to 70, the canister at x 412 from 98 and following its drift left to 102, sonde 2 at x 70 from
108 to 121, sonde 3 at x 390 from 162.6 to 176; Airstrikes at t 80, 136 and 186.7. A latch watcher
reads the right HUD panel's shield label at 10 Hz (amber `SHIELD DRAIN`) and, 1.6 s after it turns
amber, shakes (Left/Right 0.1 s, six presses).

**226 / 234 kills (97 %)**, armour damage 50 of 60, max chain 62, **sondes 3 / 3** (collected at
t 67.2, 117.1 and 172.1, +120 credits), **secret 1 / 1** (the canister hidden under its bank at t 99.45,
drawn over the banks in the t 99.5 flash and broken at t ≈ 100; its crate drifted past the sweep, so the
debrief's SECRETS credit row reads 0), **A** (+20 %, 454), **3,024 credits** (300 at launch), score
157,485. **Three latches**: t ≈ 112.5 on the tail (shot off within 0.3 s: no prompt, as the rule says;
Rook's line follows), t ≈ 115.3 on the right flank (the `SHAKE OFF` · `LEFT-RIGHT` prompt in the play
field below the ship from t ≈ 115.5, **shaken off** at ≈ 118.7, the prompt gone, the Lamprey thrown and
stunned), t ≈ 186.6 from the rear swarm during the third Airstrike (no prompt after the attempt's first
shake-off; shaken off at ≈ 188.7). The level ends at t ≈ 220 and the debrief is up from t ≈ 221. The
video is the whole run from t −1 to 230 (231 s, H.264 CRF 27, AAC 128 kb/s, the sound 3 dB down so
the encode does not clip, 27.9 MB). The sheet: fifteen frames, the left HUD panel with the play field
(the latch frames and their `SHIELD DRAIN` with the right panel instead): the gust telegraph (t 15.3),
the gust arrow above the side warnings and their banner (117.2), sonde 1 collected (67.3), lightning at
t 78 (78.12), the canister hidden (99.45) and drawn in the flash (99.62), the first latch (112.6), the
prompt (117.8), shaken off (119.2), the storm wall (138), the eye (158), sonde 3 and `SONDES DONE`
(172.3), the third latch without a prompt (187.2), the breakout light (203.5) and the debrief (225).

## level-12-readability-r34-a

Round 34, a capture of the game, not generated art: 128 px crops at 1:1 and at 2× (nearest neighbour) at
the game's 960×540, retaken on 2026-10-12 (step G9) from a rerun of the
[level-12-capture-final-r34-a](#level-12-capture-final-r34-a) recipe (the same command, fit, driver and
settings, medium, no `--invulnerable`) on a snapshot of the uncommitted working tree with the shake-off
cue at +3.5 dB (step G8), so the rain, the 2× gust arrow and the shake-off prompt are the current ones.
That run: sondes 3 / 3 (collected at t 67.1, 117.1 and 172.3), latches at t ≈ 112.4 (on the left flank,
gone by ≈ 113 before any shake), ≈ 115.0 (on the right, prompt from ≈ 115.5, shaken off at ≈ 118.6) and
≈ 186.6 (under the third Airstrike, shaken off at ≈ 188.7). The crops: the steady rain (t 100.0) and the
storm wall's (138.0), the gust telegraph's 2× outline arrow and streaks at the top left (15.3), a Spore
Bomber half hidden under a bank with its spores on top (68.4), the canister's glint under its bank
(99.45) and the canister fully seen in the flash with the lit cloud below it (99.62), the first latch on
the left flank with the hull flash (112.7), the Lamprey latched on the right (116.0), a stunned Lamprey
thrown off (119.3), sonde 1 drifting in the rain (65.5) and its pop at the ship (67.1), the eye's ring
and its light shaft (158.0), the breakout light at the top edge (203.5) and a bank lit from inside in the
wall with the bolt (147.08); below them the HUD's `SONDES` tracker (220×28 at 2×: `0 / 3` at t 2.0,
`1 / 3` at 67.2, `2 / 3` lit at 117.1, `DONE` at 172.3), the shield gauge's `SHIELD` (112.2) and `SHIELD
DRAIN` (112.5) at 1:1 with the drain at 2×, the `SHAKE OFF` · `LEFT-RIGHT` prompt in the play field
below the ship at 1:1 and 2× (116.0), and the debrief's `SONDES` row at 1:1 and 2× (t 225.0).

**The shake-off cue in the mix** (the same run's sound, OpenAL Soft's `wave` driver, aligned at the
`device reopen` log line; each sound's first 0.25 s matched against the mix at 8 kHz over pitches
0.95–1.05, the level = the file's RMS + the fitted gain): the shake-off at t 118.63 (match 0.67, the
template's 1 % noise floor 0.15) sits at ≈ −14.9 dBFS, the latches at t 112.53 (0.31, floor 0.25) and
186.77 (0.47) at ≈ −22.2 and −20.3 dBFS: **the shake-off ≈ 5.4–7.3 dB above the latch** (the file alone
is 3.5 dB louder now, −16.1 against the latch's −19.6 dBFS RMS). The third shake-off (t 188.72, under the
third Airstrike's bursts) matches only weakly (0.25) and fits ≈ −23 dBFS; that estimate is masked and
not reliable.

## level-12-capture-mines-r34-a

Round 34, a supplementary capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound): the
Proximity Mines ([weapon](../../../../player/weapons/proximity-mines/README.md)) in play and whether their drop and arm
sounds are heard, taken on 2026-10-12 (step CF) from a snapshot of the uncommitted working tree (10:40), built by `:desktop:installDist` into an isolated build directory, on a private Xvfb display (960×540; settings: a 960×540 window at 0,0), the sound from OpenAL Soft's `wave` driver (`ALSOFT_DRIVERS=wave`, nothing played aloud) aligned at the game's `device reopen` log line, the video `ffmpeg -f x11grab -draw_mouse 0 -framerate 30 -copyts`; **t** is seconds since the game's `[display] window` log line. The clip: H.264 CRF 26, AAC 128 kb/s, the sound 3 dB down, a caption and the level time (0.1 s) burnt in (`drawtext`), the cuts joined with the concat demuxer. Two runs:

- **run A** (the real mix): the capture's command with the mines in the rear slot, `--bench 95 --level 12 --difficulty
  medium --escort rook:autocannon:1 --loadout front=pulse-cannon:4,left=micro-missile-pod:2,right=micro-missile-pod:2,rear=proximity-mines:1
  --special airstrike:3`, `controls.auto-fire=true`, the capture's driver (wind-corrected dead reckoning, the latch
  watcher), no `--invulnerable`;
- **run B** (control): `--bench 50 --level 12 --difficulty medium --escort none --loadout
  front=pulse-cannon:1,rear=proximity-mines:1 --invulnerable`, the ship parked, `audio.music`, `audio.radio` and
  `audio.voice` at 0. (Two more controls with `front=hammer-mortar:1` were not usable: a latched Lamprey's drain and the
  mortar's blasts fill the mix.)

The video (24 s, 3.2 MB): run A t 14–30 (the left flank wave), run B t 24–32. The sounds: the mix (mono, 16 kHz) matched
against the two files (`shot-mine-r03-a`, the drop, played at the shots' −14 dB; `weapon-mine-arm-r28-a`, the arm, at
−10 dB) by normalised cross-correlation over a ±5 % pitch scan. The **arm** is found 0.38–0.40 s after each drop at its
coded −10 dB: NCC 0.7–0.8 in run B (20 in 44 s), 0.3–0.5 in run A (≈ 25 in 89 s; the windows' 90th percentile 0.17),
so it is in the mix as a quiet tick under the guns. The **drop** is found in neither run (NCC ≤ 0.2 even at the arm
times less 0.38 s): the mean power of the mix over the drops (aligned by their arm chirps, plotted in the sheet) sits
≈ 10 dB above the drop's click and clunk in run B and 15–20 dB above them in run A; the front gun fires on the same tick
at the same level, so the drop is masked. Not judged by ear (Claude cannot listen). The sheet: (1) the mines behind the
ship at 1:1 (run A, t 16–20); (2) 2× around the parked ship (run B, t 25–27); (3) the plot.
