# Tail Gun — prompts

## tail-gun-final-r28-a

Production art (concept round 28, M5 part A batch), not a mockup: `python3
tools/art/act2_weapon_fx.py` writes the effects into `assets/sprites/` and this review sheet and
GIF (`--review` rebuilds only the review files). The look is the chosen round-08 projectile family
(`tools/concept/vfx_r08.py`, [projectile sheet](../../concept/projectiles-r08-a.png)): the `rear` family's energy bolt (`energy_bolt(10, 2.2)`) drawn at every angle its patterns use (straight back), with the shared pulse muzzle flash and impact.

Artist brief: keep the round-08 family exactly (player shots blue / white / cyan and soft-edged,
physical rounds pre-rendered with the top-left key light); every angle or heading is drawn, never
rotated at runtime; one palette per frame set.
