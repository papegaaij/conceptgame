package vanguard.pipeline;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Cuts the placeholder sprites out of the <em>chosen</em> concept sheets in the design tree and
 * writes them as single frames into {@code assets/sprites}, ready for {@link AtlasPacker}. (The
 * level backdrops are rendered into {@code assets/backdrop} by tools/concept/backdrop_l01.py, the loot targets
 * without concept art into {@code assets/sprites} by tools/concept/ground_targets.py.) Every crop rectangle below
 * was read off the generator that drew the sheet
 * (named per cut), so a re-rendered sheet with the same layout imports unchanged.
 *
 * <p>The sheets show sprites on a checkerboard or a flat plate. Opaque sprites have hard 1-bit
 * edges, so their background colours are keyed to transparency. Glowing sprites (bolts, flashes,
 * explosions) are soft-edged: for them the background colour is subtracted, which
 * leaves what the glow added on top of it, ready for additive blending. Zoomed frames are sampled
 * at the centre of every zoom block, which gives back the native pixels.
 *
 * <p>A part whose final art exists is left alone: final sprites (rendered by tools/art/) carry a
 * {@code Source} PNG text chunk naming their generator, so a cut is skipped when its first frame in
 * {@code assets/sprites} has one ({@link FinalArt}).
 *
 * <p>Usage: {@code PlaceholderSprites <designDir> <assetsDir>}
 */
public final class PlaceholderSprites {
    /** The checkerboard colours of {@code ships_r02.checker} and {@code enemies_r03.checker}. */
    private static final int SPRITE_CHECKER_EVEN = 0x1E222E;

    private static final int SPRITE_CHECKER_ODD = 0x262A38;
    /** The dark plate behind the explosion frames ({@code vfx_r09.explosions_sheet}). */
    private static final int EXPLOSION_PLATE = 0x0A0C1A;
    /** {@code raster.LABEL_DIM}: the frame numbers printed into the explosion frames' top-left corner. */
    private static final int FRAME_LABEL = 0x6E7891;

    /** How far (per channel) a faded glow pixel may be from the background and still be keyed. */
    private static final int GLOW_TOLERANCE = 12;

    private static final int LABEL_WIDTH = 14;
    private static final int LABEL_HEIGHT = 10;

    private static final String SHIP_SHEET = "player/ship/concept/player-ship-r08-a.png";
    private static final String SKITTER_SHEET = "enemies/air/concept/skitter-r04-a.png";
    private static final String NEEDLER_SHEET = "enemies/air/concept/needler-r04-a.png";
    private static final String BULLET_SHEET = "enemies/concept/enemy-bullets-r09-a.png";
    private static final String PICKUP_SHEET = "player/concept/pickups-r09-a.png";
    private static final String PROJECTILE_SHEET = "player/weapons/concept/projectiles-r08-a.png";
    private static final String EXPLOSION_SHEET = "art-direction/concept/explosions-r09-a.png";

    /** How the background behind a sprite is removed. */
    enum Treatment {
        /** Kept as it is: a full-frame image without background. */
        OPAQUE,
        /** Background pixels become transparent. */
        KEYED,
        /**
         * Pixels within {@link #GLOW_TOLERANCE} of the background become transparent: for an opaque
         * sprite whose faint outer glow fades into the background.
         */
        KEYED_GLOW,
        /** The background colour is subtracted, for additive blending. */
        ADDITIVE
    }

    /** The background behind a frame: its colour at a sheet pixel. */
    sealed interface Background permits Checker, Plate {
        int rgbAt(int x, int y, int frame);
    }

    /**
     * A checkerboard of {@code cell} px squares starting with the even colour at its origin; the
     * origin moves {@code originStepX} per frame when every frame has its own board.
     */
    record Checker(int originX, int originY, int originStepX, int cell, int even, int odd) implements Background {
        @Override
        public int rgbAt(int x, int y, int frame) {
            int column = Math.floorDiv(x - originX - frame * originStepX, cell);
            int row = Math.floorDiv(y - originY, cell);
            return (column + row) % 2 == 0 ? even : odd;
        }
    }

    /** A flat colour. */
    record Plate(int rgb) implements Background {
        @Override
        public int rgbAt(int x, int y, int frame) {
            return rgb;
        }
    }

