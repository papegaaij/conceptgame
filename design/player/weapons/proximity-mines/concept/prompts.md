# Proximity Mines — prompts

## proximity-mines-final-r28-a

Production art (concept round 28, M5 part A batch), not a mockup: `python3
tools/art/act2_weapon_fx.py` writes the effects into `assets/sprites/` and this review sheet and
GIF (`--review` rebuilds only the review files). The look is the chosen round-08 projectile family
(`tools/concept/vfx_r08.py`, [projectile sheet](../../concept/projectiles-r08-a.png)): the mine (`mine_model` at 12 px) with its sensor dark (unarmed) and lit at three strengths (the armed pulse), the 12-frame blast (the round-09 fireball with a blue shock ring out to the 48 px radius) and the shared launcher muzzle flash.

Artist brief: keep the round-08 family exactly (player shots blue / white / cyan and soft-edged,
physical rounds pre-rendered with the top-left key light); every angle or heading is drawn, never
rotated at runtime; one palette per frame set.
