package com.absolute.floral.bento;

/** Colour + geometry tokens for the bento mosaic — deep, dimensional pastels with a soft top sheen. */
public final class Bento {

    private Bento() {}

    /**
     * Three-stop gradients: a bright top-left, a mid body, a saturated bottom-right.
     * Drawn on a ~28° diagonal so light appears to fall from the top-left, with a
     * white radial sheen layered on top by {@link BentoTile}.
     */
    public static final int[][] GRADIENTS = {
            { 0xFFFFEEDF, 0xFFFFD8BE, 0xFFFFC29B },   // 0 peach
            { 0xFFE4F2FF, 0xFFC4E0FA, 0xFF9FCBF4 },   // 1 sky
            { 0xFFF1EBFF, 0xFFDBCBFA, 0xFFC3ABF2 },   // 2 lavender
            { 0xFFFFF6E1, 0xFFFFE7B4, 0xFFFCD68B },   // 3 butter
            { 0xFFE2F8EC, 0xFFC2EED6, 0xFF9FE3BC },   // 4 mint
            { 0xFFFFE9EF, 0xFFFFCBDA, 0xFFFCADC3 },   // 5 rose
            { 0xFFEBEAFF, 0xFFCFD6FB, 0xFFB2BEF6 },   // 6 periwinkle
            { 0xFFFFF0E2, 0xFFFFD8BD, 0xFFFCC195 },   // 7 apricot
            { 0xFFECF3E4, 0xFFD3E5C4, 0xFFB6D6A0 },   // 8 sage
            { 0xFFF3ECFF, 0xFFE0DEFC, 0xFFC9C7F6 },   // 9 orchid mist
            { 0xFFDFF6F4, 0xFFB9E9E4, 0xFF93DBD3 },   // 10 seafoam
            { 0xFFFFEBF3, 0xFFFAD0E6, 0xFFF3B2D6 },   // 11 blossom
    };

    public static int[] gradient(int i) {
        return GRADIENTS[wrap(i)];
    }

    private static int wrap(int i) {
        int n = GRADIENTS.length;
        return ((i % n) + n) % n;
    }

    /** All the pastels are light — text is a soft near-black. */
    public static int inkOn(int gradientIndex) { return 0xFF34333A; }

    public static int subInkOn(int gradientIndex) { return 0x9934333A; }

    /** A translucent chip behind an icon so it reads on the brightest part of a tile. */
    public static int chipOn(int gradientIndex) { return 0x22FFFFFF; }

    /** Hue-matched ambient shadow so tiles feel like they float in their own colour. */
    public static int shadowOn(int gradientIndex) {
        int deep = gradient(gradientIndex)[2];
        int r = (deep >> 16) & 0xFF, g = (deep >> 8) & 0xFF, b = deep & 0xFF;
        // darken toward the hue, keep it soft
        r = (int) (r * 0.55f); g = (int) (g * 0.55f); b = (int) (b * 0.55f);
        return (0x4D << 24) | (r << 16) | (g << 8) | b;
    }

    public static final float RADIUS = 28f;   // dp — generous, friendly corners
    public static final float GAP = 10f;
    public static final int COLS = 6;
}
