#!/usr/bin/env python3
"""Regenerate every visual concept mockup of round 01 (takes about two minutes).

Run: python3 tools/concept/make_all.py
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import hud  # noqa: E402
import logos  # noqa: E402
import palettes  # noqa: E402
import parallax  # noqa: E402
import player_ship  # noqa: E402

if __name__ == "__main__":
    sys.argv = sys.argv[:1]
    for module in (player_ship, palettes, parallax, hud, logos):
        module.main()   # parallax and hud share the cached scene renders
