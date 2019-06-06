package pl.projekt.szachy;

import java.awt.*;
import java.util.ArrayList;


public class Pawn extends Figure {
    Pawn(Color color) {
        super(color, "pawn");
    }
/*
    //zrobic funkcje figureDetect, dzieki niej bede sprawdzal czy na mozliwych polach do ruchu znajduje sie jakakolwiek figura uniemozliwiajaca ruch
    //druga funkcja oppostieFigureDetect zwracajaca booleana, odpowiadajacego na pytanie, czy jest tam figura obca. Wykorzystam do funkcji mozliwych bic
    public ArrayList<int[]> possibleMoves() {
//jezeli isBeatingPossible to dodajemy mozliwe ruchy z biciem
        //jezeli movesQuantity 0 to do mozliwych rochow dodajemy dwa pola do przodu
        //po wszystkim jezeli na jakim kolwiek polu mozliwych ruchow znajduje sie inna figura to nie mozna ruszyc(oprocz bicia)
        ArrayList<int[]> possibleMovesCordinates = new ArrayList<>();
        int possiblyNewCordinateX=xCordinate;
        int possiblyNewCordinateY=yCordinate+1;

        possibleMovesCordinates.add(cordinatesMerge(possiblyNewCordinateX,possiblyNewCordinateY));
        if (movesQuantity == 0) {
            possiblyNewCordinateX=xCordinate;
            possiblyNewCordinateY=yCordinate+2;
            possibleMovesCordinates.add(cordinatesMerge(possiblyNewCordinateX,possiblyNewCordinateY));
        }
        if (!oppositeFigureDetection().isEmpty()){
possibleMovesCordinates.add(oppositeFigureDetection());
       possibleMovesCordinates.//dolaczenie listy z oppositeFigureDetection do listy possibleMovesCordinates
        }
//na samym koncu skonfrontownaie calej listy z mapą figur, jezeli znajduja sie figury na drodze to wywalic z listy

    }

    private boolean isBeatingPossible() {
        //zrobic tutaj ze wyszukuje przeciwnego koloru pionka po przekątnej
        // i to dodaje kolejne mozliwosci ruchow.

    }

    private ArrayList<int[]> oppositeFigureDetection() {
        //pamietac o biciu w przelocie

    }
    */
}
