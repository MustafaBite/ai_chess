package com.aichess.engine.search;

import com.aichess.engine.board.Board;
import com.aichess.engine.board.Move;
import com.aichess.engine.board.Piece;
import com.aichess.engine.board.BitboardUtils;
import com.aichess.engine.movegen.MoveGenerator;
import com.aichess.engine.movegen.MoveList;

public class Search {

    private static final int INFINITY = 999999;
    public int nodesEvaluated = 0;
    public int bestMove = 0;

    public int search(Board board, int depth) {
        nodesEvaluated = 0;
        bestMove = 0;
        
        if (depth == 0) return Evaluator.evaluate(board);

        MoveList moves = new MoveList();
        MoveGenerator.generateLegalMoves(board, moves);
        sortMoves(moves);

        int bestScore = -INFINITY;
        int alpha = -INFINITY;
        int beta = INFINITY;

        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            Board nextBoard = board.cloneBoard();
            nextBoard.makeMove(move);

            int score = -alphaBeta(nextBoard, depth - 1, -beta, -alpha);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move; // Store best move at root
                if (score > alpha) {
                    alpha = score;
                }
            }
        }
        return bestScore;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta) {
        if (depth == 0) {
            return quiescenceSearch(board, alpha, beta);
        }

        MoveList moves = new MoveList();
        MoveGenerator.generateLegalMoves(board, moves);

        if (moves.size() == 0) {
            int us = board.sideToMove;
            int ourKing = Piece.makePiece(us, Piece.KING);
            int kingSq = BitboardUtils.lsb(board.pieceBitboards[ourKing]);
            boolean inCheck = MoveGenerator.isSquareAttacked(board, kingSq, us ^ 1);
            if (inCheck) {
                return -INFINITY + (100 - depth); // Checkmate, prefer faster mates
            } else {
                return 0; // Stalemate
            }
        }

        // Basic move ordering (Capture first)
        sortMoves(moves);

        int bestScore = -INFINITY;

        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            Board nextBoard = board.cloneBoard();
            nextBoard.makeMove(move);

            // Pseudo-legal check: if we left king in check, skip
            // (Skipping full check verification for basic speed, but normally required)

            int score = -alphaBeta(nextBoard, depth - 1, -beta, -alpha);

            if (score > bestScore) {
                bestScore = score;
                if (score > alpha) {
                    alpha = score;
                }
            }

            if (alpha >= beta) {
                break; // Alpha-beta cutoff
            }
        }

        return bestScore;
    }

    private int quiescenceSearch(Board board, int alpha, int beta) {
        nodesEvaluated++;
        int standPat = Evaluator.evaluate(board);

        if (standPat >= beta) {
            return beta;
        }
        if (alpha < standPat) {
            alpha = standPat;
        }

        MoveList moves = new MoveList();
        MoveGenerator.generateLegalMoves(board, moves);
        sortMoves(moves); // Captures should be first anyway

        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            
            // In Quiescence, only look at captures
            if (!Move.isCapture(move)) continue;

            Board nextBoard = board.cloneBoard();
            nextBoard.makeMove(move);

            int score = -quiescenceSearch(nextBoard, -beta, -alpha);

            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        return alpha;
    }

    private void sortMoves(MoveList moves) {
        // Very basic bubble sort: Captures first
        int[] arr = moves.getArray();
        int n = moves.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (!Move.isCapture(arr[j]) && Move.isCapture(arr[j + 1])) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }
}
