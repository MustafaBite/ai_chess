package com.aichess.engine.pgn;

import com.aichess.engine.board.Board;
import com.aichess.engine.board.Move;
import com.aichess.engine.board.Piece;
import com.aichess.engine.board.Square;
import com.aichess.engine.movegen.MoveGenerator;
import com.aichess.engine.movegen.MoveList;
import com.aichess.engine.search.Evaluator;
import com.aichess.engine.search.Search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class GameAnalyzer {

    public static void analyzeFile(String filePath, int searchDepth) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        List<String> sanMoves = PGNParser.extractMoves(content);
        
        Board board = new Board();
        board.loadFen(Board.START_FEN);
        Search search = new Search();
        
        int prevEval = Evaluator.evaluate(board);
        
        System.out.println("Starting PGN Analysis. Depth: " + searchDepth);
        System.out.println("-------------------------------------------------");
        
        int moveNumber = 1;
        for (int i = 0; i < sanMoves.size(); i++) {
            String san = sanMoves.get(i);
            int colorToMove = board.sideToMove;
            String movePrefix = (colorToMove == Piece.WHITE) ? moveNumber + ". " : moveNumber + "... ";
            
            // Note: Since converting pure SAN (e.g. "Nf3") to our 16-bit move requires full disambiguation logic,
            // we will use the engine's Search to evaluate the current board state and see if it dropped significantly.
            // But we actually need to MAKE the move to advance the board. 
            // For a complete PGN parser, SAN -> Move conversion is necessary. 
            // Here, we provide a placeholder that attempts to match destination squares for simplicity.
            int move = tryParseBasicSAN(san, board);
            
            if (move == 0) {
                System.out.println("Could not parse move: " + san + ". Stopping analysis.");
                break;
            }
            
            // Search the position BEFORE the move
            search.search(board, searchDepth);
            int engineEvalBefore = Evaluator.evaluate(board); // static eval or search eval
            
            board.makeMove(move);
            
            // Search the position AFTER the move
            int engineEvalAfter = -Evaluator.evaluate(board); // From the perspective of the player who just moved
            
            // Blunder Detection Logic: If the evaluation drops by more than 200 centipawns (2.0 pawns)
            int evalDrop = prevEval - engineEvalAfter; 
            
            System.out.print(movePrefix + san + " \t-> Eval: " + engineEvalAfter);
            if (evalDrop > 200) {
                System.out.println(" \t?? BLUNDER! (Eval dropped by " + evalDrop + ")");
            } else if (evalDrop > 100) {
                System.out.println(" \t? INACCURACY (Eval dropped by " + evalDrop + ")");
            } else {
                System.out.println();
            }
            
            prevEval = engineEvalAfter;
            
            if (colorToMove == Piece.BLACK) {
                moveNumber++;
            }
        }
        System.out.println("-------------------------------------------------");
        System.out.println("Analysis Complete.");
    }
    
    private static int tryParseBasicSAN(String san, Board board) {
        san = san.replaceAll("[+#]", ""); // Remove check/mate symbols
        
        MoveList list = new MoveList();
        MoveGenerator.generatePseudoLegalMoves(board, list);
        
        // This is a VERY simplified SAN parser for demonstration.
        // It mostly looks at the destination square and piece type.
        for (int i = 0; i < list.size(); i++) {
            int move = list.get(i);
            int to = Move.getTo(move);
            int movingPiece = board.pieceList[Move.getFrom(move)];
            int pType = Piece.typeOf(movingPiece);
            String toStr = Square.toString(to);
            
            if (san.equals("O-O") && pType == Piece.KING && Move.getFlags(move) == Move.FLAG_CASTLE_KING) return move;
            if (san.equals("O-O-O") && pType == Piece.KING && Move.getFlags(move) == Move.FLAG_CASTLE_QUEEN) return move;
            
            if (san.endsWith(toStr)) {
                if (Character.isUpperCase(san.charAt(0))) {
                    char pChar = san.charAt(0);
                    if ((pChar == 'N' && pType == Piece.KNIGHT) ||
                        (pChar == 'B' && pType == Piece.BISHOP) ||
                        (pChar == 'R' && pType == Piece.ROOK) ||
                        (pChar == 'Q' && pType == Piece.QUEEN) ||
                        (pChar == 'K' && pType == Piece.KING)) {
                        return move; // Simplified: might fail on disambiguation (e.g. Nbd7)
                    }
                } else if (pType == Piece.PAWN) {
                    return move;
                }
            }
        }
        return 0;
    }
}
