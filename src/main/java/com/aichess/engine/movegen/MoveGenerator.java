package com.aichess.engine.movegen;

import com.aichess.engine.board.Board;
import com.aichess.engine.board.BitboardUtils;
import com.aichess.engine.board.Piece;
import com.aichess.engine.board.Move;
import com.aichess.engine.board.Square;

public class MoveGenerator {

    private static final int[] DIR_ROOK = {-8, -1, 1, 8};
    private static final int[] DIR_BISHOP = {-9, -7, 7, 9};
    private static final int[] DIR_QUEEN = {-9, -8, -7, -1, 1, 7, 8, 9};

    public static void generateLegalMoves(Board board, MoveList list) {
        MoveList pseudoLegal = new MoveList();
        generatePseudoLegalMoves(board, pseudoLegal);
        
        int us = board.sideToMove;
        int them = us ^ 1;
        
        for (int i = 0; i < pseudoLegal.size(); i++) {
            int move = pseudoLegal.get(i);
            Board clone = board.cloneBoard();
            clone.makeMove(move);
            
            int ourKing = Piece.makePiece(us, Piece.KING);
            long kingBB = clone.pieceBitboards[ourKing];
            if (kingBB == 0) continue;
            
            int kingSq = BitboardUtils.lsb(kingBB);
            if (!isSquareAttacked(clone, kingSq, them)) {
                list.add(move);
            }
        }
    }

    public static boolean isSquareAttacked(Board board, int sq, int byColor) {
        int otherColor = byColor ^ 1;
        int pawn = Piece.makePiece(byColor, Piece.PAWN);
        if ((PrecomputedAttacks.PAWN_ATTACKS[otherColor][sq] & board.pieceBitboards[pawn]) != 0) return true;

        int knight = Piece.makePiece(byColor, Piece.KNIGHT);
        if ((PrecomputedAttacks.KNIGHT_ATTACKS[sq] & board.pieceBitboards[knight]) != 0) return true;

        int king = Piece.makePiece(byColor, Piece.KING);
        if ((PrecomputedAttacks.KING_ATTACKS[sq] & board.pieceBitboards[king]) != 0) return true;

        long allPieces = board.colorBitboards[Piece.WHITE] | board.colorBitboards[Piece.BLACK];
        int bishop = Piece.makePiece(byColor, Piece.BISHOP);
        int rook = Piece.makePiece(byColor, Piece.ROOK);
        int queen = Piece.makePiece(byColor, Piece.QUEEN);

        if (isAttackedBySlider(sq, DIR_BISHOP, board.pieceBitboards[bishop] | board.pieceBitboards[queen], allPieces)) return true;
        if (isAttackedBySlider(sq, DIR_ROOK, board.pieceBitboards[rook] | board.pieceBitboards[queen], allPieces)) return true;

        return false;
    }

    private static boolean isAttackedBySlider(int sq, int[] dirs, long attackers, long allPieces) {
        for (int dir : dirs) {
            int to = sq;
            while (true) {
                if (isOffBoard(to, dir)) break;
                to += dir;
                if (BitboardUtils.getBit(attackers, to)) return true;
                if (BitboardUtils.getBit(allPieces, to)) break;
            }
        }
        return false;
    }