    /**
     * One sprite or animation cut from a sheet.
     *
     * @param name file name; frames get {@code _<index>}, which the atlas turns into an indexed region
     * @param sheet the concept sheet, relative to the design directory
     * @param x left edge of the first frame in the sheet
     * @param y top edge of the first frame in the sheet
     * @param width frame width in the sheet (zoomed)
     * @param height frame height in the sheet (zoomed)
     * @param zoom the sheet's zoom factor; the output is {@code width / zoom} wide
     * @param frames number of frames, side by side
     * @param stepX distance between two frames in the sheet
     * @param background what is behind the frames
     * @param treatment how the background is removed
     * @param frameLabels whether a frame number is printed into the top-left corner
     * @param size the output edge length when the stat block's size differs from the concept's,
     *     or {@link #NATIVE_SIZE} to keep the sheet's native pixels
     */
    record Cut(
            String name,
            String sheet,
            int x,
            int y,
            int width,
            int height,
            int zoom,
            int frames,
            int stepX,
            Background background,
            Treatment treatment,
            boolean frameLabels,
            int size) {}

    /** Keep a cut frame at the sheet's native size. */
    static final int NATIVE_SIZE = 0;

    static final List<Cut> CUTS = List.of(
            // vfx_r08.ship_sheet: the 1x strip of the 5 banking frames (hard left .. hard right).
            new Cut(
                    "ship",
                    SHIP_SHEET,
                    423,
                    232,
                    48,
                    48,
                    1,
                    5,
                    54,
                    spriteChecker(420, 228, 0, 8),
                    Treatment.KEYED,
                    false,
                    NATIVE_SIZE),
            // enemies_r03.enemy_sheet (r04 colours): the 3 wing-beat frames at 3x, drawn 30x30 there but
            // 24x24 in the Skitter's stat block, so scaled down.
            new Cut(
                    "skitter",
                    SKITTER_SHEET,
                    16,
                    318,
                    90,
                    90,
                    3,
                    3,
                    100,
                    spriteChecker(16, 318, 100, 8),
                    Treatment.KEYED,
                    false,
                    24),
            // enemies_r03.enemy_sheet (r04 colours): the Needler's 3 claw-snap frames at 3x, 36x36 like its stat block.
            new Cut(
                    "needler",
                    NEEDLER_SHEET,
                    16,
                    366,
                    108,
                    108,
                    3,
                    3,
                    118,
                    spriteChecker(16, 366, 118, 8),
                    Treatment.KEYED,
                    false,
                    NATIVE_SIZE),
            // vfx_r09.bullets_sheet, orb row: the first frame of the Vrell standard orb at 3x with a 1 px
            // pad; its outer glow fades into the checkerboard, which is keyed up to the glow tolerance.
            new Cut(
                    "orb",
                    BULLET_SHEET,
                    465,
                    63,
                    45,
                    45,
                    3,
                    1,
                    0,
                    spriteChecker(464, 62, 0, 6),
                    Treatment.KEYED_GLOW,
                    false,
                    NATIVE_SIZE),
            // vfx_r09.pickups_sheet: the 8-frame spin loops at 2x of salvage S, salvage L (the crate),
            // the shield cell and the armour patch (rows 0, 2, 3 and 4).
            pickup("salvage-small", 0, 28),
            pickup("crate", 2, 34),
            pickup("shield-cell", 3, 32),
            pickup("armour-patch", 4, 32),
            // vfx_r08.projectiles_sheet, pulse row: the 1x bolt, 3 muzzle frames and 4 impact frames at 3x.
            new Cut(
                    "pulse-bolt",
                    PROJECTILE_SHEET,
                    252,
                    60,
                    14,
                    26,
                    1,
                    1,
                    0,
                    spriteChecker(250, 58, 0, 4),
                    Treatment.ADDITIVE,
                    false,
                    NATIVE_SIZE),
            new Cut(
                    "pulse-muzzle",
                    PROJECTILE_SHEET,
                    430,
                    58,
                    60,
                    60,
                    3,
                    3,
                    66,
                    spriteChecker(430, 58, 66, 6),
                    Treatment.ADDITIVE,
                    false,
                    NATIVE_SIZE),
            new Cut(
                    "pulse-impact",
                    PROJECTILE_SHEET,
                    640,
                    58,
                    66,
                    66,
                    3,
                    4,
                    70,
                    spriteChecker(640, 58, 70, 4),
                    Treatment.ADDITIVE,
                    false,
                    NATIVE_SIZE),
            // vfx_r09.explosions_sheet: the tiny rung (24 px, all 12 frames), the small rung (40 px, all
            // 12) and the large rung (96 px, 11 of its 14 frames as sampled on the sheet: 1, 2, 4, 5, 6, 7,
            // 9, 10, 11, 13, 14).
            new Cut(
                    "explosion-tiny",
                    EXPLOSION_SHEET,
                    130,
                    56,
                    24,
                    24,
                    1,
                    12,
                    28,
                    new Plate(EXPLOSION_PLATE),
                    Treatment.ADDITIVE,
                    true,
                    NATIVE_SIZE),
            new Cut(
                    "explosion-small",
                    EXPLOSION_SHEET,
                    130,
                    106,
                    40,
                    40,
                    1,
                    12,
                    44,
                    new Plate(EXPLOSION_PLATE),
                    Treatment.ADDITIVE,
                    true,
                    NATIVE_SIZE),
            new Cut(
                    "explosion-large",
                    EXPLOSION_SHEET,
                    130,
                    230,
                    96,
                    96,
                    1,
                    11,
                    100,
                    new Plate(EXPLOSION_PLATE),
                    Treatment.ADDITIVE,
                    true,
                    NATIVE_SIZE));

