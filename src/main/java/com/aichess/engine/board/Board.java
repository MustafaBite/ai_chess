package com.aichess.engine.board;

import java.util.Arrays;

public class Board {
    // Bitboards for each piece type and color
    // Index using Piece.W_PAWN, Piece.B_KING, etc.
    // To make indexing easier, we use size 15 (0 to 14)
    public long[] pieceBitboards = new long[15];
    
    // Bitboards for colors (WHITE = 0, BLACK = 1)
    public long[] colorBitboards = new long[2];
    
    // 1D array representing the board (for fast piece lookups)
    public int[] pieceList = new int[64];
    
    public int sideToMove;
    public int enPassantSquare = Square.NONE;
    
    // Castling rights: 1 = WK, 2 = WQ, 4 = BK, 8 = BQ
    public int castlingRights;
    
    public int halfMoveClock;
    public int fullMoveNumber;
    
    public static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public Board() {
        reset();
    }

    public Board cloneBoard() {
        Board b = new Board();
        System.arraycopy(this.pieceBitboards, 0, b.pieceBitboards, 0, 15);
        System.arraycopy(this.colorBitboards, 0, b.colorBitboards, 0, 2);
        System.arraycopy(this.pieceList, 0, b.pieceList, 0, 64);
        b.sideToMove = this.sideToMove;
        b.enPassantSquare = this.enPassantSquare;
        b.castlingRights = this.castlingRights;
        b.halfMoveClock = this.halfMoveClock;
        b.fullMoveNumber = this.fullMoveNumber;
        return b;
    }

    public void reset() {
        Arrays.fill(pieceBitboards, 0L);
        Arrays.fill(colorBitboards, 0L);
        Arrays.fill(pieceList, Piece.EMPTY);
        sideToMove = Piece.WHITE;
        enPassantSquare = Square.NONE;
        castlingRights = 0;
        halfMoveClock = 0;
        fullMoveNumber = 1;
    }

    public void loadFen(String fen) {
        reset();
        String[] parts = fen.split(" ");
        
        // 1. Piece placement
        int rank = 7;
        int file = 0;
        for (int i = 0; i < parts[0].length(); i++) {
            char c = parts[0].charAt(i);
            if (c == '/') {
                rank--;
                file = 0;
            } else if (Character.isDigit(c)) {
                file += Character.getNumericValue(c);
            } else {
                int piece = Piece.fromChar(c);
                int sq = rank * 8 + file;
                addPiece(piece, sq);
                file++;
            }
        }
        
        // 2. Side to move
        sideToMove = parts[1].equals("w") ? Piece.WHITE : Piece.BLACK;
        
        // 3. Castling rights
        if (!parts[2].equals("-")) {
            if (parts[2].contains("K")) castlingRights |= 1;
            if (parts[2].contains("Q")) castlingRights |= 2;
            if (parts[2].contains("k")) castlingRights |= 4;
            if (parts[2].contains("q")) castlingRights |= 8;
        }
        
        // 4. En passant
        enPassantSquare = Square.fromString(parts[3]);
        
        // 5. Halfmove clock
        if (parts.length > 4) {
            halfMoveClock = Integer.parseInt(parts[4]);
        }
        
        // 6. Fullmove number
        if (parts.length > 5) {
            fullMoveNumber = Integer.parseInt(parts[5]);
        }
    }

    public void addPiece(int piece, int sq) {
        pieceBitboards[piece] = BitboardUtils.setBit(pieceBitboards[piece], sq);
        colorBitboards[Piece.colorOf(piece)] = BitboardUtils.setBit(colorBitboards[Piece.colorOf(piece)], sq);
        pieceList[sq] = piece;
    }

    public void removePiece(int sq) {
        int piece = pieceList[sq];
        if (piece != Piece.EMPTY) {
            pieceBitboards[piece] = BitboardUtils.clearBit(pieceBitboards[piece], sq);
            colorBitboards[Piece.colorOf(piece)] = BitboardUtils.clearBit(colorBitboards[Piece.colorOf(piece)], sq);
            pieceList[sq] = Piece.EMPTY;
        }
    }
    
