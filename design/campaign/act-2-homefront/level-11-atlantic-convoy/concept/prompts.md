# Level 11 – concept and production notes

## backdrop-final-r33-a

Production art, M5 part E (round 33; user decision E9 = a: straight to production from the chosen
ocean scene [scene-ocean-r10-a](../../../../art-direction/concept/scene-ocean-r10-a.png)). Not a
mockup: the sheet and GIF of Level 11's backdrop in `assets/backdrop/level-11/`, rendered by
[tools/art/backdrop_l11.py](../../../../../tools/art/backdrop_l11.py)
(`python3 tools/art/backdrop_l11.py`, `--proposal` for the backdrop block in
[backdrop-proposal.yaml](backdrop-proposal.yaml), `--review` for these files, `--check` for the
checks). It imports the concept scripts that made the chosen sea (`scenes_r08.py`, `scenes_r09.py`,
`scenes_r06.py`, unchanged) and the Level 11 props of `tools/art/l11_props.py` (`pieces()`: the
burning freighter, the reef growths, the reef root).

The look: the concept's overcast slate-grey Atlantic, posterized to its seven tones, split in two
layers so the sea moves while it streams past: the long swell with the slow tone field of deeper and
paler swaths on `deep` (scroll factor 0.85, opaque) and the short chop with sparse small whitecaps on
`ground` (1.0, translucent; in register the two give the concept's sea exactly). Sections: 1 the
convoy muster under thin sea mist (~15 %); 2 the jelly fields with faint olive Vrell spawn slicks;
3 seven reef lines, 7 s apart, rust-brown chitin growths with violet veins breaking the surface over
their dark under-water shelves and roots, leaving channels for the convoy's stations, under mist
banks (~22 %); 4 an oil slick, flotsam and a capsized orange lifeboat, the abandoned burning
freighter at the right edge with its smoke billowing down the edge, Platform Tiamat entering at the
top; 5 the halt: Tiamat's centre at (240, 110), its deck x 136–344, y 34–186 (round 07's 208 × 152
model, scorched, its shadow on the water and foam lapping at its edges), the four 120 px lanes open
below; 6 open water under a lighter overcast while the platform scrolls out. The composites draw the
convoy's final sprites at their stations (in their lanes at the halt) and the Harbour Kraken's grip
overlay and sub-pass head registered on the platform; the GIF plays t 151–160, the halt held 3 s on
the real clock and the scroll resuming.

## level-11-capture-final-r33-a

