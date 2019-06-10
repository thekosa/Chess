package pl.projekt.szachy;

import javafx.scene.paint.Color;

import java.io.Serializable;

public class Round implements Serializable {
    //true - white, false - black
    private boolean side = true;

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