    public void print() {
        System.out.println("  +-----------------+");
        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + " | ");
            for (int file = 0; file < 8; file++) {
                int sq = rank * 8 + file;
                int piece = pieceList[sq];
                System.out.print(Piece.toChar(piece) + " ");
            }
            System.out.println("|");
        }
        System.out.println("  +-----------------+");
        System.out.println("    a b c d e f g h");
        System.out.println("Side to move: " + (sideToMove == Piece.WHITE ? "White" : "Black"));
        System.out.println("FEN: " + generateFen());
    }
    
    public String generateFen() {
        StringBuilder sb = new StringBuilder();
        // Pieces
        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                int piece = pieceList[rank * 8 + file];
                if (piece == Piece.EMPTY) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount);
                        emptyCount = 0;
                    }
                    sb.append(Piece.toChar(piece));
                }
            }
            if (emptyCount > 0) sb.append(emptyCount);
            if (rank > 0) sb.append("/");
        }
        
        sb.append(" ").append(sideToMove == Piece.WHITE ? "w" : "b").append(" ");
        
        // Castling
        StringBuilder castling = new StringBuilder();
        if ((castlingRights & 1) != 0) castling.append("K");
        if ((castlingRights & 2) != 0) castling.append("Q");
        if ((castlingRights & 4) != 0) castling.append("k");
        if ((castlingRights & 8) != 0) castling.append("q");
        sb.append(castling.length() == 0 ? "-" : castling.toString()).append(" ");
        
        // En Passant
        sb.append(Square.toString(enPassantSquare)).append(" ");
        
        sb.append(halfMoveClock).append(" ").append(fullMoveNumber);
        
        return sb.toString();
    }

    public void makeMove(int move) {
        int from = Move.getFrom(move);
        int to = Move.getTo(move);
        int flags = Move.getFlags(move);

        int movingPiece = pieceList[from];
        int capturedPiece = pieceList[to];

        // Remove captured piece if any
        if (capturedPiece != Piece.EMPTY) {
            removePiece(to);
        } else if (flags == Move.FLAG_EN_PASSANT) {
            int epPawnSq = sideToMove == Piece.WHITE ? to - 8 : to + 8;
            removePiece(epPawnSq);
        }

        // Move the piece
        removePiece(from);
        addPiece(movingPiece, to);

        // Promotions
        if (Move.isPromotion(move)) {
            removePiece(to);
            int promType;
            if (flags >= Move.FLAG_PROMOTE_KNIGHT + 4) { // Capture promotion
                promType = flags - 4 - Move.FLAG_PROMOTE_KNIGHT + Piece.KNIGHT;
            } else {
                promType = flags - Move.FLAG_PROMOTE_KNIGHT + Piece.KNIGHT;
            }
            addPiece(Piece.makePiece(sideToMove, promType), to);
        }

        // Castling
        if (flags == Move.FLAG_CASTLE_KING) {
            int rookFrom = sideToMove == Piece.WHITE ? Square.H1 : Square.H8;
            int rookTo = sideToMove == Piece.WHITE ? Square.F1 : Square.F8;
            int rook = pieceList[rookFrom];
            removePiece(rookFrom);
            addPiece(rook, rookTo);
        } else if (flags == Move.FLAG_CASTLE_QUEEN) {
            int rookFrom = sideToMove == Piece.WHITE ? Square.A1 : Square.A8;
            int rookTo = sideToMove == Piece.WHITE ? Square.D1 : Square.D8;
            int rook = pieceList[rookFrom];
            removePiece(rookFrom);
            addPiece(rook, rookTo);
        }

        // Update state
        sideToMove ^= 1;
        enPassantSquare = Square.NONE;
        if (flags == Move.FLAG_DOUBLE_PAWN_PUSH) {
            enPassantSquare = sideToMove == Piece.BLACK ? to - 8 : to + 8; // sideToMove has already been flipped
        }
        
        // Castling rights update
        if (Piece.typeOf(movingPiece) == Piece.KING) {
            if (sideToMove == Piece.BLACK) castlingRights &= ~3; // White moved
            else castlingRights &= ~12; // Black moved
        } else if (Piece.typeOf(movingPiece) == Piece.ROOK) {
            if (sideToMove == Piece.BLACK) { // White moved
                if (from == Square.H1) castlingRights &= ~1;
                else if (from == Square.A1) castlingRights &= ~2;
            } else { // Black moved
                if (from == Square.H8) castlingRights &= ~4;
                else if (from == Square.A8) castlingRights &= ~8;
            }
        }
        
        if (Piece.typeOf(capturedPiece) == Piece.ROOK) {
            if (to == Square.H1) castlingRights &= ~1;
            else if (to == Square.A1) castlingRights &= ~2;
            else if (to == Square.H8) castlingRights &= ~4;
            else if (to == Square.A8) castlingRights &= ~8;
        }
    }
}