    public static void generatePseudoLegalMoves(Board board, MoveList list) {
        int us = board.sideToMove;
        int them = us ^ 1;
        long usPieces = board.colorBitboards[us];
        long themPieces = board.colorBitboards[them];
        long allPieces = usPieces | themPieces;
        long emptySquares = ~allPieces;

        int pawn = Piece.makePiece(us, Piece.PAWN);
        int knight = Piece.makePiece(us, Piece.KNIGHT);
        int bishop = Piece.makePiece(us, Piece.BISHOP);
        int rook = Piece.makePiece(us, Piece.ROOK);
        int queen = Piece.makePiece(us, Piece.QUEEN);
        int king = Piece.makePiece(us, Piece.KING);

        // 1. Pawns
        long pawns = board.pieceBitboards[pawn];
        long singlePushs = us == Piece.WHITE ? (pawns << 8) & emptySquares : (pawns >>> 8) & emptySquares;
        long doublePushs = 0;
        
        if (us == Piece.WHITE) {
            doublePushs = (singlePushs << 8) & emptySquares & PrecomputedAttacks.RANK_4;
        } else {
            doublePushs = (singlePushs >>> 8) & emptySquares & PrecomputedAttacks.RANK_5;
        }

        // Process Single Pushes
        long sp = singlePushs;
        while (sp != 0) {
            int to = BitboardUtils.lsb(sp);
            int from = us == Piece.WHITE ? to - 8 : to + 8;
            addPawnMove(list, from, to, false);
            sp &= sp - 1;
        }

        // Process Double Pushes
        long dp = doublePushs;
        while (dp != 0) {
            int to = BitboardUtils.lsb(dp);
            int from = us == Piece.WHITE ? to - 16 : to + 16;
            list.add(Move.makeMove(from, to, Move.FLAG_DOUBLE_PAWN_PUSH));
            dp &= dp - 1;
        }

        // Pawn Captures
        long pawnsToProcess = pawns;
        while (pawnsToProcess != 0) {
            int from = BitboardUtils.lsb(pawnsToProcess);
            long attacks = PrecomputedAttacks.PAWN_ATTACKS[us][from] & themPieces;
            while (attacks != 0) {
                int to = BitboardUtils.lsb(attacks);
                addPawnMove(list, from, to, true);
                attacks &= attacks - 1;
            }
            
            // En Passant
            if (board.enPassantSquare != Square.NONE) {
                long epAttacks = PrecomputedAttacks.PAWN_ATTACKS[us][from] & BitboardUtils.setBit(0L, board.enPassantSquare);
                if (epAttacks != 0) {
                    list.add(Move.makeMove(from, board.enPassantSquare, Move.FLAG_EN_PASSANT));
                }
            }
            
            pawnsToProcess &= pawnsToProcess - 1;
        }

        // 2. Knights
        long knights = board.pieceBitboards[knight];
        while (knights != 0) {
            int from = BitboardUtils.lsb(knights);
            long attacks = PrecomputedAttacks.KNIGHT_ATTACKS[from] & ~usPieces;
            addStandardMoves(list, from, attacks, themPieces);
            knights &= knights - 1;
        }

        // 3. Kings
        long kings = board.pieceBitboards[king];
        if (kings != 0) {
            int from = BitboardUtils.lsb(kings);
            long attacks = PrecomputedAttacks.KING_ATTACKS[from] & ~usPieces;
            addStandardMoves(list, from, attacks, themPieces);
            
            // Castling (Checking for path clear and squares not under attack)
            boolean inCheck = isSquareAttacked(board, from, them);
            if (!inCheck) {
                if (us == Piece.WHITE) {
                    if ((board.castlingRights & 1) != 0 && board.pieceList[Square.F1] == Piece.EMPTY && board.pieceList[Square.G1] == Piece.EMPTY) {
                        if (!isSquareAttacked(board, Square.F1, them)) {
                            list.add(Move.makeMove(Square.E1, Square.G1, Move.FLAG_CASTLE_KING));
                        }
                    }
                    if ((board.castlingRights & 2) != 0 && board.pieceList[Square.D1] == Piece.EMPTY && board.pieceList[Square.C1] == Piece.EMPTY && board.pieceList[Square.B1] == Piece.EMPTY) {
                        if (!isSquareAttacked(board, Square.D1, them)) {
                            list.add(Move.makeMove(Square.E1, Square.C1, Move.FLAG_CASTLE_QUEEN));
                        }
                    }
                } else {
                    if ((board.castlingRights & 4) != 0 && board.pieceList[Square.F8] == Piece.EMPTY && board.pieceList[Square.G8] == Piece.EMPTY) {
                        if (!isSquareAttacked(board, Square.F8, them)) {
                            list.add(Move.makeMove(Square.E8, Square.G8, Move.FLAG_CASTLE_KING));
                        }
                    }
                    if ((board.castlingRights & 8) != 0 && board.pieceList[Square.D8] == Piece.EMPTY && board.pieceList[Square.C8] == Piece.EMPTY && board.pieceList[Square.B8] == Piece.EMPTY) {
                        if (!isSquareAttacked(board, Square.D8, them)) {
                            list.add(Move.makeMove(Square.E8, Square.C8, Move.FLAG_CASTLE_QUEEN));
                        }
                    }
                }
            }
        }

        // 4. Sliders (Bishop, Rook, Queen)
        generateSlidingMoves(list, board.pieceBitboards[bishop], DIR_BISHOP, allPieces, usPieces, themPieces);
        generateSlidingMoves(list, board.pieceBitboards[rook], DIR_ROOK, allPieces, usPieces, themPieces);
        generateSlidingMoves(list, board.pieceBitboards[queen], DIR_QUEEN, allPieces, usPieces, themPieces);
    }

