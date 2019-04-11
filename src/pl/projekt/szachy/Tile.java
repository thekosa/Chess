package pl.projekt.szachy;

import java.awt.*;

public class Tile {
    private Color tileColor;

    Tile(boolean tileColor) {
        if (tileColor) {
            this.tileColor = Color.white;
        } else {
            this.tileColor = Color.black;
        }
    }

    Tile(Color tileColor) {
        this.tileColor = tileColor;
    }

    public Color getTileColor() {
        return tileColor;
    }

    /*
        public int[] getRGBColour() {
            int[] rgb = new int[3];
            int a;
            if (this.tileColour) a = 255;
            else a = 0;
            for (int i : rgb) i = a;
            return rgb;

        }
    */
    /*
    public String toString() {
        if (this.tileColour) return "white";
        else return "black";
    }
    */
}
