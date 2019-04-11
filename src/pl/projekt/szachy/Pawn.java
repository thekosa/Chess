package pl.projekt.szachy;

import java.util.ArrayList;

public class Pawn extends Figure {
    Pawn(int xCordinate, int yCordinate) {
        super( xCordinate, yCordinate);
    }

    public ArrayList<> possibleMoves(){

    }

    private boolean isBeating(){
        getXCordinate() //zrobic tutaj ze wyszukuje przeciwnego koloru pionka po przekątnej  i to dodaje kolejne mozliwosci ruchow.
    }
}
