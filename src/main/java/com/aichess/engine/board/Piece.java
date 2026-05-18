package com.aichess.engine.board;

public class Piece {
    public static final int EMPTY = 0;
    
    // Piece Types (1 to 6)
    public static final int PAWN = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK = 4;
    public static final int QUEEN = 5;
    public static final int KING = 6;
    
    // Colors
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    public static final int COLOR_NONE = -1;

    // Combined Pieces (Color * 8 + PieceType)
    // White pieces: 1 to 6
    public static final int W_PAWN = WHITE * 8 + PAWN;
    public static final int W_KNIGHT = WHITE * 8 + KNIGHT;
    public static final int W_BISHOP = WHITE * 8 + BISHOP;
    public static final int W_ROOK = WHITE * 8 + ROOK;
    public static final int W_QUEEN = WHITE * 8 + QUEEN;
    public static final int W_KING = WHITE * 8 + KING;

    // Black pieces: 9 to 14
    public static final int B_PAWN = BLACK * 8 + PAWN;
    public static final int B_KNIGHT = BLACK * 8 + KNIGHT;
    public static final int B_BISHOP = BLACK * 8 + BISHOP;
    public static final int B_ROOK = BLACK * 8 + ROOK;
    public static final int B_QUEEN = BLACK * 8 + QUEEN;
    public static final int B_KING = BLACK * 8 + KING;

    public static int typeOf(int piece) {
        return piece & 7;
    }

    public static int colorOf(int piece) {
        if (piece == EMPTY) return COLOR_NONE;
        return piece >> 3;
    }

    public static int makePiece(int color, int type) {
        return (color << 3) | type;
    }
    
    public static char toChar(int piece) {
        switch (piece) {
            case W_PAWN: return 'P';
            case W_KNIGHT: return 'N';
            case W_BISHOP: return 'B';
            case W_ROOK: return 'R';
            case W_QUEEN: return 'Q';
            case W_KING: return 'K';
            case B_PAWN: return 'p';
            case B_KNIGHT: return 'n';
            case B_BISHOP: return 'b';
            case B_ROOK: return 'r';
            case B_QUEEN: return 'q';
            case B_KING: return 'k';
            default: return '.';
        }
    }

    public static int fromChar(char c) {
        switch (c) {
            case 'P': return W_PAWN;
            case 'N': return W_KNIGHT;
            case 'B': return W_BISHOP;
            case 'R': return W_ROOK;
            case 'Q': return W_QUEEN;
            case 'K': return W_KING;
            case 'p': return B_PAWN;
            case 'n': return B_KNIGHT;
            case 'b': return B_BISHOP;
            case 'r': return B_ROOK;
            case 'q': return B_QUEEN;
            case 'k': return B_KING;
            default: return EMPTY;
        }
    }
}
