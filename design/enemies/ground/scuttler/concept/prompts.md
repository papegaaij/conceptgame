# Scuttler – generator notes and prompts

## scuttler-final-r16-a

Round 16, production art **proposal** (M4 part D batch), pending part D's doc gaps: the sizes, frame counts, stride and names are the ones asked for while the part's design notes are open, and sit in the parameter block at the top of the generator. Review sheet (`.png`) and loop (`.gif`) made from the files in `assets/` by `tools/art/vrell_l04.py --review scuttler`; the frames are rendered by `tools/art/vrell_l04.py` (see `tools/art/README.md`). Three sets, all 64×64:

- `scuttler_0..95` (32 colours, `16 angles`): the chosen round-08 re-render of the round-05 walker (render/archetype_models.scuttler, geometry copied so the leg swing follows the stride) at 16 headings × 6 walk frames, indexed heading × 6 + frame; heading k walks k × 22.5° clockwise from straight down (heading 0 down the screen, 4 to the left, 12 to the right). One tripod-gait cycle carries it 24 px (40 px/s at 10 fps), so the middle feet stay planted; that needs a leg swing of about ±0.25 rad, half the concept's. The material patterns turn with the body (model space, as in round 08); every heading is its own render under the fixed key light.
- `scuttler-husk_0..15` (24 colours): the legless shell at each heading for its remains: carapace, head and abdomen with broken hip stubs, scorched (albedo ×0.72), the back's glow almost out.
- `scuttler-glow_0..95` (additive, premultiplied on black, 5 colours, same frame order): the lime back seam's emission alone, rendered from the same scene with every other material black, so legs crossing it occlude it; drawn above the dust banks.

The sheet shows the 16 headings, the walk cycle at headings 0, 4 and 10, the husks, the glow masks at heading 4 and the frames at 1×. The loop has a Scuttler walking a circle of 13 whole walk cycles at 40 px/s (7.8 s, the walk frame following the distance), passing under a dust bank with its glow mask added on top. Not an image-generator prompt: the look is the chosen concept's, whose prompt stays valid; this is the brief for reviewing the production frames.
