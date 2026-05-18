package com.aichess.engine.movegen;

import com.aichess.engine.board.BitboardUtils;

public class PrecomputedAttacks {
    public static final long[] KNIGHT_ATTACKS = new long[64];
    public static final long[] KING_ATTACKS = new long[64];
    public static final long[][] PAWN_ATTACKS = new long[2][64]; // [color][square]

    public static final long FILE_A = 0x0101010101010101L;
    public static final long FILE_B = FILE_A << 1;
    public static final long FILE_H = 0x8080808080808080L;
    public static final long FILE_G = FILE_H >>> 1;
    
    public static final long RANK_1 = 0x00000000000000FFL;
    public static final long RANK_4 = 0x00000000FF000000L;
    public static final long RANK_5 = 0x000000FF00000000L;
    public static final long RANK_8 = 0xFF00000000000000L;

    static {
        initKnightAttacks();
        initKingAttacks();
        initPawnAttacks();
    }

    private static void initKnightAttacks() {
        int[] knightJumps = {-17, -15, -10, -6, 6, 10, 15, 17};
        for (int sq = 0; sq < 64; sq++) {
            long bb = BitboardUtils.setBit(0L, sq);
            long attacks = 0;
            
            attacks |= (bb << 17) & ~FILE_A;
            attacks |= (bb << 15) & ~FILE_H;
            attacks |= (bb << 10) & ~(FILE_A | FILE_B);
            attacks |= (bb << 6) & ~(FILE_H | FILE_G);
            
            attacks |= (bb >>> 17) & ~FILE_H;
            attacks |= (bb >>> 15) & ~FILE_A;
            attacks |= (bb >>> 10) & ~(FILE_H | FILE_G);
            attacks |= (bb >>> 6) & ~(FILE_A | FILE_B);
            
            KNIGHT_ATTACKS[sq] = attacks;
        }
    }

    private static void initKingAttacks() {
        for (int sq = 0; sq < 64; sq++) {
            long bb = BitboardUtils.setBit(0L, sq);
            long attacks = 0;
            
            attacks |= (bb << 8) | (bb >>> 8);
            attacks |= ((bb << 1) | (bb << 9) | (bb >>> 7)) & ~FILE_A;
            attacks |= ((bb >>> 1) | (bb >>> 9) | (bb << 7)) & ~FILE_H;
            
            KING_ATTACKS[sq] = attacks;
        }
    }

    private static void initPawnAttacks() {
        for (int sq = 0; sq < 64; sq++) {
            long bb = BitboardUtils.setBit(0L, sq);
            
            // White pawns (capture UP-LEFT and UP-RIGHT)
            PAWN_ATTACKS[0][sq] |= (bb << 7) & ~FILE_H;
            PAWN_ATTACKS[0][sq] |= (bb << 9) & ~FILE_A;
            
            // Black pawns (capture DOWN-LEFT and DOWN-RIGHT)
            PAWN_ATTACKS[1][sq] |= (bb >>> 9) & ~FILE_H;
            PAWN_ATTACKS[1][sq] |= (bb >>> 7) & ~FILE_A;
        }
    }
}