    private PlaceholderSprites() {}

    /** A pickup's spin loop: row {@code row} of the pickups sheet, {@code width} px with its outline. */
    private static Cut pickup(String name, int row, int width) {
        int y = 54 + row * 112 + 10;
        int step = width * 2 + 6;
        return new Cut(
                "pickup-" + name,
                PICKUP_SHEET,
                420,
                y,
                width * 2,
                width * 2,
                2,
                8,
                step,
                spriteChecker(420, y, step, 4),
                Treatment.KEYED,
                false,
                NATIVE_SIZE);
    }

    private static Checker spriteChecker(int x, int y, int stepX, int cell) {
        return new Checker(x, y, stepX, cell, SPRITE_CHECKER_EVEN, SPRITE_CHECKER_ODD);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PlaceholderSprites <designDir> <assetsDir>");
        }
        Path design = Path.of(args[0]);
        Path folder = Files.createDirectories(Path.of(args[1]).resolve("sprites"));
        for (Cut cut : CUTS) {
            if (FinalArt.exists(folder, cut.name())) {
                System.out.println("final art, no placeholder cut: " + cut.name());
                continue;
            }
            BufferedImage sheet = ImageIO.read(design.resolve(cut.sheet()).toFile());
            for (int frame = 0; frame < cut.frames(); frame++) {
                String file = cut.frames() == 1 ? cut.name() : cut.name() + "_" + frame;
                ImageIO.write(
                        cutFrame(sheet, cut, frame),
                        "png",
                        folder.resolve(file + ".png").toFile());
            }
        }
    }

    static BufferedImage cutFrame(BufferedImage sheet, Cut cut, int frame) {
        int zoom = cut.zoom();
        int width = cut.width() / zoom;
        int height = cut.height() / zoom;
        int left = cut.x() + frame * cut.stepX();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int x = left + column * zoom + zoom / 2;
                int y = cut.y() + row * zoom + zoom / 2;
                if (cut.frameLabels() && isFrameLabel(sheet, x - left, y - cut.y(), x, y)) {
                    // The point mirror through the frame centre stands in for the hidden pixel.
                    x = left + cut.width() - 1 - (x - left);
                    y = cut.y() + cut.height() - 1 - (y - cut.y());
                }
                int rgb = sheet.getRGB(x, y) & 0xFFFFFF;
                int background = cut.background().rgbAt(x, y, frame);
                out.setRGB(column, row, treat(cut.treatment(), rgb, background));
            }
        }
        return cut.size() == NATIVE_SIZE ? out : scaled(out, cut.size());
    }

    /** Area-averages a frame down to {@code size} x {@code size}; edges become partly transparent. */
    private static BufferedImage scaled(BufferedImage frame, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = out.createGraphics();
        graphics.drawImage(frame.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        graphics.dispose();
        return out;
    }

    private static boolean isFrameLabel(BufferedImage sheet, int frameX, int frameY, int x, int y) {
        return frameX < LABEL_WIDTH && frameY < LABEL_HEIGHT && (sheet.getRGB(x, y) & 0xFFFFFF) == FRAME_LABEL;
    }

    static int treat(Treatment treatment, int rgb, int background) {
        return switch (treatment) {
            case OPAQUE -> 0xFF000000 | rgb;
            case KEYED -> rgb == background ? 0 : 0xFF000000 | rgb;
            case KEYED_GLOW -> near(rgb, background) ? 0 : 0xFF000000 | rgb;
            case ADDITIVE ->
                0xFF000000
                        | subtract(rgb, background, 16)
                        | subtract(rgb, background, 8)
                        | subtract(rgb, background, 0);
        };
    }

    private static boolean near(int rgb, int background) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs(((rgb >> shift) & 0xFF) - ((background >> shift) & 0xFF)) > GLOW_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static int subtract(int rgb, int background, int shift) {
        int channel = Math.max(0, ((rgb >> shift) & 0xFF) - ((background >> shift) & 0xFF));
        return channel << shift;
    }
}
