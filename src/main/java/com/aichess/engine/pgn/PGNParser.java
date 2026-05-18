package com.aichess.engine.pgn;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PGNParser {
    
    // Very basic PGN parser to extract move tokens like "e4", "Nf3", "O-O"
    public static List<String> extractMoves(String pgnContent) {
        List<String> moves = new ArrayList<>();
        
        // Remove metadata like [Event "FIDE World Cup 2017"]
        pgnContent = pgnContent.replaceAll("\\[.*?\\]", "");
        
        // Remove comments like {This was a bad move}
        pgnContent = pgnContent.replaceAll("\\{.*?\\}", "");
        
        // Remove move numbers like "1.", "1...", "2."
        pgnContent = pgnContent.replaceAll("\\b\\d+\\.\\.\\.|\\b\\d+\\.", " ");
        
        // Extract words (moves)
        String[] tokens = pgnContent.split("\\s+");
        for (String token : tokens) {
            if (!token.isEmpty() && !token.equals("*") && !token.equals("1-0") && !token.equals("0-1") && !token.equals("1/2-1/2")) {
                moves.add(token);
            }
        }
        return moves;
    }
}
