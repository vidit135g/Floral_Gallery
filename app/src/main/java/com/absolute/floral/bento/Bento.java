package com.absolute.floral.bento;

/** Colour + geometry tokens for the bento mosaic. */
public final class Bento {

    private Bento() {}

    /** Two-stop gradients — bright, playful, distinct. */
    public static final int[][] GRADIENTS = {
            { 0xFFFF9A8B, 0xFFFF6A88 },   // 0 sunset
            { 0xFF5EE7DF, 0xFF66A6FF },   // 1 lagoon
            { 0xFFA18CD1, 0xFFFBC2EB },   // 2 grape
            { 0xFFF6D365, 0xFFFDA085 },   // 3 amber
            { 0xFF43E97B, 0xFF38F9D7 },   // 4 mint
            { 0xFF4FACFE, 0xFF00F2FE },   // 5 sky
            { 0xFFFF6FD8, 0xFF3813C2 },   // 6 magenta-violet
            { 0xFFFDA085, 0xFFF6D365 },   // 7 peach
            { 0xFF30CFD0, 0xFF330867 },   // 8 deep teal
            { 0xFFE0C3FC, 0xFF8EC5FC },   // 9 lilac
    };

    public static int[] gradient(int i) {
        return GRADIENTS[((i % GRADIENTS.length) + GRADIENTS.length) % GRADIENTS.length];
    }

    /** On-gradient text colour (all our gradients are light enough for dark ink except a few). */
    public static int inkOn(int gradientIndex) {
        switch (gradientIndex) {
            case 6: case 8: return 0xFFFFFFFF;
            default: return 0xFF1C1B1F;
        }
    }

    public static int subInkOn(int gradientIndex) {
        return (inkOn(gradientIndex) & 0x00FFFFFF) | 0xB0000000;
    }

    public static final float RADIUS = 24f;   // dp
    public static final float GAP = 8f;       // dp between tiles
    public static final int COLS = 6;         // mosaic column count
}
