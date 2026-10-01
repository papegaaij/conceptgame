---
title: Credits
design: draft
implementation: not-started
art: chosen
depends-on: [../../../CREDITS.md]
updated: 2026-10-01
---

# Credits

## Summary

Scrolling credits screen from the main menu (and after the campaign ends). Carries the attributions that the CC-BY licences of third-party assets require.

## Design

- Glass style; a slowly scrolling column over the menu scene; skip/back at any time.
- Sections: team roles, music, sound (with the **CC-BY attributions** — title, author, licence —
  generated from [CREDITS.md](../../../CREDITS.md), skipping rejected files), thanks to the CC0
  authors, tools.
- The list is generated at build time from CREDITS.md so it can never drift from the files
  actually shipped.

## Concept art

Concept [round 08](../../concept-rounds/round-08/README.md) — glass style; generator
`tools/concept/ui_r08.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/credits-r08-a.png](concept/credits-r08-a.png) | Credits — scrolling glass column with placeholder roles, the CC-BY sound attributions from CREDITS.md and thanks to CC0 authors | chosen |

## Implementation

- [ ] Scrolling credits with skip
- [ ] CC-BY attributions generated from CREDITS.md for every shipped CC-BY asset
- [ ] Reachable from the main menu and after the final level

## Decisions

- 2026-10-01: Screen added for the Acts 1–2 vertical slice (concept round 08).
- 2026-10-01: Concept round 08: accepted.
