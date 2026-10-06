# Fan Blaster — prompts

## fan-blaster-final-r28-a

Production art (concept round 28, M5 part A batch), not a mockup: `python3
tools/art/act2_weapon_fx.py` writes the effects into `assets/sprites/` and this review sheet and
GIF (`--review` rebuilds only the review files). The look is the chosen round-08 projectile family
(`tools/concept/vfx_r08.py`, [projectile sheet](../../concept/projectiles-r08-a.png)): the `rear` family's energy bolt, shorter and rounder for the fan's 6×6 hit box (`energy_bolt(7, 2.5)`), drawn at every angle of its fans (145–215°), with the shared pulse muzzle flash and impact.

Artist brief: keep the round-08 family exactly (player shots blue / white / cyan and soft-edged,
physical rounds pre-rendered with the top-left key light); every angle or heading is drawn, never
rotated at runtime; one palette per frame set.
