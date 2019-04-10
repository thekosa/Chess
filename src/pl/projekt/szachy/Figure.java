package pl.projekt.szachy;

public abstract class Figure {
    private boolean figureColour;
    private int xCordinate;
    private int yCordinate;

    Figure(boolean figureColour, int xCordinate, int yCordinate) {
        this.figureColour = figureColour;
        this.xCordinate = xCordinate;
        this.yCordinate = yCordinate;
    }

    public boolean getFigureColour() {
        return figureColour;
    }

    public int getXCordinate() {
        return xCordinate;
    }

    public int getYCordinate() {
        return yCordinate;
    }
}
