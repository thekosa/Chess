package pl.projekt.szachy;

public abstract class Figure {
    private int xCordinate;
    private int yCordinate;

    Figure(int xCordinate, int yCordinate) {
        this.xCordinate = xCordinate;
        this.yCordinate = yCordinate;
    }

    public int getXCordinate() {
        return xCordinate;
    }

    public int getYCordinate() {
        return yCordinate;
    }

    public void move(int xDestCordinate, int yDestCordinate){
        this.xCordinate=xDestCordinate;
        this.yCordinate=yDestCordinate;
    }
}
