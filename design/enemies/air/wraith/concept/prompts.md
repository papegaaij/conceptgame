# Wraith – generator notes and prompts

The chosen concept is [wraith-r06-a](../../concept/prompts.md#wraith-r06-a) (round 06, in the air
units' `concept/`); its prompt stays valid for the look.

## wraith-final-r32-a

Round 32, production art (M5 part D batch, user decision D10 = a: straight to production from the
chosen concept; Level 10 flies at first light, D11 = a). Review sheet (`.png`) and loop (`.gif`)
made from the files in `assets/` by `python3 tools/art/wraith.py --review`; the frames are
rendered by `python3 tools/art/wraith.py` (see `tools/art/README.md`).

- `wraith_0..63` (72×72, 40 colours, `16 angles`): the round-06 model
  (`tools/concept/render/r06_models.wraith`, imported unchanged) at 16 headings × 4 wing-ripple
  frames, indexed heading × 4 + frame; heading k flies k × 22.5° clockwise from straight down
  (0 down the screen, 4 left, 8 up, 12 right). The blue-violet veins and the eye glands are emissive
  in the frame (an air unit has no separate glow mask). **Dawn readability**: the Creeper's 1 px
  lavender-white light rim at 0.8 strength (the night levels' was full). The stat block's ×1.5
  veins are struck (part D's stated default): nothing marks them as a weak point.
- `wraith-cloak_0..63` (90×90, additive, 16 colours, same order): the cloaked shimmer at the
  high-air scale (1.25×, so the game never scales it): a broken lavender glint running round the
  silhouette's edge from frame to frame, faint stepped refraction bands over the membrane and the
  veins and eyes as a faint violet ghost (28 %). Drawn instead of the body while cloaked, no shadow.
- `wraith-decloak_0..5` (112×112, additive, 4 steps a frame = 0.4 s): a white-violet core from the
  eye glands, a violet bloom over the span, an expanding ring, violet sparks.
- `wraith-death_0..9` (96×96, additive, 4 steps a frame): the `medium` death's white-violet flash,
  violet bloom, a breaking shell of light, sparks and a dark violet mist.
- `wraith-tatters_0..9` (96×96, solid, 4 steps a frame): ten veined membrane tatters torn from the
  wings, the bone spine in two, the whip tail in three, the four veil tendrils, the body split fore
  and aft and the cephalic lobes, flung out, tumbling and shrivelling.

The sheet shows the 16 headings, the ripple at headings 0 and 8, the shimmer alone and over a dawn
stand-in (Level 08's avenues lifted and graded toward a cool morning, since Level 10's backdrop is
not built yet), the flash, the decloak as drawn (shimmer fading over 12 steps, the flash, the body
fading in over 24), the death pieces and glow with `explosion-medium`, and the frames at 1×. The
loop: it swoops down cloaked at 200 px/s past the ship and leaves the bottom edge, the 3 s edge
warning blinks, it rises back in cloaked, decloaks, holds facing up and fires two 5-shot `medium`
orb bursts straight up the screen as a fixed 40° fan (aimed at nobody; each burst's shots 0.12 s
apart at 220 px/s, in turn from the fan's left edge to its right, 0.5 s and 1.7 s into the hold),
exits up a side lane decloaked at 120 px/s and is shot down. Not an image-generator
prompt: this is the brief for reviewing the production frames.
