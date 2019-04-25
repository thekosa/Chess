package pl.projekt.szachy;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Game {
    private Board board;
    private FiguresCollection[] figuresCollections;
    // public Map<Integer[], Color> gameState = new TreeMap<>();
    private ArrayList<int[]> gameState;
    private final Color firstColor = Color.white;
    private final Color secondColor = Color.black;

    Game() {
        figuresCollections = new FiguresCollection[2];
        figuresCollections[0] = new FiguresCollection(firstColor);
        figuresCollections[1] = new FiguresCollection(secondColor);
        setFirstGameState();
    }

    public ArrayList<int[]> getGameState(){
        return gameState;
    }

    public Color getColor(int numberOfColor) {
        if (numberOfColor == 0) {
            return firstColor;
        } else if (numberOfColor == 1) {
            return secondColor;
        } else {
            return Color.RED;//wyjatek?
        }
    }

    private ArrayList<int[]> formatCordinates(ArrayList<int[]> cordinates) {
        for (int[] cords : cordinates) {
            cords[0] = board.getN() - cords[0];
            cords[1] = board.getN() - cords[1];
        }
        return cordinates;
    }

    private void setFirstGameState() {
        gameState = new ArrayList<>(figuresCollections[0].getCordinates());
        gameState.addAll(formatCordinates(figuresCollections[1].getCordinates()));

        // Integer[] cordsInteger=new Integer[2];
        //  for (int[] cords : figuresCollections[0].getCordinates()) {
        //Integer[] cordsInteger=new Integer(convertIntToInteger(cords));

        //gameState.put(cords, Color.white);
        //   }
    }

    // private Integer[] convertIntToInteger(int[] numbs) {
    //    Integer[] object = new Integer[numbs.length];
    //  for (int i = 0; i < numbs.length; i++) {
    //    object[i] = Integer.valueOf(numbs[i]);
    //}
    //return object;
    // }
}
