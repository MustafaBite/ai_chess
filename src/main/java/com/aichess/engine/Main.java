package com.aichess.engine;

import com.aichess.engine.uci.UCIHandler;
import com.aichess.gui.ChessGUI;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("uci")) {
            // Start UCI mode
            UCIHandler uci = new UCIHandler();
            uci.loop();
        } else {
            // Start GUI mode
            SwingUtilities.invokeLater(() -> {
                ChessGUI gui = new ChessGUI();
                gui.setVisible(true);
            });
        }
    }
}
