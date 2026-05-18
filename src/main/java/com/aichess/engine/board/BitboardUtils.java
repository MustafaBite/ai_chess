package com.aichess.engine.board;

public class BitboardUtils {

    // Set a bit at the given square
    public static long setBit(long bb, int square) {
        return bb | (1L << square);
    }

    // Clear a bit at the given square
    public static long clearBit(long bb, int square) {
        return bb & ~(1L << square);
    }

    // Check if a bit is set
    public static boolean getBit(long bb, int square) {
        return (bb & (1L << square)) != 0;
    }

    // Count the number of set bits (population count)
    public static int popCount(long bb) {
        return Long.bitCount(bb);
    }

    // Get the index of the least significant 1-bit (0-63)
    public static int lsb(long bb) {
        return Long.numberOfTrailingZeros(bb);
    }

    // Pop the least significant 1-bit and return its index
    // Note: bb needs to be updated by the caller: bb &= bb - 1;
    // but typically we do a loop using Long.numberOfTrailingZeros

    public static void printBitboard(long bb) {
        System.out.println("  a b c d e f g h");
        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + " ");
            for (int file = 0; file < 8; file++) {
                int sq = rank * 8 + file;
                System.out.print(getBit(bb, sq) ? "1 " : ". ");
            }
            System.out.println();
        }
        System.out.println("Bitboard value: " + bb + "L\n");
    }
}
