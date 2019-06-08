package pl.projekt.szachy;

import javafx.scene.paint.Color;

public class Round {
    //true - white, false - black
    private static boolean side;

    public static boolean getSide() {
        return side;
    }

    public static void changeSide() {
        if (side) {
            side = false;
        } else {
            side = true;
        }
    }

    public static Color getSideColor() {
        if (side) {
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }
}