    private static void generateSlidingMoves(MoveList list, long pieces, int[] dirs, long allPieces, long usPieces, long themPieces) {
        while (pieces != 0) {
            int from = BitboardUtils.lsb(pieces);
            for (int dir : dirs) {
                int to = from;
                while (true) {
                    // Check board bounds before moving
                    if (isOffBoard(to, dir)) break;
                    to += dir;
                    
                    if (BitboardUtils.getBit(usPieces, to)) break; // Hit friendly
                    
                    if (BitboardUtils.getBit(themPieces, to)) {
                        list.add(Move.makeMove(from, to, Move.FLAG_CAPTURE));
                        break; // Hit enemy, can capture but not continue
                    }
                    
                    list.add(Move.makeMove(from, to, Move.FLAG_NONE)); // Quiet move
                }
            }
            pieces &= pieces - 1;
        }
    }

    private static boolean isOffBoard(int sq, int dir) {
        int file = Square.fileOf(sq);
        int rank = Square.rankOf(sq);
        if (dir == 1 && file == 7) return true; // right
        if (dir == -1 && file == 0) return true; // left
        if (dir == 8 && rank == 7) return true; // up
        if (dir == -8 && rank == 0) return true; // down
        if (dir == 9 && (file == 7 || rank == 7)) return true; // up-right
        if (dir == 7 && (file == 0 || rank == 7)) return true; // up-left
        if (dir == -7 && (file == 7 || rank == 0)) return true; // down-right
        if (dir == -9 && (file == 0 || rank == 0)) return true; // down-left
        return false;
    }

    private static void addPawnMove(MoveList list, int from, int to, boolean isCapture) {
        int toRank = Square.rankOf(to);
        if (toRank == 0 || toRank == 7) {
            int baseFlag = isCapture ? Move.FLAG_PROMOTE_KNIGHT + 4 : Move.FLAG_PROMOTE_KNIGHT;
            list.add(Move.makeMove(from, to, baseFlag));     // N
            list.add(Move.makeMove(from, to, baseFlag + 1)); // B
            list.add(Move.makeMove(from, to, baseFlag + 2)); // R
            list.add(Move.makeMove(from, to, baseFlag + 3)); // Q
        } else {
            list.add(Move.makeMove(from, to, isCapture ? Move.FLAG_CAPTURE : Move.FLAG_NONE));
        }
    }

    private static void addStandardMoves(MoveList list, int from, long attacks, long themPieces) {
        while (attacks != 0) {
            int to = BitboardUtils.lsb(attacks);
            boolean isCapture = BitboardUtils.getBit(themPieces, to);
            list.add(Move.makeMove(from, to, isCapture ? Move.FLAG_CAPTURE : Move.FLAG_NONE));
            attacks &= attacks - 1;
        }
    }
}
