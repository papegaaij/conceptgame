"""Shared screen geometry and output helpers for the concept generators.

The current internal resolution is **960x540** (decided after concept round 01, see
design/art-direction). Round 01 mockups were made at 640x360; those values are kept in ``R01``
so the round 01 scripts stay reproducible.
"""
from pathlib import Path

SCREEN_W, SCREEN_H = 960, 540      # internal resolution, integer-scaled on real displays
FIELD_W, FIELD_H = 480, 540        # portrait play field
FIELD_X = 240                      # play field starts here; side panels are 240 wide
PANEL_W = 240
SHIP_SIZE = 48                     # player ship sprite (AF-12 Stormhawk)
WINGMAN_SIZE = 40                  # Rook's craft
SCALE = 1.5                        # sprite scale relative to round 01

R01 = {"SCREEN_W": 640, "SCREEN_H": 360, "FIELD_W": 320, "FIELD_H": 360, "FIELD_X": 160,
       "PANEL_W": 160, "SHIP_SIZE": 32}

ROOT = Path(__file__).resolve().parents[3]


def out_path(concept_dir, name):
    """Where a concept file lives: in concept/rejected/ if it was rejected, else concept/.

    Regenerating an older round therefore overwrites files in place instead of re-creating
    rejected variants next to the chosen ones."""
    concept_dir = Path(concept_dir)
    rejected = concept_dir / "rejected" / name
    return rejected if rejected.exists() else concept_dir / name
