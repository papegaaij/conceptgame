---
title: Concept round 28 — M5 part A, the Act 2 systems
design: approved
implementation: n/a
art: chosen
depends-on: [../../player/wingmen, ../../player/weapons, ../../player/systems, ../../ui/hangar, ../../ui/hud, ../../audio/sfx, ../../audio/voice, ../../enemies, ../../systems/economy, ../../systems/saves]
updated: 2026-10-06
---

# Concept round 28 — M5 part A, the Act 2 systems

## Summary

The review round of M5 part A ([roadmap](../../tech/roadmap/README.md)), the Act 2 systems built
before the Act 2 levels: Rook in the escort slot. That covers his production sprites, his AI as
built, his escort UI in the hangar and the HUD, and his 27 voiced radio barks. It also covers the
**final** effects of the five Act 2 weapons that need no water (Tail Gun, Fan Blaster, Hornet
Launcher, Swivel Gun, Proximity Mines) and the Targeting computer and Salvage scanner, whose details
are `draft`. Two a/b picks are new: the mine's arming beep and the scanner's secret glint. The round
also holds the hangar's tile abbreviations, the act HP factor and the boss-bounty rule, two
changes to note (debug runs write no saves, the Coilwyrm head's ×2 fix), and a checklist of the
user's part A decisions. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 28`).

Every art row is *approve as final* or *redo* (say what to change). An a/b row is *pick a or b*.
A built row is *approve as built* or *change* (say what). A row with a **question** needs an answer
too. Only an approved part gets `art: final`.

In the game (`./gradlew :desktop:run --args="…"`):
- Rook on Act 1's levels: `--level 6 --escort rook:missiles:3,side=right`.
- The Act 2 weapons and modules: `--loadout
  front=hornet-launcher:3,left=swivel-gun:3,right=swivel-gun:3,rear=proximity-mines:3,utility=targeting-computer,utility2=salvage-scanner:2`
  on `--level 7` (the brackets on the Brood Carrier's sacs, the glint on the lifeboat's tow cable).
- The Tail Gun and the Fan Blaster: `rear=tail-gun:5` / `rear=fan-blaster:5`.

Option a of each a/b pair is what the game played while the round was open; the user chose a for
both.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Rook's craft](../../player/wingmen/README.md#in-the-game-m5-part-a) | production art (`tools/art/rook.py`, straight from the chosen round-09 Ember model, no a/b): `rook_0` … `rook_4` (42×42, banking at −30, −15, 0, +15, +30° through the Stormhawk's camera, so the pair banks as one); a warm 8×14 additive engine flame loop (3 frames) at both nozzles (`pivots/rook.json`); the eject pod (16×16 slate capsule, yellow nose band, red beacon, tumbling in four 45° steps, the beacon blinking once a loop) | `rook-final-r28-a` (png, gif) | the bank angles are the player's ±15 / ±30°, rounded from the concept's ±14 / ±28°; the pod was drawn once, not as the a/b concepts the art-track item named; at 1× the 16×16 pod is small (it reads by its glow and trail, row 2) || **approved as final** (user): `rook_0` … `rook_4`, `rook-flame_0` … `rook-flame_2`, `rook-pod_0` … `rook-pod_3` and `pivots/rook.json`; the pod as drawn once (no a/b) and its size at 1× accepted; wingmen `art: final` |
| 2 | [Rook in the game](../../player/wingmen/README.md#shot-down-retry-and-checkpoint) | game captures: Rook in Wing and Wide, his muzzle flash, smoke below 30 %, the eject with the `medium` explosion and `EJECTED`, his rear-wave bark, the hangar's escort tab (sheet and a 101 s video with the game's sound); his first in-game look with the placeholder frames; the eject **after the pod fixes**: the pod takes the nearer side edge unless the ship is in the way (36 px), is drawn over the explosion after 0.1 s with a red beacon glow and a light smoke trail, and the eject bark plays at once | `part-a-capture-final-r28-a` (png, [mp4](../../player/wingmen/concept/part-a-capture-final-r28-a.mp4)), `rook-ingame-capture-r28-a`, `rook-eject-capture-r28-b` | the sheet and video were taken **before** the AI fixes (row 3) and the pod fixes, on scratch data (his armour 5), because in seven runs he was never hit; in the video the pod still passes under the ship; the eject capture also uses scratch data (armour 2, hit box 40, no dodging); the pod's in-the-way branch did not occur in a run (`WingmanPodTest` covers it) || **accepted** (user): the captures and the eject after the pod fixes, as built |
| 3 | [Rook's AI](../../player/wingmen/README.md#rooks-ai-first-draft-parameters) | built: the formations Wing (64, +28), Wide (120, +10) and Trail (40, +90). **Trail** is used on rear threats (a `rear` wave, a Coilwyrm chain from the rear, a chain that loops back), wins over Wide, and he takes the mirrored slot at the edges. He **fires whenever the player fires**, at his gun's rate, with or without a target. He **dodges about 70 %** of the bullets he predicts, each decided once by his own seeded generator, with a **0.6 s look-ahead** and a 0.25 s delay. He keeps 14 px clear of air enemies' bodies and **at least 40 px from the player**, also at the margins. Enemies never aim at him. The measured wear is under [Part A numbers](#rooks-wear). **Question:** the *flank threat* (the Wide formation and target priority 2) is an enemy within **160 px of him as plain distance** (a circle round his centre, so an enemy just ahead counts too), not "closing from the sides": confirm? | `part-a-capture-final-r28-a`, `rook-eject-capture-r28-b` | the wear table was measured with the 0.4 s look-ahead; only hard Level 07 was re-measured at 0.6 s (8 runs, no eject, armour 4–50 left), with medium "about the same"; the data's comment on `flank_distance` says "px horizontally" while the code and the README use plain distance; no capture shows the AI after the fixes || **approved as built** (user); **question answered**: the flank threat is plain distance, an enemy within 160 px of his centre (confirmed; the wingmen Behaviour row reworded, the data comment already said so) |
| 4 | [Escort in the hangar](../../ui/hangar/README.md#escort-from-level-08) | built: once Rook is hired the **escort is a tab** after the rear mount. His 92×92 callout shows his side `L`/`R`, his icon, his fitted gun with five pips and his armour, amber below 50 % or `GROUNDED` in red. His **gun shop** has his four guns at **60 %** of the player's prices: buy, upgrade each L1–L5, fit one, sell, all in the visit's undo, draw 0, test-fired from his nose muzzle. **Side**: a last row `ROOK'S SIDE`, **outside the undo**. **Repair**: lines `SHIP` and `ROOK` (Q/E switch) at the difficulty's repair cost. **Launch warnings**: `ROOK'S ARMOUR 34/80` below 50 % and `ROOK IS GROUNDED AND STAYS HOME`. **Grounded**: at 0 armour after an eject, until any repair | `part-a-capture-final-r28-a` (the hangar panel), [hangar decisions](../../ui/hangar/README.md#decisions) | the hangar capture used a hand-placed save at Level 08; the capture's "1 FREE SMART BOMB CHARGE" notice was an artefact of that save, not a bug; the escort callout grew to 92×92 px so his longest gun name fits || **approved as built** (user); hangar `done` again (`art` stays `chosen`: the later acts' maps and intel pictures) |
| 5 | [HUD escort box](../../ui/hud/README.md#decisions) | built after the chosen mock (`hud-r08-a`): an `ESCORT` plate under the special's row; a 38 px well with his 16 px icon; `ROOK` and his armour number on its top row (or `EJECTED` in red); his 8 px armour bar under them. The number and the bar flash red at 30 % in the player's rhythm, and nothing is drawn without an escort. The radio row names the eject bark as urgent | `rook-ingame-capture-r28-a`, `part-a-capture-final-r28-a` | the mock shows no states, so `EJECTED`, the flash and the empty level are main-agent choices; the "not yet available" list moved under the escort's region || **approved as built** (user): the code-drawn box is final with the panel; HUD `done` again |
| 6 | [Rook's radio barks](../../player/wingmen/README.md#radio-barks) | **listening row**: the 27 voiced barks (8 triggers, 3–4 variants each; quoted under [Listening](#listening)), rendered with Rook's cast voice and radio filter b. They are event lines: 8 s spacing, one waiting at a time by priority, stale after 6 s, and a bark is dropped within 8 s of a scripted Rook line. **The eject bark is urgent**: it is exempt from the spacing, the scripted lines and going stale, and it cuts his waiting or playing bark. Accept as rendered, or name the lines to re-render | the files under [Listening](#listening) | reviewed by Whisper only. Four takes were re-rolled and pinned: "Rook's hurting…" ("herding"), "Flank! Watch the edges…" ("Clank"), "Power-up! Grab it!" (the lost "P") and "Overdrive's up for grabs…" ("Over drives"). The rear-wave variant "Contacts on six! Why is it always six?" repeats Level 06's scripted line (a separate take) || **accepted as rendered** (user): all 27, the four pinned takes included; none to re-render |
| 7 | [Tail Gun](../../player/weapons/tail-gun/README.md#concept-art) | production art (`tools/art/act2_weapon_fx.py`, the chosen round-08 families): the `rear` energy bolt (`energy_bolt(10, 2.2)`) straight back, with the shared pulse muzzle flash and impact; its `pulse` sound 10 % lower as a rear gun | `tail-gun-final-r28-a` (png, gif), `act2-weapons-capture-r28-a` | the bolt is drawn only straight back (the only angle its patterns use) || **approved as final** (user); Tail Gun `art: final` |
| 8 | [Fan Blaster](../../player/weapons/fan-blaster/README.md#concept-art) | production art: the `rear` bolt shorter and rounder for its 6×6 hit box (`energy_bolt(7, 2.5)`) at every angle of its fans (145–215°), with the pulse muzzle flash and impact; the shots fade over the last quarter of their 390 px; its `pulse` sound 10 % lower | `fan-blaster-final-r28-a` (png, gif), `act2-weapons-capture-r28-a` | seen in the game only with the ship standing still (the capture had no input); the × in the capture is the X server's mouse pointer || **approved as final** (user); Fan Blaster `art: final` |
| 9 | [Hornet Launcher](../../player/weapons/hornet-launcher/README.md#concept-art) | production art: the Hornet missile (16 px with its plume) at 32 headings, a six-frame smoke-trail puff every 4 steps, the launcher muzzle flash and the explosive impact. The missiles leave the ±10 px ports in turn and speed up from 450 to 650 px/s. It plays the `missile` family's sound | `hornet-launcher-final-r28-a` (png, gif), `act2-weapons-capture-r28-a` | the 16 px missile is small at 1×, so the volley reads mostly by its smoke puffs; seen in the game only with the ship standing still || **approved as final** (user); Hornet Launcher `art: final` |
| 10 | [Swivel Gun](../../player/weapons/swivel-gun/README.md#concept-art) | production art: the turret's tracer (`tracer(8, 1.4)`, the `ballistic` family) at 32 headings, with the ballistic muzzle flash and impact; the turret aims all round at 360°/s (432 with the Targeting computer) and fires only with a target | `swivel-gun-final-r28-a` (png, gif), `act2-weapons-capture-r28-a` | **the pod's barrel is drawn fixed**: tracers leave in every direction from a barrel that does not turn || **approved as final** (user), the fixed barrel accepted; Swivel Gun `art: final` |
| 11 | [Proximity Mines](../../player/weapons/proximity-mines/README.md#concept-art) | production art: the mine (12 px) dark while unarmed and pulsing once armed; a 12-frame blast (104 px, the round-09 fireball with a blue shock ring out to the 48 px radius); the launcher muzzle flash. The drop plays the `mine` family's sound, the blast the small explosion | `proximity-mines-final-r28-a` (png, gif), `act2-weapons-capture-r28-a` | **the mine's sensor light reads only at 1–2 px**, so armed and unarmed are hard to tell apart at 1×; **the blast can draw over the ship's tail** when a mine goes off just behind it; **nothing marks a fizzle** (a mine that times out just goes) || **approved as final** (user), the weak spots accepted as they are; Proximity Mines `art: final`, `done` |
| 12 | [Mine arming beep](../../audio/sfx/README.md#concept-art) | **synthesized, pick a or b** (`tools/concept/audio/sfx_r28.py`, the round-08 UI family, peak −12 dBFS), played the step a mine arms, at most two at a time, −10 dB: **a** "armed chirp", two soft rising pulse blips a fifth apart (0.10 s); **b** "sensor ping", one sine ping gliding up a fifth over a tiny latch click (0.15 s) | `weapon-mine-arm-r28-a`, `weapon-mine-arm-r28-b` | not listened to by Claude; a mount drops up to two mines a second and keeps up to six, so the beep repeats often; b's longer ring may blur with the next mine's || **a** (user), the armed chirp; b moved to `design/audio/sfx/concept/rejected/` (`sfx_r28.py`'s `CHOSEN` writes it there); the game already played a (`Sfx.MINE_ARM`, a concept copy by `copyPlaceholderSounds`) |
| 13 | [Targeting computer](../../player/systems/README.md#targeting-computer-user-decision-d6-of-m5-part-a) | built (D6 = a). **HP bars**: 2 px under every damaged non-`tiny` enemy, the HUD's phosphor green on a dark trough, amber below 30 %, 1.5 s after the last hit and a 0.3 s fade; a Coilwyrm gets one bar under its foremost living member. **Brackets**: lime `B4FF3C`, a 2 Hz pulse between 60 and 100 %, on a boss's, mid-boss's, set piece's or `large` unit's parts with a damage multiplier while they can take damage (the Coilwyrm head included, not a regrown one). **+20 % homing turn**: the Micro-missile Pod, the Hornet and the Swivel's slew; not Rook's guns. **Question:** the design row is `draft`; approve these details? | `targeting-computer-capture-r28-a`, `part-a-capture-final-r28-a` | the bar and bracket sizes, colours, fade and the units that count are main-agent choices; bosses, mid-bosses and set pieces get no HP bar (the boss bar and the brackets cover them) || **approved** (user): built as described; the design row leaves `draft` for `approved` |
| 14 | [Salvage scanner](../../player/systems/README.md#salvage-scanner-user-decision-d7-of-m5-part-a) | built (D7 = a): **+10 % (L1) / +20 % (L2)** on every salvage pickup and every secret's hidden crate. It is not paid on bounties, objectives or the grade bonus. It is applied before the payout's one rounding, shows in the floating number and the debrief, and is **outside the budget** like the grade bonus. **Glint**: a sparkle (4 frames in 0.3 s, every 1.5 s) at the centre of every object that reveals a secret while it is on screen and unspent, also in the dark, at both levels. **Question:** the design row is `draft`; approve these details? | `targeting-computer-capture-r28-a` (the glint on the tow cable), `part-a-capture-final-r28-a` | the score counts the plain value, not the bonus; the glint's timing and the objects that glint are main-agent choices || **approved** (user): built as described; the design row leaves `draft` for `approved` |
| 15 | [Secret glint](../../player/systems/README.md#concept-art) | **production art, pick a or b** (`tools/art/secret_glint.py`, additive, 24 colours): **a** "star glint", 23×23, a four-point star with 10 px arms and a warm white core over a soft gold halo (rise, flare, twinkle, fade); **b** "scanner ping", 35×35, a thin gold ring expanding from the object (radius 3 to 15 px) with four lock marks and a white flash at the centre | `secret-glint-r28-a`, `secret-glint-r28-b` (png, gif) | a has the loot targets' four-point star shape, only bigger and warmer, so a secret and a loot target may look alike; b's thin ring is faint on a lit deck and may read as a targeting mark (the Targeting computer's brackets are lime, b is gold) || **a** (user), the star glint, final; b's review pair moved to `design/player/systems/concept/rejected/` (`secret_glint.py` writes it there); the game already played a (`glint-secret`); systems `art: final` |
| 16 | [Hangar tile abbreviations](../../ui/hangar/README.md#design) | the module tiles (48 px, at most 6 characters of the 8 px label font): `CMP I` / `CMP II` (Composite), `STRIKE` (Airstrike), `S-BOMB` (Smart Bomb), `FLARES`, `SENSOR`, `MAGNET`, `TARGET` (Targeting computer), `SALVGE` (Salvage scanner, chosen over `SCANNR`, which would read like the sensor suite) | `part-a-capture-final-r28-a` (the hangar panel) | the later acts' modules still fall back to a cut name (`CMPIII`, `EVASIV`, `AUTO-R`, `PRESSU`, `ASCEND`) until their acts give them abbreviations || **accepted** (user) |
| 17 | [Act HP factor and boss bounties](#act-hp-factor) | built (D5 = c): returning `medium` and larger units get HP × (reference DPS at the level ÷ at their first level) from Act 2 on (table under [Part A numbers](#act-hp-factor)); `tiny` and `small` keep their HP, and Act 1 is unchanged. **Boss-bounty rule**: boss and mid-boss bounties are planned as absolute credits, but their data holds Act 1 terms (absolute ÷ act factor). The code act-scales every payout with no exemption (the Harbour Kraken's 300 is written 188 and pays 301 in Act 2) | — | the enemies README's Decisions give the Coilwyrm as 113–151 HP (its 98 × the factor), but the code rounds each part, so it is 118–149 at medium; the Act 2 levels' balance is not flown yet (no Act 2 level exists) || **approved as built** (user): the act HP factor (D5 = c) and the boss-bounty rule |
| 18 | [Debug runs write no saves](../../systems/saves/README.md#decisions) | **for your information** (user decision, built): every debug run (`--level`, `--loadout`, `--special`, `--escort`, `--act-end`, `--start level`, `--bench`, `--invulnerable`, `--debug-speed`) has read-only save slots, so no autosave, failure, restart, act-end or manual save is written; the Save game screen says saving is off. The `--escort` option is never part of the campaign's gear | — | `--settings` and `--difficulty` alone are not debug options || **noted** (user) |
| 19 | [Coilwyrm head ×2](../../enemies/air/coilwyrm/README.md#decisions) | **for your information** (bug fix): the head's ×2 weak-point multiplier was in its data since Level 06 but never simulated. Now the head takes every damage ×2, so **a head-first kill on medium needs 20 points of hits instead of 40**; a regrown head stays ×1. The Targeting computer's brackets now show round a living head | — | Level 06's Coilwyrms die faster than in the reviewed Act 1 playthroughs; no test or balancing number moved (the head's HP, the bounties and the typical haul are unchanged) || **noted** (user) |
| 20 | [Part A decisions](#part-a-numbers-and-texts) | check that the user decisions listed under [Part A numbers and texts](#part-a-numbers-and-texts) were carried out | — | — || **checked** (user): every decision carried out |

## Part A numbers and texts

The numbers and texts the agents chose or measured to build part A (rows 3, 4, 13, 14 and 17). The
details live in the parts' READMEs (Decisions of 2026-10-06), and the design source is the gap
analysis `m5-gaps.md` (§6–8).

### Rook

From [wingmen](../../player/wingmen/README.md) and its [data.yaml](../../player/wingmen/data.yaml):

- **Ship:** armour 80, no shield, an 11×11 hit box on `air`, 250 px/s (full speed in 0.2 s). He
  keeps 40 px from the player, a 12 px play-field margin and a 14 px body clearance. Ramming an
  `air` enemy deals 20.
- **Formations** (glide 0.6 s): Wing (64, +28), Wide (120, +10), Trail (40, +90). He takes the
  mirrored slot while his own is outside the field, and returns 1 s after it is back inside.
- **Reactions:** a 0.25 s delay. A dodge check runs every 0.1 s with a 0.6 s look-ahead and 14 px
  clearance; he steps up to 48 px and reacts to 70 % of the bullets he predicts. His firing cone
  is 30° and 360 px. Target priority: the player's last hit within 1.0 s, then a flank threat
  within 160 px, then the nearest enemy.
- **Guns** (60 % of the base weapon's price and upgrade base):

| Gun | Based on | Scale | DPS L1 → L5 | Price | Upgrades L2 / L3 / L4 / L5 |
|---|---|---|---|---|---|
| Autocannon | [Autocannon Pod](../../player/weapons/autocannon-pod/README.md) | × 1.5 | 12 → 37 | free (fitted when he joins) | 150 / 300 / 600 / 1 200 |
| Scatter | [Scatter Vulcan](../../player/weapons/scatter-vulcan/README.md) | × 0.6 (volley) | 11 → 39 | 720 | 360 / 720 / 1 440 / 2 880 |
| Missiles | [Micro-missile Pod](../../player/weapons/micro-missile-pod/README.md) | × 1.5 | 12 → 42 | 480 | 240 / 480 / 960 / 1 920 |
| Mortar | [Hammer Mortar](../../player/weapons/hammer-mortar/README.md) | × 0.6 | 15 → 54 | 900 | 450 / 900 / 1 800 / 3 600 |

- **Repairs:** the difficulty's repair cost per point (free / 5 / 10 on easy / medium / hard). A
  launch warning shows below 50 % (under 40 of 80). He is grounded at 0 until any repair, and a
  retry gives him the player's 50 % armour floor.
- **Fire:** whenever the player fires. A headless Level 02 run (medium, 90 s) gives his L1
  Autocannon 425 volleys to the player's 850 (his L3 Missiles 255); before, with "only with a
  target in his cone", he fired 27.
- **Eject:** the pod moves at 120 px/s to the nearer side edge, or the other edge when the ship is
  on that side within 36 px of the pod's line. It is drawn over the `medium` explosion after 0.1 s.

#### Rook's wear

Measured headless (the test autopilot, the balance plan's fit, his L1 Autocannon, seeds 1–4), hits
on Rook and armour left, before → after the 70 % reaction share and the AI fixes, with the 0.4 s
look-ahead:

| Level | Difficulty | Hits before | Hits after (armour left) | Ejects after |
|---|---|---|---|---|
| 02 | medium | 0–2 | 2–3 (68–72) | none |
| 02 | hard | — | 3–5 (60–68) | none |
| 06 | medium | 0–5 | 0–4 (64–80) | none |
| 06 | hard | 1–7 | 4–9 (44–64) | none |
| 07 | medium | 3–8 (48–68) | 5–11 (36–60) | none |
| 07 | hard | 11–19 (2–36) | 10–20 (0–40) | 2 of 4 runs (3 of seeds 1–8) |

Other measurements:
- He was never closer than 40.0 px to the player.
- There was no body contact: every hit was a bullet, with the closest gap +2.9 px.
- Trail now shows on Level 06's rear wave (134 s) in every run.
- With the **0.6 s look-ahead** (user decision), hard Level 07 had **no eject in 8 runs** (armour
  4–50 left), and medium wear was about the same. Reacting to every bullet with the 0.4 s
  look-ahead still ejected him in 3 of 8 hard Level 07 runs.

### Targeting computer and Salvage scanner

From [ship systems](../../player/systems/README.md); both design rows are `draft`.

| Module | Levels | Price | Effect |
|---|---|---|---|
| Targeting computer | L1 | 3 000 | HP bars (2 px, green `40FF80`, amber `FFE04A` below 30 %, 1.5 s + 0.3 s fade), lime brackets `B4FF3C` (2 Hz, 60–100 %, each arm a quarter of the part's box, 2 px outside it), homing turn +20 % (Swivel slew 360 → 432°/s); in the shop from the L07 visit with the L06 data core, otherwise L08 |
| Salvage scanner | L1–L2 | 2 500 / 6 000 | +10 / +20 % on salvage pickups and hidden crates, outside the budget; the glint on a secret's objects (4 frames in 0.3 s every 1.5 s) |

### Act HP factor

From [enemies](../../enemies/README.md#balancing-basis): HP at medium with the factor (reference
DPS at the level ÷ at the unit's first level), each part rounded half to even; Act 1's levels and
`tiny` / `small` units unchanged.

| Unit (first level, base HP) | L08 | L09 | L10 | L11 | L12 | L13 | L14 |
|---|---|---|---|---|---|---|---|
| Spore Bomber (L03, 24) | 45 | 47 | 50 | 52 | 55 | 57 | 60 |
| Brood Pod (L04, 30) | 47 | 50 | 52 | 55 | 58 | 60 | 63 |
| Scuttler (L04, 28) | 44 | 46 | 49 | 52 | 54 | 56 | 59 |
| Mantis (L06, 26) | 30 | 31 | 33 | 35 | 36 | 38 | 40 |
| Coilwyrm head / segment / tail (L06, 40 / 4 / 10) | 46 / 5 / 12 | 48 / 5 / 12 | 51 / 5 / 13 | 54 / 5 / 13 | 56 / 6 / 14 | 58 / 6 / 15 | 62 / 6 / 15 |
| factor at L03 / L04 / L06 units | 1.88 / 1.58 / 1.15 | 1.97 / 1.66 / 1.21 | 2.06 / 1.74 / 1.27 | 2.19 / 1.84 / 1.35 | 2.28 / 1.92 / 1.40 | 2.38 / 2.00 / 1.46 | 2.50 / 2.11 / 1.54 |

Which units actually return in which Act 2 level is decided by the level parts; the table shows
what the rule gives. **Boss bounties:** data in Act 1 terms (absolute ÷ 1.6 in Act 2), the payout
act-scaled like every bounty: the Harbour Kraken's planned 300 is written 188 and pays 301.
`BalanceTest` and `tools/balance.py` check a boss's share with the act factor at its level.

### Hangar tile abbreviations

`CMP I`, `CMP II`, `STRIKE`, `S-BOMB`, `FLARES`, `SENSOR`, `MAGNET`, `TARGET`, `SALVGE`. A
model with a nickname keeps its model (`MK II`); `STD` for standard parts. Every Act 1–2 module
fits by its model or an abbreviation (`LoadoutTilesLayoutTest`).

### User decisions of part A (check they were carried out)

- [x] **D1 = a**: Rook and the escort slot are built, with the five Act 2 weapons that need no water
  (Tail Gun, Fan Blaster, Hornet Launcher and Swivel Gun with their own effects, the Proximity Mines
  with their delivery) and the Targeting computer and the Salvage scanner. The Torpedo Pod and the
  `sub` layer are tagged **later: M5 part E** ([weapons](../../player/weapons/README.md#implementation)).
- [x] **D2 = a**: Rook's kills pay like the player's: bounty, score, chain and objectives
  (`WingmanTest.hisKillsPayLikeThePlayersAndChain`).
- [x] **D3 = b**: a hangar `ROOK` repair line like the player's; unrepaired armour stays missing; a
  launch warning below 50 % (row 4).
- [x] **D4 = a**: an escort inventory: buy at 60 %, upgrade each gun L1–L5, fit one, sell at the
  sell-back share, all in the visit's undo (row 4).
- [x] **D5 = c**: the act HP factor only for returning `medium`+ units from Act 2 on (row 17).
- [x] **D6 = a**: the Targeting computer's fading HP bars, lime brackets and +20 % homing turn
  (row 13).
- [x] **D7 = a**: the Salvage scanner's +10 / +20 % and the glint on a secret's objects (rows 14
  and 15).
- [x] **Rook's four rules** (accepted 2026-10-06):
  - his repair costs the difficulty's repair cost per point;
  - he is grounded at 0 armour until any repair;
  - a retry gives him the player's 50 % armour floor;
  - a new campaign puts him on the left.
- [x] **Fire whenever the player fires**, at his gun's rate, target or not
  (`WingmanTest.heFiresWheneverThePlayerFiresEvenWithoutATargetAtHisGunsRate`).
- [x] **His side is outside the visit's undo**
  (`EscortTest.hisSideIsAHangarSettingOutsideTheUndo`).
- [x] **No debug saves**: every debug run has read-only save slots, and `--escort` is never saved
  (`EscortTest.theEscortDebugOptionIsNeverSavedNorKept`; row 18).
- [x] **70 % dodge**: `dodge.reacts: 0.7`, each bullet decided once by his own seeded generator
  (`WingmanTest.heReactsToHisShareOfTheBulletsEachDecidedOnceTheSameForTheSameSeed`).
- [x] **0.6 s look-ahead**: `dodge.look_ahead: 0.6` (was 0.4).

## Listening

Rook's 27 radio barks (choice 6), from `assets/voice/rook/`, in priority order. The game plays
the *n*-th bark of a trigger in an attempt as variant (level number + *n*) mod the trigger's
count. The mine's arming beeps (choice 12) are on the board's cards.

| Group | Cue | Speaker | Text or sound | File |
|---|---|---|---|---|
| 1 Boss warning (grim) | An act boss's warning or a mid-boss's sting | Rook | "Here comes the big one." | [play](../../../assets/voice/rook/9708c4030b81.ogg) |
| 1 Boss warning (grim) | An act boss's warning or a mid-boss's sting | Rook | "Heads up. Something nasty's coming." | [play](../../../assets/voice/rook/ff60b82841f2.ogg) |
| 1 Boss warning (grim) | An act boss's warning or a mid-boss's sting | Rook | "Oh, I don't like the look of that." | [play](../../../assets/voice/rook/ef2ae7fc97c1.ogg) |
| 1 Boss warning (grim) | An act boss's warning or a mid-boss's sting | Rook | "Big contact inbound. Stay loose, Lancer." | [play](../../../assets/voice/rook/2a773dd9aa27.ogg) |
| 2 Rear wave (shouted) | 1.5 s before a `rear` wave enters | Rook | "Six o'clock, Lancer!" | [play](../../../assets/voice/rook/cec612a57887.ogg) |
| 2 Rear wave (shouted) | 1.5 s before a `rear` wave enters | Rook | "Contacts on six! Why is it always six?" | [play](../../../assets/voice/rook/558c9f75a670.ogg) |
| 2 Rear wave (shouted) | 1.5 s before a `rear` wave enters | Rook | "Check six, check six!" | [play](../../../assets/voice/rook/9cf9518f8d72.ogg) |
| 2 Rear wave (shouted) | 1.5 s before a `rear` wave enters | Rook | "Behind us! They're on our tails!" | [play](../../../assets/voice/rook/a2bed05c7f96.ogg) |
| 3 Sides wave (shouted) | A `sides` wave enters | Rook | "Contacts on your flank!" | [play](../../../assets/voice/rook/707e28107b07.ogg) |
| 3 Sides wave (shouted) | A `sides` wave enters | Rook | "Bandits coming in from the side!" | [play](../../../assets/voice/rook/5391ce8c3b99.ogg) |
| 3 Sides wave (shouted) | A `sides` wave enters | Rook | "Flank! Watch the edges, Lancer!" (pinned seed) | [play](../../../assets/voice/rook/6dbea988135b.ogg) |
| 4 Player's armour (grim) | The player's armour falls below 30 % | Rook | "You're smoking, Lancer. Ease off!" | [play](../../../assets/voice/rook/98af1c270715.ogg) |
| 4 Player's armour (grim) | The player's armour falls below 30 % | Rook | "That hull won't take much more, Lancer!" | [play](../../../assets/voice/rook/887671642c58.ogg) |
| 4 Player's armour (grim) | The player's armour falls below 30 % | Rook | "Lancer, you're coming apart. Get clear!" | [play](../../../assets/voice/rook/48396442d3dd.ogg) |
| 5 Rook's armour (grim) | His armour falls below 30 %, once per attempt | Rook | "Taking a beating over here!" | [play](../../../assets/voice/rook/fdface1f26cc.ogg) |
| 5 Rook's armour (grim) | His armour falls below 30 %, once per attempt | Rook | "I'm hit. I'm okay. I'm mostly okay!" | [play](../../../assets/voice/rook/6c9a04c3ad9e.ogg) |
| 5 Rook's armour (grim) | His armour falls below 30 %, once per attempt | Rook | "Rook's hurting, Lancer. Hurting bad." (pinned seed) | [play](../../../assets/voice/rook/f398f9f266a7.ogg) |
| 6 Eject (shouted, urgent) | Rook ejects | Rook | "Punching out! Give 'em hell!" | [play](../../../assets/voice/rook/306cc55c74b2.ogg) |
| 6 Eject (shouted, urgent) | Rook ejects | Rook | "I'm out, I'm out! She's all yours, Lancer!" | [play](../../../assets/voice/rook/7fc6eecb398d.ogg) |
| 6 Eject (shouted, urgent) | Rook ejects | Rook | "Ejecting! Somebody owes me a ship." | [play](../../../assets/voice/rook/97ff5d44865f.ogg) |
| 7 Kill streak (fierce) | 10 kills (the player's and his) within 5 s | Rook | "Nice shooting!" | [play](../../../assets/voice/rook/c5d53f9f111c.ogg) |
| 7 Kill streak (fierce) | 10 kills (the player's and his) within 5 s | Rook | "Ha! Save some for me!" | [play](../../../assets/voice/rook/9cdf9f91f020.ogg) |
| 7 Kill streak (fierce) | 10 kills (the player's and his) within 5 s | Rook | "Now that's how it's done." | [play](../../../assets/voice/rook/2c2fe93fa02b.ogg) |
| 7 Kill streak (fierce) | 10 kills (the player's and his) within 5 s | Rook | "Look at you go, Lancer." | [play](../../../assets/voice/rook/045c5f533c40.ogg) |
| 8 Overdrive (fierce) | An overdrive pickup appears | Rook | "Power-up! Grab it!" (pinned seed) | [play](../../../assets/voice/rook/e67f76d7e9c7.ogg) |
| 8 Overdrive (fierce) | An overdrive pickup appears | Rook | "Ooh, shiny. Grab that." | [play](../../../assets/voice/rook/a6bd9428b54b.ogg) |
| 8 Overdrive (fierce) | An overdrive pickup appears | Rook | "Overdrive's up for grabs, Lancer!" (pinned seed) | [play](../../../assets/voice/rook/31edae4eee56.ogg) |

## Notes

- Review file names: `<subject>-final-r28-a.png/.gif` (production art), `<subject>-capture-r28-a/b`
  (game captures) and `<subject>-r28-a/b` (the two a/b pairs), in each part's `concept/`. Their
  `prompts.md` entries say how they are made. Nothing in `assets/` is hand-edited.
- The board shows the video `part-a-capture-final-r28-a.mp4` as a link ("open").
- The voice files above were matched to their lines by the voice key (the SHA-256 of the line's
  voice, settings and spoken text, as `VoiceLines.key` computes it). The four pinned takes carry
  their seeds in [voice data](../../audio/voice/data.yaml).
- The captures of choice 2 were taken with scratch data placed only on the capture's classpath; the
  real [data.yaml](../../player/wingmen/data.yaml) is unchanged.
- After the choices (done at the close):
  - the approved parts got `art: final` (wingmen, the five Act 2 weapons, ship systems; the HUD's
    was already final);
  - the chosen beep and glint are option a, which the game already played; the rejected b files
    moved to `concept/rejected/`, where their generators now write them;
  - the Targeting computer's and the Salvage scanner's rows left `draft` for `approved`;
  - the flank question is answered in wingmen (the data comment already said plain distance);
  - the barks' voice item is ticked in wingmen and in voice.

## Decisions

- 2026-10-06: Opened with M5 part A.
- 2026-10-06: Closed (user): **everything accepted**, with the mine's arming beep **a** (the armed
  chirp; b, the sensor ping, rejected) and the secret glint **a** (the star glint; b, the scanner
  ping, rejected). Rook's sprites, the five Act 2 weapons' effects and the glint approved as final;
  Rook in the game, his AI, the escort UI, the HUD escort box, the Targeting computer and the
  Salvage scanner approved as built, both modules' design rows `approved`; the flank threat
  confirmed as plain distance (160 px from his centre); the 27 barks accepted as rendered; the tile
  abbreviations, the act HP factor table and the boss-bounty rule accepted; the debug-saves and
  Coilwyrm-head notes taken; the part A decision checklist checked. The rejected files are in
  `design/audio/sfx/concept/rejected/` and `design/player/systems/concept/rejected/`; the game
  plays the chosen ones (it already played option a of both). M5 part A is done; part B (Level 08
  and the Act 2 intro) is next.
