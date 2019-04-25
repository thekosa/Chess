package pl.projekt.szachy;

import javax.swing.*;
import java.awt.*;

public class BoardPanel extends JPanel {
    private final int tileSize = 100;
    private Board board;


    BoardPanel() {
        board = new Board();
        int windowSize = board.getN() * tileSize + 20;
        setPreferredSize(new Dimension(windowSize, windowSize));
    }

    private void paintBoardPanel(Graphics g) {
        int max = board.getN();

        for (int col = 0; col < max; col++) {
            for (int row = 0; row < max; row++) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(board.getTile(col, row).getTileColor());
                g2.fillRect(col * tileSize + 10, row * tileSize + 10, tileSize, tileSize);
            }
        }
    }

    public void paint(Graphics g) {
        paintBoardPanel(g);
    }
}
