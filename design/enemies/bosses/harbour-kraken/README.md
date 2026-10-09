---
title: Harbour Kraken
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy, ../../../allies]
updated: 2026-10-09
---

# Harbour Kraken

## Summary

The Act 2 mid-boss: a rust cephalopod wrapped around an offshore UTC platform. Its arms slam telegraphed lanes while the head lurks below; when it surfaces crown-first, its lime eyes open as weak points and it fires fans.

## Design

### Stat block

Values are first-draft balancing numbers at **medium**, in [data.yaml](data.yaml) with the boss script (`boss`: the lanes, the slam cycle, the phases, the par; the slam-arm chains; the part layers and the eyes' weak spots; M5 part E, step E2c, keys in the [schemas](../../../tech/architecture/README.md#data-file-schemas)) (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (the sea's surface); each part's **current layer**: `sub` while submerged, `ground` while surfaced; the slam arms are `ground` from their rise until they sink |
| Size tier | `huge` |
| Size | 344×424 px, hitbox 208×152 (the grip overlay and the head: the mantle with the arm roots ≈ 250 px long, 130 px wide); the hitbox is Platform Tiamat's deck (a backdrop piece; no armour: shots fly over it) |
| Parts | head/mantle (`vital`; the lime eyes a weak spot on it, ×2 while open), left and right slam arm (`destroyable`; runtime chains of 13 segments, E7 = a), the gripping arms (a drawn overlay on the platform) and the idle arms (`sub` shadows) |
| Orientation | `32 angles` (slam-arm segments; the head pre-rendered surfacing frames) |
| HP | head 1,700 (×0.5 on the mantle, ×2 on the open eyes; tuned to a ≈ 60 s fight, E6 = a); slam arm 350 each; total 2400 (easy 1800 / hard 3120, from the global multipliers) |
| Armour / shield | submerged parts: only `anti-sub` and the Smart Bomb ([layer rules](../../README.md#layer-rules)); surfaced head: mantle ×0.5, eyes ×2 while open |
| Speed | 0 px/s (anchored) |
| Movement | anchored: it scrolls in with the platform (invulnerable, without its bar) and engages when the scroll halts, the platform's centre at y 78; the head surfaces and dives, the arms slam (see *Phases* and *Lanes and slams*) |
| Attack | see *Phases*; the slams: telegraph 1 s, rise 0.5 s, impact `heavy` = 10 on the ship and Rook in the lane plus 6 `small` splash bullets (120 px/s), awash 1.5 s, sink 0.6 s; a 7-orb `fan` from the beak (spread 70°, 140 px/s, `medium` = 6) every 2 s while the eyes are open, each after a 0.5 s crimson beak glow |
| Formations | solo set piece (a Driftjelly field released in the arena) |
| Weak points | lime eyes (×2) (only while open) |
| Effective traits | `anti-sub`, `anti-ground`, `forward` |
| Credits | 216 (score 2160 × chain) (arms 36 each, head 144; paid × 1.6 × Level 11's `bounty_scale` of 0.6: 208, 15 % of budget(11) = 1,377) |
| Death | `huge` water-surface variant: the head sinks in a churning foam ring, the gripping arms slide off the platform, spray, credit shower |
| First level / used in | L11; mid-boss of L11 only |
| Difficulty hooks | hard: lane telegraph 0.8 s; phase 3 slams three lanes |
<!-- /data -->

### Phases

| Phase | Ends at | Behaviour |
|---|---|---|
| 1 — Slams | 3 slams or 20 s, or both slam arms severed | Head submerged. The slam cycle (see *Lanes and slams*), one slam at a time: a lane churns and flashes for 1.0 s (red dashed telegraph), a slam arm rises base-to-tip and slams it, lies awash 1.5 s (hittable) and sinks; the next telegraph starts when it has sunk. |
| 2 — Head up | the head's HP below 40 % | Cycle: the head surfaces crown-first (2 s, swell and foam), the eyes open for 8 s; the beak glows crimson 0.5 s before each 7-orb `fan` (spread 70°, 140 px/s, `medium` = 6) every 2 s; then it dives and one slam follows. Its first surfacing releases the arena's Driftjelly field. |
| 3 — Two lanes | the head's HP at 0 | The head stays up, eyes open, fanning every 2 s; every 4 s two lanes are telegraphed and slammed at once, one by each living arm (hard three). A shield cell drops as it begins. |

### Arena

[Atlantic convoy (L11)](../../../campaign/act-2-homefront/level-11-atlantic-convoy/README.md): the
scroll halts with Platform Tiamat at the top of the play field; four lanes of 120 px run the full
height below it (lane 1 at the left edge); the [convoy's cargo ships](../../../allies/README.md#convoy-cargo-ship)
hold lanes 1, 2 and 4 in the lower band (the level's secondary objective). Target duration 45–75 s
at medium, tuned to **≈ 60 s** (E6 = a, see *Duration*).

### Behaviour

- **Anchored arrival** (M5 part E): the Kraken has no descent. It is placed on the ground layer
  with the platform and scrolls in with it, entering at the top edge before the halt, invulnerable,
  without its bar and silent. It **arrives at the arena's start** (its `t`), as the scroll begins to
  ease to a halt over 1 s: the bar appears (the short mid-boss bar), the mini-boss sting plays and the
  boss checkpoint is recorded; it **engages at the halt**, 1 s later (invulnerable until then, as a
  boss in its descent). Its centre (Platform Tiamat's) is then 78 px below the top edge (110 until
  round 33): it lies on
  the ground at the halt scroll, which is the scroll at the arena's start plus the ease (68.8 px at
  140 px/s), and the level's platform piece must be placed at the same ground position. The level's
  foreshadowing (the passing shape at t≈55, the mantle shadow under the platform in section 4) is
  scenery, not the boss.
- **Lanes and slams** (user decision E5 = a): each slam arm **owns a half**: the left arm lanes 1–2,
  the right arm lanes 3–4. A slam is made by the arm whose half holds the chosen lane.
  - **The cycle**: the telegraph, a red dashed churn along the lane, 1.0 s (hard 0.8 s); the arm
    rises base-to-tip out of the water at the platform (≈ 0.5 s, a first value) and slams down the
    lane: at the **impact** the ship and Rook take `heavy` = 10 if their hit box overlaps the lane
    (once per slam), a cargo ship in the lane takes one hit, and a line of 6 splash bullets
    (`small`, 120 px/s) fans out from along the arm (from six evenly spaced points, out to the left
    and the right in turn, 15° below the horizontal); then the arm lies **awash** for 1.5 s (on
    `ground`: every weapon that reaches the ground hits it, `anti-ground` ×2; no contact) and
    sinks (≈ 0.6 s, a first value) back to `sub`. The arm is `ground` from its rise until its sink
    starts; only the impact hurts. The lanes run from the deck's lower edge (y 154) to the bottom
    edge; from its rise the arm lies along its lane's centre line, its 13 segments evenly from the
    lane's top to its tip 24 px above the bottom edge (each segment one of the arm's hit boxes); at
    rest it lies under the water by its root.
  - **Lane choice**: the slams alternate between the lane the player is in and the lane of the
    surviving cargo ship nearest the player's x (default; a tie goes to the lower-numbered lane);
    with no ship left every slam targets the player's lane. A slam in a lane holding both the
    player and a ship hits both.
  - **Severed arms**: an arm destroyed in its awash window (or by torpedoes while submerged) makes
    its half **safe** for the rest of the fight. A choice that falls in a severed half goes to the
    other choice if that lies in the living half, else the living arm slams its half's lane nearest
    the player. With both arms severed nothing slams: phase 1 ends, phase 2 skips its slam, phase 3
    only fans. Cutting the left arm saves Halvorsen's and Mbeki's lanes, the right arm
    Saint-Laurent's.
  - **Phase 3**: each living arm slams once per volley: the player's lane and the nearest ship's
    lane, each by its half's arm; when both fall in one half (or are the same lane), the other arm
    slams its own half's lane nearest the player. **Hard**'s third lane is the next-nearest ship's
    lane (without one, the lane beside the player's): an arm whose half holds two targets slams its
    second lane 0.5 s after its first. With one arm left it slams only its half (hard: twice, 0.5 s
    apart). The first volley comes 1 s after the phase begins, then one every 4 s; an arm whose
    cycle still runs (an earlier slam not yet sunk) sits a volley out.
- **Ship damage**: a slam in a lane with a cargo ship is one hit; a ship survives three slams
  (smoke and a list from the first) and sinks on its fourth (easy: survives four; hard: sinks on
  its second; retuned 2026-10-09). Cutting the arm whose half holds the ships first is what keeps
  them: every slam in the left half strikes a ship's lane. Ships take no other damage from
  the Kraken (splash bullets and fans travel on the player's plane). See
  [allies](../../../allies/README.md#convoy-cargo-ship).
- **The head**: submerged in phase 1 (`sub`: only torpedoes and the Smart Bomb reach it, so a
  player with the optional Torpedo Pod can damage it, a reward for buying `anti-sub` early). It
  surfaces crown-first over 2 s (its layer flips to `ground` at the midpoint), its eyes open (the
  weak spot ×2 while open; the mantle ×0.5 elsewhere on the head, also under the water), and dives
  the reverse way over 1.5 s. A shot under an open eye flies on to it (the hit counts the eye on
  its line ahead). The fans leave from the beak (the head's lower edge); the first one 0.5 s after
  the eyes open, the glow running from the opening, then one every 2 s. When phase 3 begins mid-dive
  the head turns round and rises again.
- **The arena's jelly field** (default): as the head first starts to surface the Kraken releases 6
  [Driftjellies](../../naval/driftjelly/README.md) as a `field` over lanes 1 and 4 (three each, in
  rows 70 px apart from 50 px below the lanes' top, jittered by a generator of their own), half of
  them submerged (the jelly's `start`), with their own swap timers, drifting down the halted sea at
  their 15 px/s; they count into the level's enemies as they are released (as a boss's streams), not
  into its density. A release, because the level clock is halted at the arena.
- **Clock**: every timer of the fight (the telegraphs, the slam cycle, the surfacing, the fans, the
  jellies' swaps) runs on the simulation's real steps and is in the state hash; the level clock
  stays halted until the Kraken dies.
- Every arm visibly continues under water to the mantle (water rules); the Kraken never pops in.
- **Rook** keeps out of a telegraphed lane until its impact (his slot moves beside the run of
  telegraphed lanes it falls in, on the side nearer him) and a slam hits him like the ship
  ([wingmen](../../../player/wingmen/README.md#act-2-hazards)); the test autopilot avoids
  telegraphed lanes too (a move still in a lane when it strikes counts as a hit), so the balance and
  playthrough tests can finish the fight.

### Duration

The draft's ≈ 64 s was the total HP 2 700 ÷ the effective boss DPS (0.6 × 70 = 42), as if every part
were always exposed. In fact phase 1 (≤ 20 s) exposes only the arms, 1.5 s per slam (the head only
to torpedoes), phase 2 exposes the head about 8–10 s of every ≈ 16 s cycle, and only phase 3 is
continuous: about 90–100 s at medium without a torpedo. User decision **E6 = a**: the shape stays;
the HP (mostly the head's) is tuned in step E2c until the autopilot's fight lasts **≈ 60 s at
medium** with the balance plan's loadout; `BalanceTest` gets a **window-aware duration** (the HP of
each phase ÷ the DPS over its exposure windows) instead of total HP ÷ 0.6 × reference, and the
`par` comes from the measured fight.

**As tuned (step E2c):** the head **1,700** (the arms 350 each, total 2,400; easy 1,800, hard
3,120). The autopilot (`KrakenTest`, the balance plan's Level 11 fit: the front at L4, the Bomb
Rack, the Autocannon Pod, the Tail Gun, Rook's Mortar, no Torpedo Pod; Level 11's convoy) kills it
in **60.9 s** on average at medium over five seeds (51–67 s, no armour lost to it), so the **par
stays 60 s**; easy 42 s (34–48 s); hard about 99 s (87–110 s) on the four of five seeds it wins (the
autopilot is worn down on one). The aimed eyes are why the head ends higher than the 1,200–1,400
first guessed: their ×2 over the open window outweighs the mantle's ×0.5. The window-aware estimate
in `BalanceTest` and `tools/balance.py` (the effective 0.6 × 70 = 42 DPS; while the eyes are open
the mean of the eyes' ×2 and the mantle's ×0.5, as hits fall on both; ×0.5 over half the rise and
the dive; nothing under the water) is **57.4 s**: the ease 1 s, phase 1 9.7 s (its delay and three
slam cycles to the third impact), phase 2 33.7 s (2.2 surfacing cycles of 15.1 s with the slam after
each dive), phase 3 13.0 s.

### How it is built

User decision **E7 = a** (the atlas budget: one unit on at most one 2048² page):

- The **two slam arms** are runtime chains with authored motion (rise base-to-tip, slam along the
  lane, lie awash, sink), drawn from a few tapered segment sprites at 32 angles; their submerged
  stretches through the generic `sub` pass ([art direction](../../../art-direction/README.md#animation-rules), E4 = c).
- The **four gripping arms** are baked into a pre-rendered grip overlay registered on the platform
  (the platform itself is the level's backdrop piece); they slide off at the death.
- The **two idle arms** are `sub` shadows.
- The **head and mantle** are pre-rendered surfacing and diving frames, the eyes opening and the
  beak's glow.

**Drawing notes** (step E1, [`tools/art/water_fx.py`](../../../../tools/art/README.md) and `harbour_kraken.py`):

- The head is drawn at the chosen concept's scale (88.2 px per model unit): the mantle with the
  arm roots is about **250 px** long and 130 px wide, not the ≈ 190 px of the first draft.
- The lane telegraph's churn is tiled down the whole lane. Variant a draws its red dashes down
  **both lane edges**; variant b its chevrons down the **centre** (and boils the whole lane).
- The slam's splash is laid as one segment every **48 px**, from the arm's base to its tip, each
  one frame later than the one before (a: spray sheets to both sides; b: a crown with two rollers).
- **Registration:** all positions are relative to Platform Tiamat's centre. The deck is round
  07's 208×152 px (edges x ±104, y ±76) and the head's centre lies **216 px** below the platform
  centre (152 until round 33): in open water off the deck's south edge, every head frame's top below
  it, the gripping arms reaching up from the head's roots over the deck's edges; the level's backdrop
  piece must match it (or the grip overlay is re-registered).

### Concept art

Chosen concept: [harbour-kraken-r07-a.png](../concept/harbour-kraken-r07-a.png), [harbour-kraken-r07-a.gif](../concept/harbour-kraken-r07-a.gif) (listed in the [bosses](../README.md#concept-art) Concept art table). M5 part E takes it straight to production with the platform's grip (user decision E9 = a, concept round 33; `tools/art/harbour_kraken.py`): the head and mantle frames, the eyes and beak, the arm segments at 32 angles in a few taper sizes, the grip overlay, the death frames and the intel silhouette, on at most one atlas page. The **lane telegraph and slam look** is an a/b at production quality in round 33 (E9 = a).

The production files (M5 part E, straight to production per E9 = a, built per E7 = a; approved as final in concept round 33), generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/harbour-kraken-final-r33-a.png](concept/harbour-kraken-final-r33-a.png) | Final sprites (`tools/art/harbour_kraken.py`): the plain head for the `sub` pass, the surfaced head with its eye and beak frames and collar, the 2 s surfacing (`harbour-kraken-surface_0..11`), the slam-arm segments at 7 tapers and the tips at 32 headings, foam clumps, the grip overlay and its under-water stretch, the idle and foreshadow shadows, the death (sinking head, sliding grip, burst) and the severed arm's pop; in play on sea and platform stand-ins | chosen |
| [concept/harbour-kraken-final-r33-a.gif](concept/harbour-kraken-final-r33-a.gif) | The concept's choreography with the final sprites: a slam, the head surfacing crown first, eyes open, beak glow, the dive, then the death | chosen |
| [slam-r33-a.png](../../../art-direction/concept/slam-r33-a.png), [slam-r33-a.gif](../../../art-direction/concept/slam-r33-a.gif) | Lane telegraph and slam, variant a "edge dashes + spray sheets" (`tools/art/water_fx.py`; kept in art-direction's concept directory) | chosen |
| [slam-r33-b.png](../../../art-direction/concept/rejected/slam-r33-b.png), [slam-r33-b.gif](../../../art-direction/concept/rejected/slam-r33-b.gif) | Variant b "lane boil, chevrons, rollers" (`tools/art/water_fx.py --variant b`) | rejected |

## Implementation

- [x] Stat block and boss script in a `data.yaml` loaded from data; global difficulty multipliers
      applied (M5 part E, step E2c: `BossSpec.Arena`, `SlamArena`; `KrakenTest`, `SlamArenaTest`)
- [x] Anchored arrival: scrolling in with the platform, invulnerable and barless until the halt,
      engaging there (bar, sting, checkpoint) without a descent (M5 part E, step E2c)
- [x] Lanes and slams (E5 = a): telegraph → rise → impact → awash → sink on the real steps; each
      arm owning its half; lane choice alternating the player's lane and the nearest ship's;
      severed arms making their half safe; phase 3's volleys and hard's third lane (M5 part E, step
      E2c)
- [x] The impact: `heavy` 10 to the ship and Rook in the lane, one hit to a ship in it, 6 splash
      bullets; ships sinking on the second hit (easy the third) (M5 part E, step E2c)
- [x] Phases: phase 1 ending on 3 slams, 20 s or both arms severed; phase 2 ending on the head's HP
      share; phase 3; the shield cell (M5 part E, step E2c)
- [x] Surfacing and diving head: the layer flip at the midpoint, the eyes' weak spot only while
      open, the mantle ×0.5, the beak glow 0.5 s before each fan (M5 part E, step E2c)
- [x] The arena's Driftjelly field released at the first surfacing (M5 part E, step E2c)
- [x] HP tuned to a ≈ 60 s fight at medium with the autopilot (E6 = a); `BalanceTest`'s
      window-aware duration and its share after `bounty_scale`; `par` from the measured fight;
      the autopilot avoiding telegraphed lanes (`KrakenTest`) (M5 part E, step E2c; the share,
      15 % at Level 11's `bounty_scale` of 0.6 with the bounty of 216, 2026-10-09)
- [x] An arm-first autopilot (`Autopilot.ArenaPlay.ARMS`) keeps all three cargo ships afloat most
      of the time at medium in a ≈ 60 s fight (`KrakenTest`) (M5 part E, 2026-10-09)
- [x] Water-surface interaction per the art-direction water rules; the arms as chains with
      authored motion, the grip overlay, the idle arms' shadows, the head's frames (E7 = a) (M5
      part E, steps E1b and E3c: `KrakenLooks`, `Level11LooksTest`)
- [x] The lane telegraph and slam look (round 33's a/b) (M5 part E, steps E1b and E3c: either
      variant drawn from the sizes the atlas holds)
- [x] Death effect, bounty and score per this spec (M5 part E, steps E2c and E3c)

## Decisions

- 2026-10-01: Promoted from the bosses roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L11 reference DPS (×70/100): head 2 900 → 2 000, slam arms 500 → 350, total 3 900 → 2 700; duration ≈ 64 s (was ≈ 65 s). Bounty unchanged.
- 2026-10-01: Lane choice (alternate the player's lane and the nearest ship's lane) and ship damage (a ship sinks on its second slam) moved here from the L11 level doc.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-08: M5 part E (user decisions of 2026-10-08): **E5 = a** each slam arm owns a half of the
  lanes (left 1–2, right 3–4); severing an arm makes its half safe; phase 3 slams the targets in
  both halves, hard's third lane a second slam 0.5 s later by an arm whose half holds two targets;
  rejected: b (either arm reaching every lane: "remove their lanes" reworded, cutting protects less
  visibly) and c (non-adjacent lanes per arm: an arm crossing another's lane). **E6 = a** the shape
  kept and the HP tuned to a ≈ 60 s fight with a window-aware duration in `BalanceTest`; rejected: b
  (keeping the HP, a ≈ 90 s mid-boss as an exception) and c (wider windows: fewer slams, an easier
  secondary). **E7 = a** the two slam arms as runtime chains with authored motion, the gripping arms
  baked, the idle arms as `sub` shadows, the head pre-rendered; rejected: b (eight runtime chains:
  more frames and draw calls for arms that only sway) and c (pre-rendered slams per lane: over a
  page). **E9 = a** straight to production, the lane telegraph and slam look a/b. Stated defaults:
  the bounty paid ≈ 15 % of budget(11) ≈ 207 after the act factor and the level's `bounty_scale`
  (the draft's "300 absolute" of 1 967 and "≈ 15 % of 1 967" restated), arms : head 1 : 4; lane
  choice "nearest ship" by the player's x, ties to the lower lane, a slam hitting player and ship in
  one lane both; the arena's jelly field released at the first surfacing; the sting at the arena
  halt; Rook keeping out of the lanes and hit by slams; the fight's timers on the real steps. Our
  readings, for review in round 33: the anchored arrival (no descent; it scrolls in with the
  platform); the rise 0.5 s and sink 0.6 s; the next phase-1 telegraph once the arm has sunk; phase
  1 also ending when both arms are severed; a choice in a severed half going to the other choice or
  the living half's lane nearest the player; the splash bullets `small`; the platform a backdrop
  piece with the grip as the Kraken's overlay. `design` goes to `review`; `implementation` is
  `in-progress` for part E.
- 2026-10-08: Presentation as built (step E1): the head at the chosen concept's scale (mantle with arm roots ≈ 250 px, the stat block's ≈ 190 px was too low; hitboxes stay a later step's numbers) and Platform Tiamat's deck registered as round 07's 208×152 px (edges ±104/±76, the head 152 px below the platform centre; the stat block's 220×150 corrected), so the backdrop piece must match. The slam look is an a/b in round 33 (a edge dashes + spray sheets, b lane boil, chevrons, rollers).
- 2026-10-08: Simulation as built (M5 part E, step E2c; our readings, for review in round 33): the
  stat block and boss script in `data.yaml` (movement `anchored: {y: 110}`; parts with their
  starting `layer` and the eyes as `spots`; the slam arms as chains with `motion: slam`; the boss's
  `lanes` and `slam`; phases with `until.slams`/`until.below`, `slamming`, `surface`, `release` and
  `drop`; hard's `telegraph` and `lanes` hooks). It **arrives at the arena's start** (bar, sting,
  checkpoint) as the scroll eases to a halt over 1 s and **engages at the halt** (the E0 text had the
  bar at the halt); the ease adds 68.8 px of scroll at 140 px/s, so the platform piece lies at the
  halt scroll, not at the scroll at the arena's start. The arm is `ground` from its rise until its
  sink starts, laid along its lane from the deck's edge to 24 px above the bottom edge (13 evenly
  spaced segments as its hit boxes); the splash leaves from six points along it, out to either side
  in turn, 15° down; the fans leave from the beak (the head's lower edge), the first 0.5 s after the
  eyes open; a shot under an open eye counts the eye on its line ahead; the mantle's ×0.5 also
  under the water; the dive 1.5 s; the field placed in rows down lanes 1 and 4, half submerged,
  released as the head first starts to rise; phase 3's first volley 1 s after it begins, a busy arm
  sitting a volley out, a dive turned round into a rise; a phase ending on slams also ends with both
  arms severed (phase 2 then only skips its slam). **E6 = a tuned:** the head 1,700 (total 2,400),
  the autopilot's fight 60.9 s at medium (par 60 kept), the window-aware estimate 57.4 s (see
  *Duration*); the bounty 186 in Act 1 terms (arms 31, head 124), ≈ 15 % of budget(11) at a
  `bounty_scale` of ≈ 0.7.
- 2026-10-08: Level 11's data (M5 part E, step E3a) sets its `bounty_scale` to 0.6 (the typical haul
  on budget(11) with the popcorn the pacing rule needs), so the bounty of 186 pays 179, **13 %** of
  budget(11), inside the mid-boss band of 10–20 % (`BalanceTest`); the bounty stays (raising it to
  ≈ 216 would bring back 15 %, for review in round 33). The fight in the level at medium (the plan's
  fit, KrakenTest's five seeds): 59.0 s mean (48.9–67.2).
- 2026-10-08: Presentation as built (M5 part E, step E3c; our readings, for review in round 33):
  `KrakenLooks` draws the idle arms (2×), the grip's under-water stretch, the slam arms' submerged
  segments and the plain head (fainter while it is down) in the `sub` pass, then on the surface the
  grip overlay, the head's surfacing step (its 12 frames in time with the simulation's rise and
  dive) or its up frame (eyes half open for 0.15 s, then open; the beak's glow half then full over
  the 0.5 s tell) with its collar, and the arms above the water, tip first. A slam arm is laid along
  a path: at rest the pivots' curl under the mantle; in the telegraph it moves under the water
  toward its lane; it rises base to tip along the lane from the deck's edge (the stretch from its
  root to the deck's edge stays under the water, behind the head), lies awash with foam strips both
  sides, sinks tip first over the first half of its sink and drifts back to rest under the water
  over the second; foam clumps where it breaks the surface. Its sprites lie half their size apart
  along the path, tapering 42 → 14 px with the tip's curl at the end. The lane look: the churn tiled
  down the lane (fading in over 0.3 s, until 0.2 s after the impact), the marks blinking at 5 Hz,
  the splash every 48 px from the base, a frame apart; a churn narrower than the lane runs down its
  centre and a mark taller than wide is a dash down both edges (a), else a chevron crawling down
  the centre (b), so both variants draw from the atlas. A severed arm pops segment by segment from
  its root (`harbour-kraken-arm-death`, 3 steps apart) over a water burst. Its death: water bursts
  at its parts, three large ones and its lime-and-crimson burst at the head, the head sinking in its
  foam ring, the grip sliding off the deck, the shadows fading, everything scrolling on with the sea
  from where it died. The foreshadowing (Level 11, t≈55) is its shadow swimming up under the convoy
  from t = 52 for 7 s, turned 18°, fading in and then into the deep; the section-4 "mantle shadow"
  is the Kraken itself scrolling in under the platform (its sub parts drawn before it arrives).
- 2026-10-09: The bounty is **216** in Act 1 terms (head 144, arms 36 each, still 1 : 4 per arm): it
  pays 208 at Level 11's `bounty_scale` of 0.6, **15 %** of budget(11) = 1,377 (was 186: 179, 13 %);
  `BalanceTest`'s band 10–20 %, the typical haul 1,414 (+2.7 %, inside ±5 %). The convoy bonus,
  measured with an arm-first autopilot (it cuts the arm guarding the most ships first and baits the
  slams by these lane rules): no pilot kept the ships with two slams each, since each slam the left
  arm lies awash in strikes Halvorsen's or Mbeki's lane and it takes about six to cut; the cargo
  ships now take four slams at medium (allies, Level 11). The fight itself is unchanged: arm-first
  58.6 s mean at medium on the fixture (eyes first 62.1 s), all three ships afloat on 5 of 5 seeds
  (eyes first 0); hard stays harsh (two slams to a ship, no ship kept).
- 2026-10-09: Fix pass after round 33's capture (three presentation faults): (1) **hit flash**: the
  head turned solid white under sustained fire (the 2-step flash restarted on every hit, so its eyes
  and beak vanished for seconds); the head and the slam arms now flash at most once each 0.25 s
  (2 steps, then no new flash within 15 steps of the last one's start), the head only halfway to
  white so its eyes and beak stay readable through it (art direction, Decisions 2026-10-09);
  (2) **foreshadowing**: its shadow, drawn through the `sub` pass (tinted toward the water's blue
  and composited fainter), did not read at t≈55; it is now drawn over the chop and under the
  convoy, untinted, at the same 0.9 peak, and reads as a huge dark shape passing under the ships;
  (3) **mantle**: the gripping arms' under-water stretches showed through the plain head while it
  was down (drawn at 75 % opacity); the plain head is now opaque and 25 % darker while down
  instead of fainter, so nothing behind it crosses the mantle.
- 2026-10-09: Round 33 (user): four fixes to the Kraken in the arena.
  (1) **The head surfaced and sank through Platform Tiamat** ("That's not possible"): the head's
  centre now lies **216 px** below the platform's centre (152 before), in open water off the deck's
  south edge, every head frame's top below the edge, the gripping arms reaching up from its roots
  and holding on; the death sink is there too. With the platform where it was, the beak would have
  sat in the ship's band (fans silent within the 72 px no-fire distance, spawning on Mbeki in lane 2)
  and the autopilot's convoy result fell from 5/5 to 1/5, so the user chose **(b)**: the arena halt
  raises the platform 32 px (its centre at y **78**, the deck at y 2–154, fully on screen; lanes from
  y 154), the head 32 px lower on the screen than before (centre y 294, the beak y 394). Rejected:
  (a) keep the platform and retune the fight; (c) the platform at y 46, the fight unchanged but the
  deck's top 30 px off the screen. `tools/art/harbour_kraken.py` drops the concept's layout by
  `HEAD_DROP` 64 px (the grip's under-water sprite 344×368 at 184).
  (2) **Weapons made water ripples when they hit it:** a hit on the surfaced head, an arm out of the
  water or Tiamat's deck shows the normal impact or blast; splashes and ripples only for parts under
  the water. (3) **The slamming arms were perfect straight lines:** the arm along its lane bends in a
  travelling S that whips toward the tip as it rises (22 px either side) and settles to a 6 px curl
  while awash (drawing only; the hit area stays the lane's line). (4) **Every hit blinked the whole
  creature:** only the part hit flashes, a third of the way to a pale flesh tint (the arms flashed
  full white), at most once each 0.25 s.
  Measured with the head HP and the fan unchanged (KrakenTest, five seeds): medium eyes first 59.6 s
  (53–70), arm-first 60.6 s with all three ships afloat on 4 of 5 seeds (its slowest seed 75.5 s: the
  arm-first per-seed bound is now 80 s, the mean still 60 ± 6); easy 39.1 s; hard 3 of 5 won (74.8
  s), arm-first 0 of 5 (3 of 5 before: the fan starts closer). Whole level: eyes first medium 64.9 s,
  arm-first medium 48.3 s with 15 of 15 ships afloat; on hard the arm-first pilot needs 4–12 attempts
  (it plays the eyes from its fourth, `Level11Test`).
- 2026-10-09: Concept round 33 closed (user): the production art (the head with its `-sub` body,
  the surfacing, the slam arms, tips and foam, the idle arms, the shadow, the sinking and death, the
  Tiamat grip overlay) and the intel silhouette approved as **final**, the weak spots as they are;
  the lane telegraph and slam **a** "edge dashes + spray sheets" (already in the game) picked, b
  rejected and moved to art-direction's `concept/rejected/`. After the user's feedback on the round
  the head surfaces and sinks off the deck's south edge with the platform raised (option b), shots
  on the surfaced parts hit flesh, the slamming arms curve and whip, and only the hit part flashes;
  the retaken Kraken capture was accepted, hard kept as built ("hard is supposed to be hard"). The
  stat block's numbers and the build choices (f)–(k), (o), (q), (r) and (u) accepted. `design:
  approved`, `art: final`; every Implementation item is ticked, so the implementation is `done`.
