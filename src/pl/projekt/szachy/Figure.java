package pl.projekt.szachy;

import java.awt.*;

public abstract class Figure {
    protected int xCordinate;
    protected int yCordinate;   //kolejnym krokiem bedzie przerobienie kordynatow na tablice, bo trzeba je nonstop mergowac,
    //co jest cholernie upierdliwe
    protected Color color;
    protected int movesQuantity = 0;

    Figure(Color color, int xCordinate, int yCordinate) {
        this.xCordinate = xCordinate;
        this.yCordinate = yCordinate;
        this.color = color;
    }

    public int getXCordinate() {
        return xCordinate;
    }

    public int getYCordinate() {
        return yCordinate;
    }

    public void move(int xDestCordinate, int yDestCordinate) {
        this.xCordinate = xDestCordinate;
        this.yCordinate = yDestCordinate;
        movesQuantity++;
    }

    protected int[] cordinatesMerge(int xCordinate, int yCordinate) {
        int[] tempCordinates = new int[2];
        tempCordinates[0] = xCordinate;
        tempCordinates[1] = yCordinate;
        return tempCordinates;
    }
}
