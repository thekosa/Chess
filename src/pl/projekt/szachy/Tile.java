package pl.projekt.szachy;

import java.awt.*;

public class Tile {
    private boolean tileColour;

    Tile(boolean tileColour) {
        setTileColour(tileColour);
    }

    private void setTileColour(boolean tileColour) {
        this.tileColour = tileColour;
    }

    public boolean getBooleanColour() {
        return tileColour;
    }

    public Color getColour() {
        if (tileColour) return Color.white;
        else return Color.black;
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
    public String toString() {
        if (this.tileColour) return "white";
        else return "black";
    }
}
