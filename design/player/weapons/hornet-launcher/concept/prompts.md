# Hornet Launcher — prompts

## hornet-launcher-final-r28-a

Production art (concept round 28, M5 part A batch), not a mockup: `python3
tools/art/act2_weapon_fx.py` writes the effects into `assets/sprites/` and this review sheet and
GIF (`--review` rebuilds only the review files). The look is the chosen round-08 projectile family
(`tools/concept/vfx_r08.py`, [projectile sheet](../../concept/projectiles-r08-a.png)): the Hornet missile (`missile_model` at 16 px with its plume) at 32 headings with the key light fixed, the six-frame smoke-trail puff (`smoke_puff`, stepped translucency), the shared launcher muzzle flash and explosive impact.

Artist brief: keep the round-08 family exactly (player shots blue / white / cyan and soft-edged,
physical rounds pre-rendered with the top-left key light); every angle or heading is drawn, never
rotated at runtime; one palette per frame set.
