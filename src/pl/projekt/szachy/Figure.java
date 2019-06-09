package pl.projekt.szachy;

import javafx.scene.paint.Color;

public abstract class Figure {
    protected Color color;
    private int movesQuantity = 0;
    private String name;

    Figure(Color color, String name) {
        this.color = color;
        this.name = name;
    }

    public String getColorToString() {
        if (color == Color.BLACK) {
            return "black";
        } else {
            return "white";
        }
    }

    public void incrementMovesQuantity() {
        movesQuantity++;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }
}
