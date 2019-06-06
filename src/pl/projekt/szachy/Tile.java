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
}
