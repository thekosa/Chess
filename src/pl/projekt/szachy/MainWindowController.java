package pl.projekt.szachy;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {
    @FXML Button newGameButton;
    @FXML Button loadGameButton;
    @FXML Button saveGameButton;


//todo jezeli bedzie opcja new game - przekazujemy do gamestate zeby odczytywac z odpowiedniego pliku,
// jezeli wybierzemy co innego to z tego innego

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void newGameButtonAction() {
        System.out.println("nowa gra");
       // GameState gameState;
     //   gameState = new GameState(true);
       // for (Figure figure : gameState.getGameState()) {
            //wyciagamy kazda informacje po kolei:
            // kordynatory wpakowujemy tam gdzie bedzie miejsce okreslajace umiejscowienie obrazka,
            // kolor bedzie definiowal jaki kolor
            // nazwa - do nazwy bedziemy dodawac ".jpg" i za pomoca String figureJPGName bedziemy wywolywac obrazek
      //  }
    }

    public void loadGameButtonAction() {
        System.out.println("Wczytaj gre");
        //okno sie wyswietla z mozliwoscia wyboru poprzedniej gry
    }

    public void saveGameButtonAction(){
        System.out.println("Zapisz gre");
    }
}
