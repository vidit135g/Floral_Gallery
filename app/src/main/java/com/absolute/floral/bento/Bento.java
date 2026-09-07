package com.absolute.floral.bento;

/** Colour + geometry tokens for the bento mosaic — soft pastels, dark ink. */
public final class Bento {

    private Bento() {}

    /** Two-stop pastel gradients — gentle, easy on the eyes, all distinct. */
    public static final int[][] GRADIENTS = {
            { 0xFFFFE3D3, 0xFFFFC9B0 },   // 0 peach
            { 0xFFD9ECFF, 0xFFBFDDF6 },   // 1 sky
            { 0xFFEAE1FF, 0xFFD6C6F4 },   // 2 lavender
            { 0xFFFFF1D6, 0xFFFFE0B0 },   // 3 butter
            { 0xFFD9F5E4, 0xFFBEEBD0 },   // 4 mint
            { 0xFFFFE0E7, 0xFFFFC7D6 },   // 5 rose
            { 0xFFE3E1FF, 0xFFCBD8F8 },   // 6 periwinkle
            { 0xFFFFEAD8, 0xFFFFD6BC },   // 7 apricot
            { 0xFFE6EFDF, 0xFFCFE3C6 },   // 8 sage
            { 0xFFEDE4FF, 0xFFDCE0FB },   // 9 orchid mist
    };

    public static int[] gradient(int i) {
        return GRADIENTS[((i % GRADIENTS.length) + GRADIENTS.length) % GRADIENTS.length];
    }

    /** All the pastels are light — text is a soft near-black. */
    public static int inkOn(int gradientIndex) { return 0xFF3B3A3E; }

    public static int subInkOn(int gradientIndex) { return 0x8A3B3A3E; }

    public static final float RADIUS = 26f;   // dp — bigger, friendlier corners
    public static final float GAP = 9f;
    public static final int COLS = 6;
}