Round 33, a capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound),
taken on 2026-10-09 from the uncommitted working tree, the atlases packed from the current `assets/`
by `:desktop:installDist` (an isolated build directory). `terran-vanguard --bench 300 --settings
<file> --level 11 --invulnerable --escort rook:mortar:1 --loadout
front=pulse-cannon:4,left=bomb-rack,right=autocannon-pod,rear=tail-gun:1 --special airstrike:2` at
medium (the balance plan's L11 visit: the Pulse Cannon at level 4, Rook's owned Mortar fitted again)
on a private Xvfb display (960×540; settings: a 960×540 window at 0,0, `controls.auto-fire=true`).
Video: `ffmpeg -f x11grab -draw_mouse 0 -framerate 30 -copyts`; sound: OpenAL Soft's `wave` driver
writing the game's mix to a file, nothing played aloud, aligned at the game's `device reopen` log line
(4.34 s before t = 0; aligning by the file's end, as in round 32, would put the sound 0.15 s late).
**t** is seconds since the game's `[display] window` log line: the level clock until the arena's
halt at 161, then the real clock while the level clock stands still (the level clock resumes at the
Kraken's death). The ship is flown by XTest key events from a small Python driver (dead reckoning at
270 px/s): from t 8 the slow sweep of round 32 along the start line (132–348 px, a 4 s cycle), both
Airstrike charges at t 80 and 136; from t 156.5 an arena controller: a wall reset, then it reads the
lane telegraphs off the screen (the red dashes down a lane's edges, polled at about 15 Hz) and plays
the arms first: it stands in a left-half lane so each left slam comes to it, sits under the arm while
it lies awash (auto-fire), then moves to the other left lane so the slams alternate between Halvorsen
and Mbeki; once a telegraph from the left half goes to lane 3 (the left arm severed) it moves to
lane 3 (x 280, the empty lane, where the right arm's slams cost nothing); after 16 s without a
telegraph it sits under the head (x 240).

Run 1 is the one shown: the left arm took four slams (on Mbeki at t ≈ 162.5 and ≈ 170.7, on Halvorsen
at 167.3 and 183.6) and was severed awash at t ≈ 184.0; the right arm slammed lane 3 only (t 196.5, 205.7); the
Kraken died at t ≈ 209.0, **fight 48 s** (par 1:00, rush +4,000); 154 / 276 kills (56 %), armour
untouched, secret 0 / 1, max chain 18, **hulls afloat 3 / 3** (+160), B (rating 59, A at 70),
**1,787 credits**, score 89,315. The level ends at t ≈ 234 and the debrief is up from t ≈ 236. The
video is the whole of run 1 from t −1 to 246 (247 s, H.264 CRF 27, AAC 128 kb/s, the sound 3 dB down
so the encode does not clip, 28.9 MB). The sheet: fifteen frames with the left HUD panel: the convoy
muster under sea mist (t 2.0), the jelly field with a submerged Driftjelly (50.0), a Reef Spitter
raft at the reef line (71.5), Tiamat scrolling in with the mantle shadow and the arms under it
(158.0), the bar appearing (160.5), the first telegraph in lane 2 with the ships glided to lanes 1,
2 and 4 (162.5), the lane 1 telegraph (165.8) and its slam on Halvorsen's lane (167.3), the head up
with its eyes open and a fan, the CONVOY pips amber, amber, green (175.0), the left arm severed
(184.0), the right arm on the empty lane 3 (196.5), the Kraken's death (209.0), the convoy holding
clear of Tiamat in lanes 1 and 4 while the deck scrolls down between them (211.5), the ships back at
their stations with Ruyter (216.0) and the debrief (242.0).

After the fix pass (2026-10-09): this capture predates it (the head's white silhouette under
fire, the foreshadowing at t ≈ 52–59 not visible, the arms' stretches across the mantle); the
fixed arena and the t ≈ 55 foreshadowing are in [kraken-capture-final-r33-a](#kraken-capture-final-r33-a).

## kraken-capture-final-r33-a

Round 33, the Harbour Kraken's arena as built in the game, a capture, not generated art (`.png`
sheet, `.mp4` with the game's sound). **Retaken after round 33's feedback** (2026-10-09, from the
uncommitted working tree, atlases packed by `:desktop:installDist` in an isolated build directory):
the head surfaces, stays up and sinks in open water off the deck's south edge (216 px below the
platform's centre; the arena halt raises Platform Tiamat to y 78, the deck at y 2–154, option b);
hits on the surfaced head or an arm out of the water show the normal impact, not water ripples; a
slamming arm bends in a travelling S that whips down its lane and settles awash; only the part hit
flashes, a light flesh tint ([Harbour Kraken](../../../../enemies/bosses/harbour-kraken/README.md#decisions),
Decisions 2026-10-09). The earlier fix pass's changes are kept (the flash at most once each 0.25 s,
the plain head opaque while down, the foreshadowing over the chop).

A new run of the recipe of [level-11-capture-final-r33-a](#level-11-capture-final-r33-a) (same fit,
display set-up and plan, `--bench 225`, so no debrief), the arena driver's lane reader moved to the
new lane top (screen y 158). Tiamat enters at t ≈ 159; the lane 1 telegraph at 165.8 (slam ≈ 166.8),
lane 2 at 169.4; the head surfaces at ≈ 172; the left arm slammed lane 1 again at ≈ 182.4 and was
gone before the next telegraph; the right arm slammed lane 3 from ≈ 196; the Kraken died at
t ≈ 207 (fight ≈ 46 s). Sound aligned at the `device reopen` (5.72 s before t = 0). The `.mp4` is
t 155–215 (60 s), the whole screen with the HUD (H.264 CRF 22, AAC 160 kb/s, the sound 3 dB down,
9.9 MB). The sound files are still round 33's proposals (their rework comes after).

The sheet (6 × 4, play field at 1:1): the foreshadowing under the convoy (t 55.0), Tiamat entering
with the head under the water in front of it (159.0, 160.0), the arrival with the bar (161.0), the
lane 1 telegraph (165.8), the arm rising bent (166.6), the impact as it whips down the lane (167.2),
awash with the bend settling (168.0), the head surfacing in open water (172.0) and up off the deck's
edge (173.0), its eyes open and a fan from the beak (176.0), under fire with flesh sparks (180.0),
the dive (182.0), lane 1 awash (184.0), the head up again (190.0), the right arm on lane 3 (196.5),
a mantle hit with only the head lightly tinted (199.6), lane 3 awash beside the mantle (200.3), a
lane 3 slam whipping (204.2), the head low under fire (205.5), the death sinking in front of the
platform (207.0), the scroll resuming (208.0), Tiamat scrolling on (210.0) and the ships holding
clear in lanes 1 and 4 (213.0).

## torpedo-capture-r33-a

Round 33, a capture of the game, not generated art (`.png` sheet, a short `.mp4` with the game's
sound): run 2, the recipe of [level-11-capture-final-r33-a](#level-11-capture-final-r33-a) with
`left=torpedo-pod:3` in place of the Bomb Rack (`--loadout
front=pulse-cannon:4,left=torpedo-pod:3,right=autocannon-pod,rear=tail-gun:1`, the rest of the fit
kept) and the sweep replaced from t 88 to 98.6 by a hold at x ≈ 294, under the sunken pod
(`at: [[93.78, 296]]`). Run 2: the pod freed at t ≈ 96.0, its crate floating up by Mbeki's bow
(96.5) and caught at t ≈ 98 (+160); in the arena the left arm took three slams (Mbeki t ≈ 162.5 and
≈ 170.7, Halvorsen ≈ 167.2) and was severed before t 182.2 (that telegraph from the left half went to
lane 3); fight 51 s; 168 / 276 kills (61 %), secret 1 / 1, max chain 28, hulls afloat 3 / 3 (+160),
A (rating 79), 2,269 credits, score 104,320. The `.mp4` is t 88–100 (12 s, the pod freed and the
crate caught; H.264 CRF 20, AAC 160 kb/s, sound aligned at the device reopen, 5.06 s before t = 0).
The sheet: twelve frames with the right HUD panel (its weapon rows list the Torpedo Pod): surfaced and
submerged jellies (t 23.0), a hit under the water, a white puff on a submerged target (24.1), and its
ripple ring (24.4), the reef line over the pod (94.5, 95.5), the crate floating up (96.5), drifting
to the ship (97.5), caught, +160 (98.5), the arms under the water with torpedoes running at the
submerged Kraken (160.2), the change from phase 1 to 2 with the left arm cut (171.0), the next
telegraph redirected to lane 3 (182.5) and the debrief (245.0). `NO WATER` cannot show on Level 11:
its data sets `water: true` throughout, so the pod is never idle here.

After the fix pass (2026-10-09): this sheet shows the torpedo before it; in it the torpedo was
not to be seen. The fixed torpedo (lit back over the chop, bubbles on the surface) was shown in the
last row of the fix pass's [kraken-capture-final-r33-a](#kraken-capture-final-r33-a), which was
retaken after round 33's feedback without those crops (the torpedo is unchanged since).

## level-11-readability-r33-a

Round 33, a capture of the game, not generated art: 128 px crops of run 1 of
[level-11-capture-final-r33-a](#level-11-capture-final-r33-a) (the jellies from run 2 of
[torpedo-capture-r33-a](#torpedo-capture-r33-a)) at 1:1 and at 2× (nearest neighbour) at the game's
960×540: a Driftjelly surfaced with its foam collar (t 23.0) and two submerged under the sub pass
(23.0), a Reef Spitter raft on its kelp (71.5), Halvorsen at its station with its bow foam and wake
(30.0), Saint-Laurent halted in lane 4 with its foam collar and no wake (175.0), the lane 1
telegraph, the churn and the red edge dashes of look a (165.8), the slam's spray sheets (167.3), the
arm awash under fire (168.0), the arms under the water crossing the mantle's shadow (167.0), the head
up with its eyes open and the beak lit (175.0) and the head under sustained fire drawn solid white
(206.5); below them the HUD's CONVOY tracker (224×56 at 2×: three green pips at t 160.5, amber,
amber, green at 175.0, `DONE` at 216.0) and the debrief's BOSS TIME and HULLS AFLOAT rows at 1:1 with
the HULLS AFLOAT row at 2× (t 242.0).
