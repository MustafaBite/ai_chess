package com.aichess.gui;

import com.aichess.engine.board.Board;
import com.aichess.engine.board.Move;
import com.aichess.engine.board.Piece;
import com.aichess.engine.board.Square;
import com.aichess.engine.movegen.MoveGenerator;
import com.aichess.engine.movegen.MoveList;
import com.aichess.engine.search.Search;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ChessGUI extends JFrame {

    private Board board;
    private BoardPanel boardPanel;
    private Search search;
    
    private int selectedSquare = Square.NONE;
    private List<Integer> validMovesForSelected = new ArrayList<>();
    
    private boolean isAITurn = false;
    
    // History
    private List<Board> boardHistory = new ArrayList<>();
    private int historyIndex = 0;
    private DefaultListModel<String> moveListModel;
    private JList<String> moveJList;
    private JButton btnBack;
    private JButton btnForward;

    public ChessGUI() {
        super("AI Chess Engine - Antigravity");
        
        board = new Board();
        board.loadFen(Board.START_FEN);
        search = new Search();

        // Save initial state
        boardHistory.add(board.cloneBoard());
        
        setLayout(new BorderLayout());

        boardPanel = new BoardPanel();
        boardPanel.setPreferredSize(new Dimension(600, 600));
        add(boardPanel, BorderLayout.CENTER);
        
        // Right Panel for History
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(200, 600));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Hamle Geçmişi"));
        
        moveListModel = new DefaultListModel<>();
        moveJList = new JList<>(moveListModel);
        JScrollPane scrollPane = new JScrollPane(moveJList);
        rightPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        btnBack = new JButton("< Geri");
        btnForward = new JButton("İleri >");
        
        btnBack.addActionListener(e -> navigateHistory(-1));
        btnForward.addActionListener(e -> navigateHistory(1));
        
        buttonPanel.add(btnBack);
        buttonPanel.add(btnForward);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(rightPanel, BorderLayout.EAST);
        
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        updateNavButtons();
    }
    
    private void navigateHistory(int delta) {
        int newIndex = historyIndex + delta;
        if (newIndex >= 0 && newIndex < boardHistory.size()) {
            historyIndex = newIndex;
            selectedSquare = Square.NONE;
            validMovesForSelected.clear();
            
            if (historyIndex > 0) {
                moveJList.setSelectedIndex(historyIndex - 1);
                moveJList.ensureIndexIsVisible(historyIndex - 1);
            } else {
                moveJList.clearSelection();
            }
            
            updateNavButtons();
            boardPanel.repaint();
        }
    }
    
    private void updateNavButtons() {
        btnBack.setEnabled(historyIndex > 0);
        btnForward.setEnabled(historyIndex < boardHistory.size() - 1);
    }
    
    private boolean isViewingHistory() {
        return historyIndex < boardHistory.size() - 1;
    }

    private class BoardPanel extends JPanel {
        private final Color lightColor = new Color(240, 217, 181);
        private final Color darkColor = new Color(181, 136, 99);
        private final Color highlightColor = new Color(130, 151, 105, 180);
        private final Color moveDotColor = new Color(0, 0, 0, 50);
        
        public BoardPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isAITurn || isViewingHistory()) return; // Disable play when viewing history
                    
                    int tileSize = getWidth() / 8;
                    int file = e.getX() / tileSize;
                    int rank = 7 - (e.getY() / tileSize); // Y is inverted (0 at top, 7 at bottom)
                    
                    if (file < 0 || file > 7 || rank < 0 || rank > 7) return;
                    int sq = rank * 8 + file;
                    
                    handleSquareClick(sq);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int tileSize = getWidth() / 8;
            
            // Draw board
            for (int rank = 0; rank < 8; rank++) {
                for (int file = 0; file < 8; file++) {
                    boolean isLight = (rank + file) % 2 != 0;
                    g2.setColor(isLight ? lightColor : darkColor);
                    g2.fillRect(file * tileSize, (7 - rank) * tileSize, tileSize, tileSize);
                    
                    // Draw coordinates
                    g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                    g2.setColor(isLight ? darkColor : lightColor);
                    
                    if (file == 0) { // Ranks (1-8) on the left edge
                        g2.drawString(String.valueOf(rank + 1), file * tileSize + 4, (7 - rank) * tileSize + 14);
                    }
                    if (rank == 0) { // Files (a-h) on the bottom edge
                        String fileChar = String.valueOf((char)('a' + file));
                        g2.drawString(fileChar, file * tileSize + tileSize - 12, (7 - rank) * tileSize + tileSize - 4);
                    }
                }
            }
            
            // Draw highlight for selected square
            if (selectedSquare != Square.NONE && !isViewingHistory()) {
                int file = Square.fileOf(selectedSquare);
                int rank = Square.rankOf(selectedSquare);
                g2.setColor(highlightColor);
                g2.fillRect(file * tileSize, (7 - rank) * tileSize, tileSize, tileSize);
            }
            
            // Draw valid moves
            if (!isViewingHistory()) {
                for (int move : validMovesForSelected) {
                    int to = Move.getTo(move);
                    int file = Square.fileOf(to);
                    int rank = Square.rankOf(to);
                    
                    int cx = file * tileSize + tileSize / 2;
                    int cy = (7 - rank) * tileSize + tileSize / 2;
                    
                    g2.setColor(moveDotColor);
                    g2.fillOval(cx - 10, cy - 10, 20, 20);
                }
            }
            
            // Draw pieces
            Board displayBoard = boardHistory.get(historyIndex);
            
            g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, tileSize - 10));
            FontMetrics fm = g2.getFontMetrics();
            
            for (int rank = 0; rank < 8; rank++) {
                for (int file = 0; file < 8; file++) {
                    int sq = rank * 8 + file;
                    int piece = displayBoard.pieceList[sq];
                    if (piece != Piece.EMPTY) {
                        String symbol = getPieceSymbol(piece);
                        int w = fm.stringWidth(symbol);
                        int h = fm.getAscent();
                        
                        g2.setColor(Piece.colorOf(piece) == Piece.WHITE ? Color.WHITE : Color.BLACK);
                        
                        g2.drawString(symbol, 
                                file * tileSize + (tileSize - w) / 2, 
                                (7 - rank) * tileSize + (tileSize + h) / 2 - fm.getDescent());
                    }
                }
            }
        }
    }

    private void handleSquareClick(int sq) {
        // If clicked on a valid move, make it
        for (int move : validMovesForSelected) {
            if (Move.getTo(move) == sq) {
                makeUserMove(move);
                return;
            }
        }
        
        // Otherwise, select the piece if it belongs to the player
        int piece = board.pieceList[sq];
        if (piece != Piece.EMPTY && Piece.colorOf(piece) == board.sideToMove) {
            selectedSquare = sq;
            generateValidMovesForSquare(sq);
        } else {
            selectedSquare = Square.NONE;
            validMovesForSelected.clear();
        }
        boardPanel.repaint();
    }

    private void generateValidMovesForSquare(int sq) {
        validMovesForSelected.clear();
        MoveList list = new MoveList();
        MoveGenerator.generateLegalMoves(board, list);
        
        for (int i = 0; i < list.size(); i++) {
            int move = list.get(i);
            if (Move.getFrom(move) == sq) {
                validMovesForSelected.add(move);
            }
        }
    }
    
    private void checkGameState() {
        MoveList list = new MoveList();
        MoveGenerator.generateLegalMoves(board, list);
        if (list.size() == 0) {
            int us = board.sideToMove;
            int king = Piece.makePiece(us, Piece.KING);
            long kingBB = board.pieceBitboards[king];
            if (kingBB != 0) {
                int kingSq = com.aichess.engine.board.BitboardUtils.lsb(kingBB);
                if (MoveGenerator.isSquareAttacked(board, kingSq, us ^ 1)) {
                    String winner = us == Piece.WHITE ? "Siyah" : "Beyaz";
                    JOptionPane.showMessageDialog(this, "Şah-Mat! " + winner + " Kazandı.");
                } else {
                    JOptionPane.showMessageDialog(this, "Pat! Berabere.");
                }
            }
        } else if (isThreefoldRepetition()) {
            JOptionPane.showMessageDialog(this, "Berabere! 3 Konum Tekrarı (Üçlü Tekrar).");
        } else if (board.halfMoveClock >= 100) {
            JOptionPane.showMessageDialog(this, "Berabere! 50 Hamle Kuralı.");
        }
    }

    private void makeUserMove(int move) {
        int from = Move.getFrom(move);
        int to = Move.getTo(move);
        int movingPiece = board.pieceList[from];
        
        String moveStr = Piece.toChar(movingPiece) + Square.toString(from) + "-" + Square.toString(to);
        if (Move.isCapture(move)) {
            moveStr = Piece.toChar(movingPiece) + Square.toString(from) + "x" + Square.toString(to);
        }
        if (Move.getFlags(move) == Move.FLAG_CASTLE_KING) moveStr = "O-O";
        if (Move.getFlags(move) == Move.FLAG_CASTLE_QUEEN) moveStr = "O-O-O";
        
        board.makeMove(move);
        
        String notation = (board.fullMoveNumber - (board.sideToMove == Piece.WHITE ? 1 : 0)) + ". " + moveStr;
        moveListModel.addElement(notation);
        
        boardHistory.add(board.cloneBoard());
        historyIndex = boardHistory.size() - 1;
        
        selectedSquare = Square.NONE;
        validMovesForSelected.clear();
        
        updateNavButtons();
        moveJList.setSelectedIndex(historyIndex - 1);
        moveJList.ensureIndexIsVisible(historyIndex - 1);
        
        boardPanel.repaint();
        checkGameState();
        
        if (!isGameOver()) {
            triggerAIMove();
        }
    }
    
    private boolean isGameOver() {
        MoveList list = new MoveList();
        MoveGenerator.generateLegalMoves(board, list);
        return list.size() == 0 || isThreefoldRepetition() || board.halfMoveClock >= 100;
    }

    private boolean isThreefoldRepetition() {
        if (boardHistory.isEmpty()) return false;
        
        Board currentBoard = boardHistory.get(boardHistory.size() - 1);
        String currentFenStr = getPositionFen(currentBoard);
        
        int count = 0;
        for (Board b : boardHistory) {
            if (getPositionFen(b).equals(currentFenStr)) {
                count++;
            }
        }
        
        return count >= 3;
    }
    
    private String getPositionFen(Board b) {
        String fen = b.generateFen();
        String[] parts = fen.split(" ");
        if (parts.length >= 4) {
            // Pieces, side to move, castling, en passant
            return parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
        }
        return fen;
    }

    private void triggerAIMove() {
        isAITurn = true;
        
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                search.search(board, 4);
                return search.bestMove;
            }

            @Override
            protected void done() {
                try {
                    int move = get();
                    if (move != 0) {
                        int from = Move.getFrom(move);
                        int to = Move.getTo(move);
                        int movingPiece = board.pieceList[from];
                        
                        String moveStr = Piece.toChar(movingPiece) + Square.toString(from) + "-" + Square.toString(to);
                        if (Move.isCapture(move)) {
                            moveStr = Piece.toChar(movingPiece) + Square.toString(from) + "x" + Square.toString(to);
                        }
                        if (Move.getFlags(move) == Move.FLAG_CASTLE_KING) moveStr = "O-O";
                        if (Move.getFlags(move) == Move.FLAG_CASTLE_QUEEN) moveStr = "O-O-O";
                        
                        board.makeMove(move);
                        
                        String notation = (board.fullMoveNumber - (board.sideToMove == Piece.WHITE ? 1 : 0)) + "... " + moveStr;
                        moveListModel.addElement(notation);
                        
                        boardHistory.add(board.cloneBoard());
                        historyIndex = boardHistory.size() - 1;
                        
                        updateNavButtons();
                        moveJList.setSelectedIndex(historyIndex - 1);
                        moveJList.ensureIndexIsVisible(historyIndex - 1);
                        
                        boardPanel.repaint();
                        checkGameState();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isAITurn = false;
                }
            }
        };
        worker.execute();
    }

    private String getPieceSymbol(int piece) {
        switch (piece) {
            case Piece.W_KING: return "♔";
            case Piece.W_QUEEN: return "♕";
            case Piece.W_ROOK: return "♖";
            case Piece.W_BISHOP: return "♗";
            case Piece.W_KNIGHT: return "♘";
            case Piece.W_PAWN: return "♙";
            
            case Piece.B_KING: return "♚";
            case Piece.B_QUEEN: return "♛";
            case Piece.B_ROOK: return "♜";
            case Piece.B_BISHOP: return "♝";
            case Piece.B_KNIGHT: return "♞";
            case Piece.B_PAWN: return "♟";
            default: return "";
        }
    }
}
