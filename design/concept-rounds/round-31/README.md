---
title: Concept round 31 — M5 part C, Level 09 Arcology Fall
design: approved
implementation: n/a
art: chosen
depends-on: [../../campaign/act-2-homefront/level-09-arcology-fall, ../../enemies/ground/hive-node, ../../enemies/ground/ravager, ../../ui/briefing, ../../ui/hangar, ../../audio/music, ../../audio/sfx, ../../audio/voice, ../../player/wingmen, ../../art-direction/production]
updated: 2026-10-07
---

# Concept round 31 — M5 part C, Level 09 Arcology Fall

**Closed 2026-10-07** (user): the production art approved as final, the collapse sound **a**,
both Ravager pounces kept (picked at random), the Kilo Lead **a**; the capture, voiced lines,
texts, numbers, decisions and build choices accepted; the collapse's look **c** (after a and b
were rejected) approved with two tweaks and built in the game, hold C lasting until the dust
settles. The outcome of each row is in the *Choices* table.

## Summary

The review round of M5 part C ([roadmap](../../tech/roadmap/README.md#m5-parts)): Level 09
*Arcology Fall*, the first `destroy-targets` level of Act 2. The production art went straight to
production (D9 = a) and is reviewed here as **final**:
- the Hive Node, with its creep, death, stump and intel portrait;
- the Ravager, with its gallop, leap and shadow, death and intel portrait;
- Level 09's arcology district backdrop in the last hour of the night, with the Kilo trucks and the
  creep cocoon;
- the two briefing images;
- "Firestorm" (track 7) and its base stem.

Four a/b picks are new: the look of the arcology's collapse, the collapse sound, the Ravager's
pounce sound, and the Kilo Lead's voice. The round also holds:
- the game capture of Level 09, with its readability strip;
- a listen to the 20 new voiced lines;
- the new texts (Level 09's document is `design: review`), quoted in full;
- the numbers the agents chose or measured;
- a checklist of the user's part C decisions;
- six choices made during the build, to confirm.

Open [index.html](index.html) in a browser (regenerate with `python3 tools/concept/board.py 31`).

Every art row is *approve as final* or *redo* (say what to change). An a/b row is *pick a or b*.
A built row is *approve as built* or *change* (say what). A row with a **question** needs an answer
too. Only an approved part gets `art: final`.

In the game (`./gradlew :desktop:run --args="…"`):
- Level 09 with the capture's fit (the balance plan's anti-ground: Bomb Racks and Rook's Mortar):
  `--level 9 --escort rook:mortar:2 --loadout front=pulse-cannon:3,left=bomb-rack:2,right=bomb-rack:2 --special airstrike:2`.
  Until row 4's look is approved the collapse plays a neutral fall (a plain dust-wall front); the
  collapse sound a, the two pounce sounds and the Kilo Lead's voiced lines are in since the
  2026-10-07 decisions.
- A missed node: the same without Rook's Mortar and the Bomb Racks (`--escort none --loadout
  front=pulse-cannon:3`): the first node that leaves the screen alive fails the mission, with
  Okafor's `{group}` line on the mission failed screen.
- The way in: `--level 8 --invulnerable`. Winning it plays the debrief, Level 09's two briefing
  pages and the hangar with Rook's teaser, the intel panel (at each sensor level) and, launching
  without an anti-ground source, the `NO ANTI-GROUND SOURCE FITTED` warning.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Hive Node](../../enemies/ground/hive-node/README.md#concept-art) | production art (`tools/art/hive_node.py`, straight from the chosen round-06 model, no a/b): `hive-node_0..23` (76×76, radial: 6 iris states × 4 lobe-pulse frames); the additive **glow** masks with a teal iris halo that grows as it opens (the spawn's telegraph, drawn above low-air); the **creep** patch `hive-node-creep_0..8` (192×192: frame 0 alive, 1–8 the 2 s wither after its death); the 16-frame **death** and `-tatters` (128×128, played with the `large` burst); the **stump** left behind. Night readability lift as the Creeper's in round 30 (albedo ×1.45, the 1 px lavender-white rim). The 30×30 **intel portrait** (`tools/art/intel.py`) | [hive-node-final-r31-a.png](../../enemies/ground/hive-node/concept/hive-node-final-r31-a.png), [.gif](../../enemies/ground/hive-node/concept/hive-node-final-r31-a.gif), [intel-final-r31-a.png](../../ui/hangar/concept/intel-final-r31-a.png) | night contrast **3.5** against the Creeper's 5 after its lift; the stump may read like a live node; the creep is a static 2D patch (it neither pulses nor spreads); the iris and spawn sounds are reused (row 8) | **approved as final** (user): `hive-node_0..23`, the glow masks, the creep and its wither, the `large` death with its tatters, the stump and the intel portrait; the weak spots as they are; Hive Node `art: final`, `done`, its numbers and readings accepted (row 13), so `design: approved` |
| 2 | [Ravager](../../enemies/ground/ravager/README.md#concept-art) | production art (`tools/art/ravager.py`, straight from the chosen round-05 model): `ravager_0..127` (56×56, 16 headings × 8 gallop frames, 46 px of ground per cycle); maw and eye glow masks; the **leap** frames `ravager-leap_0..63` and `-leap-glow` (80×80, 16 headings × 4 lift steps, 1.11–1.43×, no scaling at run time); the shadow sliding away from it during the leap; the 10-frame `small` **death** and `-tatters` (80×80, no husk). The 30×30 **intel portrait** | [ravager-final-r31-a.png](../../enemies/ground/ravager/concept/ravager-final-r31-a.png), [.gif](../../enemies/ground/ravager/concept/ravager-final-r31-a.gif), [intel-final-r31-a.png](../../ui/hangar/concept/intel-final-r31-a.png) | the leap uses the **same pose at all four lift steps** (only the scale changes); the leap shadow is faint at 1:1 (the readability strip); night contrast 3.1 | **approved as final** (user): `ravager_0..127`, the glow masks, the leap frames and their shadow, the `small` death with its tatters and the intel portrait; the weak spots as they are; Ravager `art: final`, `done`, its numbers and readings accepted (row 13), so `design: approved` |
| 3 | [Level 09 backdrop](../../campaign/act-2-homefront/level-09-arcology-fall/README.md#layout) | production art (`tools/art/backdrop_l09.py` on Level 08's kit, its block merged into the level's data, 653 placements): the arcology district in the **last hour of the night** (D9 = a; a grey glow in the east, darks lifted toward a cool grey, fewer lit windows). Five tile sets: the ruined elevated highway, Unity Plaza with the dry fountain and the creep of nodes A1 and A2, the boulevard with the burnt-out tram, the lagoon with the Okonjo Bridge and the **Kilo trucks** (a CDF truck at 16 headings, `tools/concept/props_r31.py`, on the real clock), the Ndidi lobby plaza. Perspective towers (city, creep-veined, burnt), two smaller arcologies, the **Ndidi Arcology** hollowed by creep (a ground tower of height 1.5), the creep **cocoon** on its low roof (`assets/sprites/cocoon*`), fires and smoke plumes | [backdrop-final-r31-a.png](../../campaign/act-2-homefront/level-09-arcology-fall/concept/backdrop-final-r31-a.png), [.gif](../../campaign/act-2-homefront/level-09-arcology-fall/concept/backdrop-final-r31-a.gif) (the generator's block: [backdrop-proposal.yaml](../../campaign/act-2-homefront/level-09-arcology-fall/concept/backdrop-proposal.yaml)) | the glow in the east is subtle; **two creep hues**: violet on the towers (as Level 08's arcology), the Hive Node's teal-black on the ground; the Kilo trucks are small and dark (strip, row 10); the bridgehead ramp is dark; no bank drifts, so the trucks and the fires are the only motion; the rubble piece is provisional until row 4 | **approved as final** (user): the backdrop with the Kilo trucks and the cocoon, the weak spots as they are; Level 09 stays `art: chosen` until row 4's look is approved (the rubble piece with it) |
| 4 | [The collapse look](../../campaign/act-2-homefront/level-09-arcology-fall/README.md#layout) | **rework, approve c or say what to change** (`tools/concept/collapse_r31.py c`): the 1.5 s warning is the arcology's sway (it shudders and bends a few growing degrees, debris and dust trickle off it, its own cast shadow moving with it); then it collapses straight down in 1.5 s, its shadow shortening right to left; dust billows from its base 0.35 s before the impact (3.0 s after the warning starts), a large dust blast rolls out in every direction at the impact and settles over 6 s onto a rubble heap. The kill ring (sheet only) spreads from the foot over 1 s. Earlier: **concepts, pick a or b** (`tools/concept/collapse_r31.py`), both after the same 1.5 s warning (the tower shudders, its shadow sweeps the band) and with the same 6 s `heavy` dust peak. **a** "topples across": the arcology tips over its right foot edge and falls across the field, drawn by the tower projection as a box rotated about its hinge (its tip's ground x runs with the kill band over 3 s), a dust burst along the impact line, the crushed body left lying across. **b** "pancakes into a dust wave": the floors give way and the tower sinks into itself in 1.2 s, a cloud boils out of its foot and a dust wave rolls left to right along the band in 3 s (its front is the kill band), leaving a heap and debris along the band. Until the pick the game draws a **neutral fall** (`NeutralFall`) | [collapse-r31-c.png](../../campaign/act-2-homefront/level-09-arcology-fall/concept/collapse-r31-c.png), [.gif](../../campaign/act-2-homefront/level-09-arcology-fall/concept/collapse-r31-c.gif); rejected: [collapse-r31-a.png](../../campaign/act-2-homefront/level-09-arcology-fall/concept/rejected/collapse-r31-a.png), [.gif](../../campaign/act-2-homefront/level-09-arcology-fall/concept/rejected/collapse-r31-a.gif); [collapse-r31-b.png](../../campaign/act-2-homefront/level-09-arcology-fall/concept/rejected/collapse-r31-b.png), [.gif](../../campaign/act-2-homefront/level-09-arcology-fall/concept/rejected/collapse-r31-b.gif) | c: the production towers cast no shadow, so the arcology's would be the only one (it can be left out if it looks odd); the game's tower projection draws the wrong east/west wall for a tower left of the centre (found while drawing c; to fix with the production); the dust is blobby in both; the rubble is provisional; a needs the tower projection to draw a tilted box and a dust-burst sheet; b needs an animated tower height, a foot-cloud sheet, a dust-wave sheet, a heap and a debris strip; a late cluster C kill plays the collapse low on the screen (row 10) | **c** approved (user) with two tweaks (the shadow halved; a single lean to the right instead of a sway), built in the game ([collapse-capture-final-r31-a](../../campaign/act-2-homefront/level-09-arcology-fall/concept/collapse-capture-final-r31-a.png)); hold C lasts until the dust settles. a and b rejected earlier (user): moved to `concept/rejected/`; the kills spread outward from the tower's foot at the impact over 1 s |
| 5 | [Level 09 briefing images](../../ui/briefing/README.md#concept-art) | production art (`tools/art/briefing_images.py`, 672×240, one per page). **Okafor's page** `level-09-arcology-district`: the route over the district before dawn (the elevated highway, Unity Plaza with cluster A, the boulevard, the Lagos Lagoon and the Okonjo Bridge with the Kilo convoy, cluster B at the bridgehead, the Ndidi Arcology with cluster C), Lancer and Rook at the start. **Varga's page** `level-09-node-scan`: the production Hive Node with its iris shut and open, the hardened marker and the anti-ground sources; the production Ravager on its pounce arc at the ship, the arc's middle 0.3 s red (the air window) | [briefing-images-final-r31-a.png](../../ui/briefing/concept/briefing-images-final-r31-a.png) | the district map's corner label now reads **"LAST HOUR OF NIGHT"** (was "FIRST LIGHT IN THE EAST", which contradicted D9: first light is Level 10's) and ELEVATED HIGHWAY moved below the highway, clear of the ROOK label; the cluster letters A, B, C are small; the numbers on the images (60 civilians, 200 px, every 4 s) are the level's | **approved as final** (user): both images, the weak spots as they are |
| 6 | ["Firestorm" final and base stem](../../audio/music/README.md#concept-art) | production files (`tools/art/themes.py firestorm firestorm-base`). Track 7 is the chosen `act2-b-theme-r08-a` rendered again with a `SOURCE` comment: 120.4 s, loop 7.29 s + 109.71 s, −14.0 LUFS, −1.3 dBTP, seam 0.63. The **base stem** comes from a new frozen generator (`music_r31.py`, the round-11 method: no lead, brass, stab riff, melody strings, breakbeat top, toms or crashes), mastered linked to the full mix: −17.7 LUFS (−3.6 LU), −1.4 dBTP, seam 0.52. Two seam aids. Level 09 plays the base stem from the launch and the full mix in each hold and from the collapse (`full_on`) | [firestorm-final-r31-a.ogg](../../audio/music/concept/firestorm-final-r31-a.ogg), [firestorm-base-final-r31-a.ogg](../../audio/music/concept/firestorm-base-final-r31-a.ogg), [firestorm-seam-final-r31-a.ogg](../../audio/music/concept/firestorm-seam-final-r31-a.ogg), [firestorm-base-seam-final-r31-a.ogg](../../audio/music/concept/firestorm-base-seam-final-r31-a.ogg), [music-final-r31-a.png](../../audio/music/concept/music-final-r31-a.png) | measured only, not listened to; the base stem had no concept round; its sections A and D are thin; the chosen mix failed the true-peak limit (−0.2 dBTP), so both files got Afterburner's transient dip (**8 dips**, at most −1.32 dB, 433 ms of 120 s) | **approved as final** (user): track 7 and its base stem (the base stem without a concept round of its own, its thin sections A and D and the 8 dips as they are); music `done` |
| 7 | [Collapse sound](../../audio/sfx/README.md#concept-art) | **concepts, pick a or b** (`tools/concept/audio/sfx_r31.py`, from Freesound originals): a rumble under the 1.5 s warning swelling into a long crash. **a** "Cracking Earthquake" by uagadugu (CC0) + "Big crash, a house tumbling down" by YleArkisto (**CC-BY 4.0**): a stony rumble into a tumbling masonry crash (5.5 s). **b** "Radiator Metal Rumbling" by RutgerMuller (CC0) + "Explosion or Collapse.wav" by tec_studio (CC0): a steel-frame rattle into a deep rolling cave-in with a long dust tail (5.7 s). The game plays a placeholder (hit-crumble a) from the fall; the pick moves to the shadow's start | [collapse-r31-a.ogg](../../audio/sfx/concept/collapse-r31-a.ogg), [collapse-r31-b.ogg](../../audio/sfx/concept/rejected/collapse-r31-b.ogg) | **a** is CC-BY: if picked, YleArkisto goes on the in-game credits screen; **b**'s warning rumble is quiet; both are sub-heavy; not listened to | **a** (user), CC-BY 4.0 (YleArkisto on the in-game credits roll): produced at the close (`assets/sfx/collapse-r31-a.ogg`, `tools/art/sfx_originals.py` with `sfx_r31.py`'s `PRODUCTION`), played from the shadow's start (the crash at 1.5 s; it moves to the impact with row 4's rework); b moved to `concept/rejected/` |
| 8 | [Ravager pounce sound](../../audio/sfx/README.md#enemies) | **concepts, pick a or b** (`sfx_r31.py`), played at the take-off. **a** "Goblin Snarl" by qubodup (CC0): a rasping dog-like attack snarl over a quick swish (0.7 s). **b** "Dragon: Snarl, Roar + Attack" by Breviceps (CC0): a snapping attack roar, sped up to hound size, over a heavier whoosh (0.7 s). The game plays a placeholder (carrier launch b). **Confirm** too: the Hive Node reuses the Brood Carrier's iris b as its iris opens and the Vrell spawn a as it releases, and neither unit screeches on entry | [ravager-pounce-r31-a.ogg](../../audio/sfx/concept/ravager-pounce-r31-a.ogg), [ravager-pounce-r31-b.ogg](../../audio/sfx/concept/ravager-pounce-r31-b.ogg); reused: [enemy-carrier-iris-r25-b.ogg](../../audio/sfx/concept/enemy-carrier-iris-r25-b.ogg), [enemy-spawn-r08-a.ogg](../../audio/sfx/concept/enemy-spawn-r08-a.ogg) | not listened to; b is the heavier of the two, a sped-up dragon roar | **a and b both kept** (user): the game picks one at random per pounce (`Sfx.RAVAGER_POUNCE_A` / `_B`), both produced at the close; the Hive Node's reuse of the carrier iris b and the Vrell spawn a, and no screech for either unit, **confirmed** |
| 9 | [Kilo Lead voice](../../audio/voice/README.md#concept-art) | **audition, pick a or b** (`tools/concept/audio/tts_r31.py`, Chatterbox, radio filter b, neutral): the CDF officer of the Kilo truck convoy, both his lines 1 s apart (t=95 and the secondary's thanks). **a** Aaron Bennett (LibriVox, public domain, clip 129 Hz; 11.5 s); **b** tombooker (LibriVox, public domain, 101 Hz; 14.3 s, slower). Until the pick his speaker row is `uncast` and his lines play as text | [voice-kilo-lead-r31-a.ogg](../../audio/voice/concept/voice-kilo-lead-r31-a.ogg), [voice-kilo-lead-r31-b.ogg](../../audio/voice/concept/rejected/voice-kilo-lead-r31-b.ogg); clip a, now [ref-kilo-lead.wav](../../audio/voice/refs/ref-kilo-lead.wav) (b's deleted) | Whisper reads a back word for word but for "and the" for "on the"; it hears b's **"Aegis" as "ages"** in both lines (also in the raw take) and "Conboy"; b's pitch is near Hammer Lead's (99 Hz); "Okonjo" reads "Okanjo" in all four (an unknown name); measured only, not listened to | **a** (user), Aaron Bennett, neutral as auditioned: his clip renamed `refs/ref-kilo-lead.wav`, his two lines voiced (no pins needed), `AUDITIONING` emptied; b moved to `concept/rejected/`, its clip deleted with its CREDITS.md row |
| 10 | [Level 09 in the game](../../campaign/act-2-homefront/level-09-arcology-fall/README.md#concept-art) | game captures. **Level 09** at medium with Rook (Mortar L2), a Bomb Rack on each wing, `--invulnerable`, flown by a key script: the holds, the nodes' iris and releases, a Ravager pounce mid-air, Rook's mortar on the nodes, the tracker, the cocoon under the bombs and its crate, the bridge with the Kilo trucks, the collapse warning, the (neutral) fall, the dust and rubble, the debrief (180 / 253 kills, secret 1 / 1, secondary met, grade A, 1,781 credits); 12 panels and the whole run with sound (233 s). **The readability strip**: the node with its iris open, a galloping Ravager, a Ravager in its leap with its shadow, the cocoon and a Kilo truck, at 1:1 and 2× | [level-09-capture-final-r31-a.png](../../campaign/act-2-homefront/level-09-arcology-fall/concept/level-09-capture-final-r31-a.png), [.mp4](../../campaign/act-2-homefront/level-09-arcology-fall/concept/level-09-capture-final-r31-a.mp4), [level-09-readability-r31-a.png](../../campaign/act-2-homefront/level-09-arcology-fall/concept/level-09-readability-r31-a.png) | the capture **predates the dust fade-out fix**: panel 11 shows the dust still at its peak at t 205, which never cleared then (fixed since: the dust ramps out over 6 s); the collapse is the neutral fall and its sound the placeholder; a **late cluster C kill** plays the collapse low on the screen (here within the bottom 170 px); the trucks are dark; the leap shadow is faint at 1:1; `--invulnerable` keeps a missed node from failing, so the fail is not captured | **accepted** (user): the captures as taken, the fixes after them as built |
| 11 | [Level 09's lines voiced](../../audio/voice/README.md) | **listening row**: 20 new lines rendered with the cast voices (`tools/art/voice.py`): the two briefing pages (dry), the radio lines with the `hold-start`, `first-kill`, `first-pounce`, `collapse` and level-end lines, the cocoon's line, and the mission failed line once per node, A1–C2 (Okafor 11, Varga 5, Rook 4). Quoted under [Listening](#listening). Accept as rendered, or name the lines to re-render | the files under [Listening](#listening) | listen to **Okafor's six failed-node lines**: their seeds ran "you, Lancer" into "you'll answer", so A1, A2 and C1 are pinned, and **A1** never read back clean ("U Lancer"); **Varga's page 2** for "in packs" (pinned; the key's seed read "the streets and packs"); **"arcology"**, "Vrell" and "Airstrike" read back only with a name prompt; the Kilo Lead's two lines wait for row 9; the teaser and intel lines are not voiced, by design; reviewed by Whisper only | **accepted as rendered** (user): all 20 lines, the four pinned takes included; none to re-render |
| 12 | [Texts for review](#texts-for-review) | **text review** (Level 09's document is `design: review`): the two briefing pages, Rook's hangar teaser, Varga's four intel lines, the radio script with the Kilo Lead's two lines, and the secondary's name **"Hold the bridge"**, all quoted under [Texts for review](#texts-for-review). Approve, or say what to change | — | changing a voiced text means a re-render (row 11); "sixty civilians" and the cluster names are new facts; the level-end line's "first light" hands over to Level 10 | **approved** (user): every text as quoted; Level 09's document leaves `review` for `approved` |
| 13 | [Part C numbers](#part-c-numbers) | accept or change the numbers listed under [Part C numbers](#part-c-numbers): density, pacing, the credit budget and `bounty_scale`, the holds' speeds and windows, the Hive Node's and the Ravager's numbers, the anti-ground time to kill against the windows, the plan's L09 and L10 rows, the autopilot's grades, the Ravager's time-to-kill exception and the capture run | — | the density is **83.9 a minute** against the design's ≈ 62 (the pacing rule in real seconds needed more Skitters after the holds); hard's anti-ground needs 0.73 of its window (accepted: the check is medium only); the autopilot's hard run misses the bridge secondary (B); in the capture **Rook's Mortar killed 4 of 6 nodes** | **accepted** (user) |
| 14 | [Part C decisions](#user-decisions-of-part-c-check-they-were-carried-out) | check that the user decisions D1–D11 and the stated defaults listed under [Part C numbers and texts](#user-decisions-of-part-c-check-they-were-carried-out) were carried out | — | D9 and D11 are carried out as far as part C goes before this round: the collapse look, the two sounds and the Kilo Lead's voice wait for rows 4, 7, 8 and 9; the Airstrike charge's dropper changed from the draft's turret pair (see the checklist) | **checked** (user): every decision carried out (the collapse's look as far as row 4 goes) |
| 15 | [Choices made during the build](#choices-made-during-the-build) | **confirm or change**, each: (a) hold C lasts until the collapse's fall ends; (b) Rook with a lobbed gun picks ground targets anywhere ahead within 200 px, not only in his 30° cone; (c) the pounce's overshoot lands on the play field (clamped); (d) the speaker name "Kilo Lead" with the generic CDF portrait; (e) the atlas rule D10 as built; (f) a `destroy-targets` primary pays no bonus. Details under [Choices made during the build](#choices-made-during-the-build) | — | (a) was the main agent's default; (b) goes beyond the brief the user gave (aim the lobbed gun) | **confirmed** (user): (a)–(f) as built |

## Part C numbers and texts

The numbers and texts the agents chose or measured to build part C (rows 12–15). The details are in
the parts' READMEs (Decisions of 2026-10-07). The design source is the gap analysis of part C (D1–D11
of 2026-10-07).

### Texts for review

From [Level 09's data](../../campaign/act-2-homefront/level-09-arcology-fall/data.yaml).

**Level 09 briefing**, two pages, one screen each
([Level 09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md#briefing)):

> **Commander Okafor** (`level-09-arcology-district`): "Overnight the Vrell grew six hive nodes in
> the arcology district, Lancer: three clusters, A, B and C. Each one breeds swarmers, and their
> creep eats the towers. Destroy all six. Leave one alive and we lose the district."
>
> **Dr. Varga** (`level-09-node-scan`): "The nodes are hardened: only bombs, mortars or the
> Airstrike crack them, Rook's mortar too. And something new hunts the streets in packs. Fast, low,
> and it leaps at you. Shoot it before it jumps."

**Hangar teaser** (Rook, `LT. K. TANAKA / AEGIS TWO`, not voiced): "Hardened nodes ahead, Lancer.
Fit something that hits the ground, or fit me the mortar."

**Varga's intel lines** (one per sensor-suite level, `IntelPanelLayoutTest`):

| Sensor suite | Line |
|---|---|
| none | "The arcology district before dawn, Lancer. Things on the ground that must die, and flyers over them." |
| L1 | "Mostly from ahead, two pincers from the flanks. Six targets to destroy, and a tower may fall. It won't touch you." |
| L2 | "New: the Hive Node, a hardened spawner, and the Ravager, a pack hunter that pounces. Creepers and turrets too." |
| L3 | "Anti-ground is a must: nothing else cracks a node. One cache: a creep cocoon on a boulevard roof. Bomb it open." |

**Radio script** ([Level 09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md#radio-chatter);
times are script time):

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Six hive nodes in the arcology district. All six die, Lancer." |
| t=8.5 | Rook (`requires: escort`) | "Six bugs, two pilots. I like those odds." |
| t=24 | Varga | "The nodes are hardened. Normal rounds just spark off them. Bombs, mortars or the Airstrike." |
| First hold starts (`hold-start`) | Okafor | "Slowing you down over the plaza. Make it count." |
| First Hive Node destroyed (`first-kill`) | Rook (`requires: escort`) | "One down. That's a horrible noise they make." |
| t=58 | Varga | "Fast movers on the ground, Lancer. Low, four legs. Hunting." |
| First pounce (`first-pounce`) | Rook (`requires: escort`) | "It jumped at you! Since when do they jump?" |
| t=95 | Kilo Lead (generic CDF portrait) | "Aegis, Kilo convoy on the Okonjo Bridge. Sixty civilians in the trucks, and things are coming over the far bank." (text until cast, row 9) |
| t=107 | Okafor | "Lancer, keep them off that bridge." |
| t=140 | Varga | "The arcology's frame is hollow with creep. Kill those last nodes and it may come down." |
| t=152.5 | The Choir (distorted) | "[the Choir sings]" (its round-29 stage sound) |
| Collapse starts (`collapse`) | Rook (`requires: escort`) | "Timber. Somebody had to say it." |
| Cocoon cache (secret) | Varga | "That cocoon was sitting on a CDF supply crate. Take it." |
| Level end | Okafor | "All six nodes dead. The outer districts evacuate at first light, and you're flying cover." |
| Secondary met | Kilo Lead (generic CDF portrait) | "Kilo convoy is across. Thank you, Aegis." (text until cast) |
| A node leaves the screen alive (the mission failed screen) | Okafor (grim) | "{group} got past you, Lancer. If it lives, the district is lost." (`{group}` = Node A1 … Node C2) |

**Names on screen**: the secondary's name **"Hold the bridge"** (the briefing's objectives line
`BONUS: HOLD THE BRIDGE`); the objective field from sensor L1 `DESTROY 6 HIVE NODES`; the tracker
`NODES A1 A2 B1 B2 C1 C2`, then `RAVAGERS n / 8`; the launch warning
`NO ANTI-GROUND SOURCE FITTED`.

### Part C numbers

From [Level 09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md) and its data:

- **Length:** 150 px/s, 27,900 px = **186 s of script time**, launch 5 s; about 3 min 20 s of real
  time with typical holds. Every time in the data and README is script time (D2 = a).
- **Holds** (D3 = a): when a cluster's first node is 200 px below the top edge the scroll eases over
  1 s to **30 px/s** (easy 20, hard 40) until both nodes die, then back over 1 s; the level clock
  runs at 0.2 in a hold at medium. A node not killed leaves the bottom edge ≈ **10.6 s** after the
  hold starts (easy 15.6 s, hard 8.3 s); no timeout. The capture's hold A took 11 s of real time.
- **Density:** **253 enemies at medium** (224 in the waves, 17 ground units, the 12 node Skitters
  counted as one opening per node), **83.9 a minute** over the 181 s after the launch (Act 2's
  minimum is 50). Waves: Skitter 146, Needler 38, Ravager 22, Creeper 9, Stinger 9; ground: Hive
  Node 6, Spine Turret 7 (hard +2), Polyp Mortar 4. The design had about 187 (≈ 62 a minute): in real
  seconds the stretches after each hold and the collapse emptied the screen for 4–8 s (11 pauses on
  easy), so Skitter streams and snakes fill them (146 Skitters against the design's ≈ 80).
- **Pacing** (`PacingTest`, real seconds, the plan's fit with Rook): easy 2 pauses over 3 s
  (191.1–194.9 s just after the collapse, the quiet end 206.6–213.3 s), medium 1 (the quiet end,
  195.6–202.1 s), hard 1 (the quiet end, 193.0–199.0 s).
- **Credit budget:** budget(9) = 700 × 1.07⁸ ≈ **1,203**. **`bounty_scale` 0.4**: the typical
  haul is **1,179** (−2.0 %), a perfect run **1,789** (1.49 × budget); the design's estimate was
  0.47 for ≈ 187 enemies. The bounties paid at Level 09 (× 1.6 × 0.4, rounded per kill): Skitter 3,
  Needler 8, Stinger 10, Creeper 14, Ravager 12, Hive Node 29, Spine Turret 8, Polyp Mortar 10. The
  data keeps Act 1 terms: the cocoon crate 100 (pays 160), the secondary 63 (pays 101). Rook's
  and the collapse's kills pay like the player's; the primary pays no bonus (confirmation f).
- **Anti-ground against the windows** (D8 = c): the plan's L09 visit sells the left Autocannon for a
  **Bomb Rack** and buys **Rook's Mortar** (1,575 cr with repairs, 58 cr left; forward DPS 64 as
  the sheet counts the Bomb Rack, 1.02 of the reference 63); the **Tail Gun moves to L10**; the
  later visits are re-planned so none overspends (the Airstrike charge to L12, the Tail Gun's L2 at
  L14). Anti-ground DPS 12 + 15 = 27 against a cluster's 128 HP (hard 166): **4.7 s** of the medium
  window (10.3 s counted to the first node's hit box leaving; 0.46), hard **6.1 s of 8.3 s** (0.73,
  above the 0.6 rule: accepted by the user, `BalanceTest`'s hardened check runs at medium only).
- **Autopilot** (`Level09Test`, the plan's fit with Rook): it flies the level to its end with
  grades **A / A / B** (easy / medium / hard); hard misses the bridge secondary. Since Rook aims his
  Mortar (confirmation b) medium wins without an Airstrike charge.
- **Capture run** (row 10): 180 / 253 kills, the secret, the secondary, A, 1,781 credits; **Rook's
  Mortar killed four of the six nodes** (A1, A2, B1, B2), the bombs C1 and C2; flown without input,
  Rook alone killed the first four and the C nodes left alive.
- **Radio retimes** (the 1 s rule, `RadioTimelineTest` flown with the holds, with and without Rook):
  t=2/4 → 1/8.5; the CDF officer and Okafor t=104/106 → 95 (two pages)/107; Varga and the Choir
  t=150/158 → 140 (two pages)/152.5. No voiced timed line starts more than 1 s late.

**The Hive Node** ([stat block](../../enemies/ground/hive-node/README.md#stat-block), first level 09):

| Field | Value |
|---|---|
| HP | 64 (easy 48, hard 83), **hardened**: only anti-ground deliveries, the Airstrike and the Smart Bomb damage it; time to kill 1.0 s at the reference DPS 63 |
| Size | 76×76, hit box 56×56, `medium`, `ground`, radial |
| Spawn | every 4 s from crossing the top edge: the iris opens over 0.5 s (the telegraph), releases 2 Skitters (hard 3) at 160 px/s in a 120° arc toward the ship; skipped while the ship is within 96 px |
| Counting | released Skitters count into the level's enemies as released; density and haul count one opening per node (12 at medium) |
| Bounty | 45 (pays 29 at Level 09), score 450 × chain |
| Death | the `large` organic burst, the creep withers over 2 s, the stump stays |

**The Ravager** ([stat block](../../enemies/ground/ravager/README.md#stat-block), first level 09):

| Field | Value |
|---|---|
| HP | 16 (easy 12, hard 21): time to kill **0.25 s** at the reference DPS 63, a `BalanceTest` exception ("fast, fragile pack hunter", user decision 2026-10-07) |
| Size | 56×56, hit box 40×28, `medium`, `ground` (`air` for the middle 0.3 s of a pounce) |
| Run | 160 px/s on its path plus the scroll, turning 180°/s, 46 px per gallop cycle; `pack` of 3–5, each on its own path, 0.25 s apart |
| Pounce | within 200 px of the ship: a 0.75 s leap aimed at take-off + 2 × (ship − take-off), the air window 0.225–0.525 s over the ship's spot, contact `medium` = 15 once per pounce (ship and Rook), the landing kept on the field; 3 s between pounces (hard 2) |
| Bounty | 18 (pays 12 at Level 09), score 180 × chain |
| Death | the `small` organic burst, no husk |

The **Creeper** returns with the act HP factor: 32 HP at Level 09.

### User decisions of part C (check they were carried out)

- [x] **D1 = a**: a node that leaves the screen alive fails the mission at once (the failed primary
  flow, the mission failed screen 3 s later) with Okafor's `{group}` line, voiced once per node; the
  rear stream, the end-of-scroll fail and the timed collapse are struck.
- [x] **D2 = a**: the level clock slows with the scroll in a hold (script time); waves, ground
  targets, radio lines, backdrop placements and the progress bar wait; enemies, bullets, the
  nodes' cycle, Rook and the backdrop's animation (`BackdropClock`) run on real time; the hold is in
  the state hash; `PacingTest` measures real seconds.
- [x] **D3 = a**: no hold timeout; hold speeds 20 / 30 / 40 px/s (hold C kept until the collapse's
  fall ends, confirmation a).
- [x] **D4 = a**: the Ravager is `air` for the middle 0.3 s of its pounce, the per-unit layer read at
  every hit, contact and targeting site; the user's later choice of the **overshoot** leap is built
  (confirmation c).
- [x] **D5 = a**: the collapse is a simulated sweep that kills the ground units under it and pays
  like Airstrike kills; its band is the `arcology` tower's footprint on the ground
  (`collapse.tower`), so it follows the tower whenever the nodes die.
- [x] **D6 = a**: the secondary counts the Ravagers of the two waves tagged `bridge` (6 / 8 / 10),
  the trucks are scenery; the tracker's second line `RAVAGERS n / 8`.
- [x] **D7 = a**: `threat_profile.required: [anti-ground]`; the launch warning at every sensor level,
  counting an anti-ground weapon, Rook's Mortar while he flies, an Airstrike or Smart Bomb charge
  (`LaunchRequiredTraitTest`).
- [x] **D8 = c**: the plan's L09 Bomb Rack swap and Rook's Mortar, the Tail Gun at L10, and
  `BalanceTest`'s hardened check (at medium only, user decision).
- [x] **D9 = a**: the Hive Node, the Ravager, the backdrop with the trucks and the cocoon, the
  briefing images and the intel portraits straight to production in the last hour of the night
  (rows 1–3, 5); a/b only for the collapse look, the two sounds and the Kilo Lead (rows 4, 7–9),
  whose production follows this round.
- [x] **D10 = a**: units used by several levels go into each level's unit atlas (`SpriteUse`,
  `AtlasPacker`; confirmation e).
- [x] **D11 = a**: the Kilo Lead auditioned a/b (row 9), `uncast` and his lines as text until then.
- [x] **Defaults:**
  - weak points drawn only (the node's ×2 iris and the Ravager's ×1.5 maw struck);
  - `armour: hardened` flown for enemies (the glance and spark; the Airstrike and the Smart Bomb
    hit; homing shots and Rook skip a hardened target unless their weapon is anti-ground);
  - the node's spawn cycle as in its stat block, one opening per node in the density and haul,
    nodes at the ground share (80 %);
  - six groups `Node A1` … `Node C2`, each hold naming its two, the tracker's two-character marks;
  - `pack` in the formation vocabulary; the Ravager's gallop and pounce as in its stat block;
  - the data in Act 1 terms (crate 100, secondary 63) with a `bounty_scale`; the Creeper with the act
    HP factor (32 HP);
  - the density from the level's own roster, Polyp Mortars included; wave times in script time;
  - the radio retimed to the 1 s rule, Rook's "at me" now "at you", the events `hold-start`,
    `first-pounce` and `collapse`, the cocoon's line, Rook's lines `requires: escort`;
  - two one-screen briefing pages with images, Rook's teaser (not voiced), Varga's four lines;
  - the Airstrike charge only with a special fitted and no shield-cell fallback; **changed in the
    build**: it drops from the last Stinger of the t=31 column (easy's second from the t=116
    column), not from the plaza turret pair's last unit, since a group drops its pickup only when it
    is an objective group;
  - the cocoon on a low roof, a hardened destructible of 30 HP, its crate and an armour patch;
  - the Kilo trucks as scenery, no skybridges, nothing on `far`, the Ndidi Arcology as Level 08's
    ground tower;
  - the collapse's 1.5 s shadow, 3 s fall, event-triggered `heavy` dust over 6 s, the rubble;
  - the base stem from the launch, the full mix in each hold and from the collapse (`full_on`); the
    docs' "intensity rising with the density" claim corrected (never built);
  - moving scenery on the real clock; no HUD indicator for a hold;
  - the Act 2 README fixed (the stray backticks, the Creeper in the act-HP list, the teaser not
    voiced).

### Choices made during the build

Each to **confirm or change** (row 15):

- **(a) Hold C lasts until the collapse's fall ends** (main-agent default,
  [Level 09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md#decisions)): otherwise
  the scroll eases back to 150 px/s as C2 dies and the arcology scrolls about 675 px away during
  the warning and the fall.
- **(b) Rook with a lobbed gun picks ground targets anywhere ahead within 200 px**, not only in his
  30° cone ([wingmen](../../player/wingmen/README.md#decisions)): needed for the aiming the user
  chose, since a target beside the player never enters his cone within the lob's range; his shells
  land on the target, their flight time (0.6 s at 200 px) and arc scaled to the distance.
- **(c) The pounce's overshoot lands on the play field**
  ([Ravager](../../enemies/ground/ravager/README.md#behaviour)): a landing point off the field is
  pulled back so the centre stays at least half its hit box inside every edge; the second half of
  the leap is then shorter.
- **(d) The speaker name "Kilo Lead"** with the generic CDF radio portrait (`radio-generic-cdf`)
  for the convoy's officer ([voice](../../audio/voice/README.md)).
- **(e) The atlas rule D10 as built**
  ([production](../../art-direction/production/README.md#pipeline)): after D10 the shared atlas is
  1 page of 2048×1024 at **72 %** with no enemies left on it; Level 01 2048×128 (56 %, Skitter,
  Needler, cargo); Level 08 2048×1024 (71 %); Level 09 2048² (69 %, Hive Node, Ravager); Level 07
  two unit pages (2048² at 96 % and 2048×128); every level at 3 of 6 pages or fewer. A unit used by
  several levels is copied into each level's atlas in the build output (never committed).
- **(f) A `destroy-targets` primary pays no bonus**
  ([economy](../../systems/economy/README.md#per-level-budget)): the nodes pay their bounties as
  kills (29 each); the debrief shows the primary at 0, by design.

## Listening

The 20 new voiced lines (row 11) as the game plays them, from `assets/voice/`. The files were matched
to their lines by the voice key, the file name (`VoiceLines.key`, as in the render log of
`tools/art/voice.py`). The Kilo Lead's audition (row 9) is on the board's cards; his two lines play
as text until he is cast.

| Line | Speaker | Text | Take |
|---|---|---|---|
| Briefing page 1 | Okafor | "Overnight the Vrell grew six hive nodes in the arcology district, Lancer…" (listen for "arcology" and "Vrell", 19.9 s) | [play](../../../assets/voice/okafor/9710c8c6d7ca.ogg) |
| Briefing page 2 | Varga | "The nodes are hardened: only bombs, mortars or the Airstrike crack them…" (pinned take; listen for "in packs", 12.6 s) | [play](../../../assets/voice/varga/febdaf698bba.ogg) |
| t=1 | Okafor | "Six hive nodes in the arcology district. All six die, Lancer." | [play](../../../assets/voice/okafor/7e0da1d6b538.ogg) |
| t=8.5 | Rook | "Six bugs, two pilots. I like those odds." | [play](../../../assets/voice/rook/e8c1e7704064.ogg) |
| t=24 | Varga | "The nodes are hardened. Normal rounds just spark off them. Bombs, mortars or the Airstrike." | [play](../../../assets/voice/varga/a305a0d5aaec.ogg) |
| First hold | Okafor | "Slowing you down over the plaza. Make it count." | [play](../../../assets/voice/okafor/c0a036e87397.ogg) |
| First node destroyed | Rook | "One down. That's a horrible noise they make." | [play](../../../assets/voice/rook/f4a206a86680.ogg) |
| t=58 | Varga | "Fast movers on the ground, Lancer. Low, four legs. Hunting." | [play](../../../assets/voice/varga/2686b5d1ec79.ogg) |
| First pounce | Rook | "It jumped at you! Since when do they jump?" | [play](../../../assets/voice/rook/567b56e46401.ogg) |
| t=107 | Okafor | "Lancer, keep them off that bridge." | [play](../../../assets/voice/okafor/a8ae1e92835b.ogg) |
| t=140 | Varga | "The arcology's frame is hollow with creep. Kill those last nodes and it may come down." | [play](../../../assets/voice/varga/3138bac277d9.ogg) |
| t=152.5 | The Choir | "[the Choir sings]" (round 29's stage sound, reused) | [play](../../../assets/voice/choir/voice-choir-sings-r29-b.ogg) |
| Collapse | Rook | "Timber. Somebody had to say it." | [play](../../../assets/voice/rook/ac9223e40032.ogg) |
| Cocoon cache | Varga | "That cocoon was sitting on a CDF supply crate. Take it." | [play](../../../assets/voice/varga/a0d876a30989.ogg) |
| Level end | Okafor | "All six nodes dead. The outer districts evacuate at first light, and you're flying cover." | [play](../../../assets/voice/okafor/7352cb0e9216.ogg) |
| Failed, Node A1 | Okafor (grim) | "Node A1 got past you, Lancer. If it lives, the district is lost." (pinned take; never read back clean, "U Lancer") | [play](../../../assets/voice/okafor/e17cf2dea039.ogg) |
| Failed, Node A2 | Okafor (grim) | "Node A2 got past you, Lancer…" (pinned take) | [play](../../../assets/voice/okafor/f6b5fc8fbc52.ogg) |
| Failed, Node B1 | Okafor (grim) | "Node B1 got past you, Lancer…" | [play](../../../assets/voice/okafor/815c8e962600.ogg) |
| Failed, Node B2 | Okafor (grim) | "Node B2 got past you, Lancer…" | [play](../../../assets/voice/okafor/17b8ba5f79c2.ogg) |
| Failed, Node C1 | Okafor (grim) | "Node C1 got past you, Lancer…" (pinned take; "See, one" through the filter) | [play](../../../assets/voice/okafor/3e65a05739f4.ogg) |
| Failed, Node C2 | Okafor (grim) | "Node C2 got past you, Lancer…" | [play](../../../assets/voice/okafor/7144fed65ace.ogg) |

## Notes

- Review file names: `<subject>-final-r31-a.png/.gif/.ogg` (production art and audio),
  `level-09-capture-final-r31-a` / `level-09-readability-r31-a` (game captures), and
  `<subject>-r31-a/b` (the four a/b picks), in each part's `concept/`. Their `prompts.md` entries
  say how they are made. Nothing in `assets/` is hand-edited.
- The board shows the video `level-09-capture-final-r31-a.mp4` as a link ("open"). It does not show
  the Kilo Lead's reference clips (they live in `design/audio/voice/refs/`, not in a `concept/`
  directory); they are linked in row 9.
- The capture was taken with `--invulnerable` on a private display; its prompts entry lists the
  exact options. It was made before the dust fade-out fix (panel 11); the fix and two checks since
  (the music fit's base stem in hold C was fit drift, guarded by a test in `Level09Test`; the
  primary's 0 credits in the debrief is by design, confirmation f) are not captured.
- After the choices (at the close):
  - the approved parts get `art: final`;
  - the picked collapse look replaces the neutral fall in the renderer (`Fall` a or b) with its
    final rubble, and the rejected concept moves to `concept/rejected/`;
  - the picked sounds go to `assets/sfx/` (a `PRODUCTION` entry in `sfx_r31.py`), replace the
    placeholders, and the collapse sound moves to the shadow's start (`COLLAPSE_AT_WARNING`); a CC-BY
    pick gets its credits-screen entry; the rejected files move to `concept/rejected/`;
  - the cast Kilo Lead renders his two lines (`uncast` dropped, `AUDITIONING` emptied), the
    rejected clip is deleted with its CREDITS.md row;
  - Level 09's design leaves `review` once the texts are approved.

## Decisions

- 2026-10-07: Opened with M5 part C.
- 2026-10-07: User verdict on every row but row 4: the Hive Node, the Ravager, Level 09's backdrop
  with the Kilo trucks and the cocoon, the two briefing images and "Firestorm" with its base stem
  approved as final; the collapse sound **a** (CC-BY 4.0, YleArkisto, on the credits roll; b, a
  steel-frame cave-in, rejected); the Ravager's pounce: **both** a and b kept, the game picking one
  at random per pounce; the Kilo Lead **a** (Aaron Bennett; b, tombooker, rejected, his clip deleted
  with its CREDITS.md row); the node's reuse of the iris and spawn sounds confirmed; the capture, the
  20 voiced lines, the texts (Level 09 now `approved`), the part C numbers, the D1–D11 checklist and
  the build choices (a)–(f) accepted. The sounds and the Kilo Lead's lines were produced at the
  close. Row 4, the collapse's look: **both a and b rejected** (moved to
  `design/campaign/act-2-homefront/level-09-arcology-fall/concept/rejected/`), a rework asked for
  (a little sway, then straight down; the shadow moves with the building; dust in every direction
  from just before the impact and a large blast at it; no sharp dust edge), and the collapse's kills
  are to spread outward from the tower's foot at the impact over about 1 s (planned, after c). The
  rejected sounds are in `design/audio/sfx/concept/rejected/` and
  `design/audio/voice/concept/rejected/`. **The round stays open for row 4** (variant c in
  progress).
- 2026-10-07: Closed (user): row 4, the collapse's look, **c** approved with two tweaks (the shadow
  halved, about 50 %; a single lean to the right instead of a back-and-forth sway), built in the
  game (the lean and the drop by the tower projection, the cast shadow, the puff sprites, the
  `arcology-heap`, the kills outward from the foot over 1 s, the sound's crash at the impact) and
  captured (`collapse-capture-final-r31-a`); hold C lasts until the collapse's dust has settled
  (9.0 s after the warning starts) so the heap stays in sight after a late kill.
