package com.aichess.engine.board;

public class Move {
    // 16-bit move representation
    // 0-5  (6 bits): From square (0-63)
    // 6-11 (6 bits): To square (0-63)
    // 12-15 (4 bits): Flags

    public static final int FLAG_NONE = 0;
    public static final int FLAG_DOUBLE_PAWN_PUSH = 1;
    public static final int FLAG_CASTLE_KING = 2;
    public static final int FLAG_CASTLE_QUEEN = 3;
    public static final int FLAG_CAPTURE = 4;
    public static final int FLAG_EN_PASSANT = 5;
    
    // Promotions (8-15) - Add 4 to standard flag if it's also a capture
    public static final int FLAG_PROMOTE_KNIGHT = 8;
    public static final int FLAG_PROMOTE_BISHOP = 9;
    public static final int FLAG_PROMOTE_ROOK = 10;
    public static final int FLAG_PROMOTE_QUEEN = 11;
    
    public static int makeMove(int from, int to, int flags) {
        return (from & 0x3F) | ((to & 0x3F) << 6) | ((flags & 0xF) << 12);
    }
    
    public static int getFrom(int move) {
        return move & 0x3F;
    }
    
    public static int getTo(int move) {
        return (move >> 6) & 0x3F;
    }
    
    public static int getFlags(int move) {
        return (move >> 12) & 0xF;
    }
    
    public static boolean isCapture(int move) {
        int flags = getFlags(move);
        return (flags == FLAG_CAPTURE) || (flags == FLAG_EN_PASSANT) || (flags >= FLAG_PROMOTE_KNIGHT + 4);
    }
    
    public static boolean isPromotion(int move) {
        return getFlags(move) >= FLAG_PROMOTE_KNIGHT;
    }
    
    public static String toString(int move) {
        int from = getFrom(move);
        int to = getTo(move);
        int flags = getFlags(move);
        
        String s = Square.toString(from) + Square.toString(to);
        
        if (flags >= FLAG_PROMOTE_KNIGHT) {
            int pType = flags & 3; // 0=N, 1=B, 2=R, 3=Q
            switch (pType) {
                case 0: s += "n"; break;
                case 1: s += "b"; break;
                case 2: s += "r"; break;
                case 3: s += "q"; break;
            }
        }
        return s;
    }
}
