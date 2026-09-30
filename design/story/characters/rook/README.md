---
title: Lt. Kenji "Rook" Tanaka
design: draft
implementation: n/a
art: none
updated: 2026-09-30
---

# Lt. Kenji "Rook" Tanaka

## Summary

Lancer's wingman and the source of most radio chatter: warnings, banter and the occasional
one-liner. When the player equips an AI wingman, it is Rook flying it (see
[player](../../../player/README.md)).

## Design

- **Role**: in-level radio — wave warnings ("contacts on six"), pickups, near-misses, jokes.
  Flies the optional wingman ship. Speaks in every level except while missing (L27–L28), even
  when the escort slot is empty (then he flies elsewhere in the formation, off-screen).
- **Personality**: cocky, warm, talks too much when nervous. Covers fear with jokes. Loyal.
  Hates flying over water.
- **Voice**: casual, quick, rhetorical questions, pilot slang. The only character allowed a
  pulp one-liner per level.
- **Background**: 30s, from a Luna mining family; joined the CDF to get out of the tunnels.
  Five years in Aegis Wing before Lancer arrived.
- **Arc**: assigned as Lancer's wingman at L08 (before that he flies in the wider formation and
  is heard on the radio). Shot down in the dark at the end of L26; missing and radio silent for
  L27–L28, and captured by Helix subs. The wing frees him at Ceres Hub during the L29 mutiny, and
  he flies as wingman again from L30. During L27–L29 the escort slot can hold only a heavy drone
  (see [wingmen](../../../player/wingmen/README.md)). In Act 7 he refuses to stay behind.
- **Gameplay tie-in**: his radio warnings are useful — "on your six" always means a rear
  attack is coming within a few seconds.

### Sample lines

| Situation | Line |
|---|---|
| Rear attack | "Contacts on six! Why is it always six?" |
| Big wave | "That is a lot of bugs. That is a *lot* of bugs." |
| Pickup | "Ooh, shiny. Grab that." |
| Wingman low armour | "I'm hit — I'm okay — I'm mostly okay!" |
| Boss appears | "Okay. Okay. That's big. We can do big." |
| Water level | "Water. Why is it always water?" |
| Act 5 return | "Miss me? Don't answer that. Just shoot." |

### Portrait brief

Japanese man in his early thirties, messy black hair under a pushed-up flight helmet visor, a
crooked grin, stubble. CDF flight suit, orange-and-navy Aegis Wing patch, oxygen mask hanging
loose on one side. Neutral: grin; alternates: alarmed (wide eyes, shouting), hurt (grimace,
cockpit warning light on his face). Cockpit lighting, late-90s pre-rendered CGI style.

## Decisions

- 2026-09-30: Rook voices the AI wingman; missing for part of Act 4.
- 2026-09-30: Timeline aligned with campaign and wingmen: shot down end of L26, captured, freed at
  Ceres Hub in L29, wingman again from L30; heavy drone allowed in the escort slot meanwhile.
