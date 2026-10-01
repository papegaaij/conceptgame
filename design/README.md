---
title: Game design
design: approved
implementation: not-started
art: chosen
updated: 2026-10-01
---

# Game design

## Summary

A vertical shoot'em up with a late-90s pre-rendered CGI look. In 2185 the alien Vrell invade
the solar system; the player flies for the United Terran Coalition through 50 levels in 7 acts,
from Earth orbit to the far side of the Vrell gate. Between levels the player spends earned
credits in the hangar on weapons, generator, shields, armour, wingmen and special abilities,
guided by intel about the next level.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [art-direction](art-direction/README.md) | Visual style, resolution, palette, sprite rules, parallax layer model | approved | n/a | chosen |
| [story](story/README.md) | Premise, factions, characters, timeline and the mid-campaign twist | approved | n/a | chosen |
| [world](world/README.md) | The settings the levels take place in | approved | n/a | chosen |
| [campaign](campaign/README.md) | Acts and the 50-level outline, pacing and difficulty curve | approved | not-started | none |
| [enemies](enemies/README.md) | Enemy roster, behaviours, formations and bosses | approved | not-started | chosen |
| [allies](allies/README.md) | Friendly units and structures to escort or defend (crawlers, shuttles, convoy, relay) | approved | not-started | none |
| [player](player/README.md) | The player ship, its loadout slots and all equipment | approved | not-started | chosen |
| [systems](systems/README.md) | Economy, scoring, difficulty, retry and saves | approved | not-started | n/a |
| [ui](ui/README.md) | Main menu, hangar, briefing screen and HUD | approved | not-started | chosen |
| [audio](audio/README.md) | Music and sound effects | approved | not-started | chosen |
| [tech](tech/README.md) | Tech stack: libGDX on Java, Gradle, desktop only, Apache-2.0 | approved | not-started | n/a |
| [concept-rounds](concept-rounds/README.md) | Batches of concept proposals awaiting the user's choice | review | n/a | chosen |
| [reviews](reviews/README.md) | Formal approval rounds for sets of design documents | approved | n/a | n/a |

## Design

### Pillars

1. **Arcade action first.** Readable bullet patterns, satisfying weapons, big explosions.
2. **Preparation matters.** Every level rewards a different loadout; the hangar tells you what
   is coming, and choosing well is part of the game.
3. **A war worth fighting.** A story told through briefings and radio chatter, serious in tone
   with pulp edges: memorable characters, a bombastic villain, the odd one-liner.
4. **Late-90s craft.** Pre-rendered CGI sprites, deep parallax, tracker-era music.

### Fundamentals

- **View**: top-down vertical scroller, 16:9 screen with a portrait play field in the centre and
  HUD panels left and right. See [art-direction](art-direction/README.md).
- **Depth**: several parallax layers; enemies live on different layers (ground turrets, low
  flyers, the player's plane, high flyers). See [art-direction](art-direction/README.md).
- **Campaign**: 7 linear acts, about 7 levels each, 50 in total, a boss at the end of every act.
  See [campaign](campaign/README.md).
- **Loadout**: front gun, rear gun, left and right wing mounts, generator, shield, armour,
  special ability. See [player](player/README.md).
- **Failure**: shields regenerate, armour does not. At zero armour the level is retried and the
  credits earned in that attempt are lost. See [systems](systems/README.md).
- **Players**: single-player; the wingman is AI-controlled.
- **Difficulty**: easy, medium, hard, chosen when starting a new game.
- **Tech**: libGDX on Java 21 with Gradle, desktop only; see [tech](tech/README.md). The
  rest of the design stays engine-agnostic.

### Shared vocabulary

Used across level, enemy, weapon and hangar-intel documents so they can be compared:

- **Layers**: `deep` (background), `ground`, `low-air`, `air` (player's plane), `high-air`,
  `sub` (under water), `space`.
- **Attack directions**: `front`, `sides`, `rear`, `all`.
- **Weapon traits**: `forward`, `spread`, `rear`, `side`, `homing`, `piercing`, `beam`,
  `anti-ground`, `anti-sub`, `area`, `shield-breaker`.
- **Threat profile** (per level, shown in the hangar intel panel): dominant layers, attack
  directions, density (1–5), recommended weapon traits, hazards, boss.

## Open questions

- None open.

## Decisions

- 2026-09-30: Structure by domain, `README.md` per directory, variants kept in
  `concept/rejected/`, binaries in Git LFS.
- 2026-09-30: Pre-rendered CGI sprite look; 16:9 with side HUD panels; engine decided later.
- 2026-09-30: Alien invasion with a mid-campaign twist; serious tone with pulp edges;
  briefings plus radio chatter; 7 linear acts by setting.
- 2026-09-30: Armour bar with level retry; single-player; Tyrian-style loadout slots.
- 2026-09-30: Working title: **Terran Vanguard** (concept round 01, logo D).
- 2026-10-01: Acts 1–2 open questions settled (layer hit rules, chain regrow, objective failure, controls, escort slot, utility bays, one special, visible pods, game over on hard, armour on retry, no level select, 5 banking frames, scaling, test fire later, text-only voices); score and credits stay separate. The tech stack is explicitly left open for a thorough evaluation.
- 2026-10-01: New top-level part [allies](allies/README.md) for friendly units and structures (user decision).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](reviews/acts-1-2/README.md).
- 2026-10-01: Tech stack decided after the spike: libGDX on Java with Gradle, Apache-2.0 (user decision, see [tech](tech/README.md)).
