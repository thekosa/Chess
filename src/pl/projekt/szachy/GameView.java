package pl.projekt.szachy;

import javax.swing.*;
import java.awt.*;

public class GameView extends JPanel {
    private final int pawnSize = 50;
    private final int bishopSize = 60;
    private final int kingSize = 70;
    private final int knightSize = 80;
    private final int queenSize = 90;
    private final int rookSize = 100;
    private Game game;


    GameView() {
        game = new Game();
    }


    public void paint(Graphics g) {
        paintFigures(g);
    }


    private void paintFigures(Graphics g) {
        for (int[] figureCords : game.getGameState()) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.red);
            g2.fillOval(figureCords[0]*100+10,figureCords[1]*100+10, 20, 20);
        }
    }
}
