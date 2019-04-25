package pl.projekt.szachy;

import javax.swing.*;

public class Window extends JFrame {
    Window() {
        super("Szachy");
        BoardPanel boardPanel = new BoardPanel();
        add(boardPanel);

        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void startGame(){
        GameView gameView= new GameView();
        add(gameView);
    }
}