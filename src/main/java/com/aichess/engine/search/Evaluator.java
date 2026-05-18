package com.aichess.engine.search;

import com.aichess.engine.board.Board;
import com.aichess.engine.board.Piece;
import com.aichess.engine.board.BitboardUtils;
import com.aichess.engine.board.Square;

public class Evaluator {

    private static final int PAWN_VAL = 100;
    private static final int KNIGHT_VAL = 320;
    private static final int BISHOP_VAL = 330;
    private static final int ROOK_VAL = 500;
    private static final int QUEEN_VAL = 900;

    // Center-focused PST for knights
    private static final int[] KNIGHT_PST = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    public static int evaluate(Board board) {
        int score = 0;

        score += evaluatePieces(board, Piece.WHITE) - evaluatePieces(board, Piece.BLACK);

        return board.sideToMove == Piece.WHITE ? score : -score;
    }

    private static int evaluatePieces(Board board, int color) {
        int score = 0;
        
        long pawns = board.pieceBitboards[Piece.makePiece(color, Piece.PAWN)];
        score += BitboardUtils.popCount(pawns) * PAWN_VAL;
        
        long knights = board.pieceBitboards[Piece.makePiece(color, Piece.KNIGHT)];
        score += BitboardUtils.popCount(knights) * KNIGHT_VAL;
        
        // Add PST for knights
        long kPieces = knights;
        while (kPieces != 0) {
            int sq = BitboardUtils.lsb(kPieces);
            int pstIndex = color == Piece.WHITE ? (63 - sq) : sq; // flip for white so they move UP
            score += KNIGHT_PST[pstIndex];
            kPieces &= kPieces - 1;
        }

        long bishops = board.pieceBitboards[Piece.makePiece(color, Piece.BISHOP)];
        score += BitboardUtils.popCount(bishops) * BISHOP_VAL;

        long rooks = board.pieceBitboards[Piece.makePiece(color, Piece.ROOK)];
        score += BitboardUtils.popCount(rooks) * ROOK_VAL;

        long queens = board.pieceBitboards[Piece.makePiece(color, Piece.QUEEN)];
        score += BitboardUtils.popCount(queens) * QUEEN_VAL;

        return score;
    }
}
