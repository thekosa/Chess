package pl.projekt.szachy;

import javafx.scene.paint.Color;

public class Round {
    //true - white, false - black
    private boolean side = true;

    public boolean getSide() {
        return side;
    }

    public void changeSide() {
        if (side) {
            side = false;
        } else {
            side = true;
        }
    }

    public Color getSideColor() {
        if (side) {
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }
}
