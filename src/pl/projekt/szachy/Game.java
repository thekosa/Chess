package pl.projekt.szachy;

import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

public class Game {
    private Board board;
    private FiguresCollection[] figuresCollections;
    public Map<Integer[], Color> gameState = new TreeMap<>();

    Game() {
        figuresCollections = new FiguresCollection[2];
        figuresCollections[0] = new FiguresCollection(Color.white);
        figuresCollections[1] = new FiguresCollection(Color.black);
        setFirstGameState();
    }

    private void setFirstGameState() {
        Integer[] cordsInteger=new Integer[2];
        for (int[] cords : figuresCollections[0].getCordinates()) {
            Integer[] cordsInteger=new Integer(convertIntToInteger(cords));

            gameState.put(cords, Color.white);
        }


        figuresCollections[0].getCordinates()
    }

    private Integer[] convertIntToInteger(int[] numbs) {
        Integer[] object = new Integer[numbs.length];
        for (int i = 0; i < numbs.length; i++) {
            object[i] = Integer.valueOf(numbs[i]);
        }
        return object;
    }
}
