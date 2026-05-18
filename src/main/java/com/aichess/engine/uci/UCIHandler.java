package com.aichess.engine.uci;

import com.aichess.engine.board.Board;
import com.aichess.engine.board.Move;
import com.aichess.engine.movegen.MoveGenerator;
import com.aichess.engine.movegen.MoveList;
import com.aichess.engine.search.Search;

import java.util.Scanner;

public class UCIHandler {

    private Board board;
    private Search search;

    public UCIHandler() {
        board = new Board();
        board.loadFen(Board.START_FEN);
        search = new Search();
    }

    public void loop() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("AI Chess Engine by Antigravity v1.0");

        while (true) {
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            String command = tokens[0].toLowerCase();

            switch (command) {
                case "uci":
                    System.out.println("id name AI Chess Engine");
                    System.out.println("id author Antigravity");
                    System.out.println("uciok");
                    break;
                case "isready":
                    System.out.println("readyok");
                    break;
                case "position":
                    parsePosition(tokens);
                    break;
                case "go":
                    parseGo(tokens);
                    break;
                case "quit":
                    return;
                case "d":
                case "display":
                    board.print();
                    break;
                default:
                    System.out.println("Unknown command: " + command);
            }
        }
    }

    private void parsePosition(String[] tokens) {
        if (tokens.length < 2) return;

        int movesIndex = -1;

        if (tokens[1].equals("startpos")) {
            board.loadFen(Board.START_FEN);
            movesIndex = 2;
        } else if (tokens[1].equals("fen")) {
            StringBuilder fen = new StringBuilder();
            movesIndex = 2;
            while (movesIndex < tokens.length && !tokens[movesIndex].equals("moves")) {
                fen.append(tokens[movesIndex]).append(" ");
                movesIndex++;
            }
            board.loadFen(fen.toString().trim());
        }

        if (movesIndex < tokens.length && tokens[movesIndex].equals("moves")) {
            for (int i = movesIndex + 1; i < tokens.length; i++) {
                int move = parseMove(tokens[i]);
                if (move != 0) {
                    board.makeMove(move);
                }
            }
        }
    }

    private int parseMove(String moveStr) {
        MoveList list = new MoveList();
        MoveGenerator.generateLegalMoves(board, list);
        for (int i = 0; i < list.size(); i++) {
            int move = list.get(i);
            if (Move.toString(move).equals(moveStr)) {
                return move;
            }
        }
        return 0; // Invalid
    }

    private void parseGo(String[] tokens) {
        int depth = 5; // default depth
        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].equals("depth") && i + 1 < tokens.length) {
                depth = Integer.parseInt(tokens[i + 1]);
            }
        }
        
        search.search(board, depth);
        
        if (search.bestMove != 0) {
            System.out.println("bestmove " + Move.toString(search.bestMove));
        } else {
            System.out.println("bestmove 0000"); // Resign / Checkmate
        }
    }
}
