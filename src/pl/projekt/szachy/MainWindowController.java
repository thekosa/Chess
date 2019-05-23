package pl.projekt.szachy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {
    public Rectangle tile_0_0;
    public Rectangle tile_1_0;
    public Rectangle tile_2_0;
    public Rectangle tile_3_0;
    public Rectangle tile_4_0;
    public Rectangle tile_5_0;
    public Rectangle tile_6_0;
    public Rectangle tile_7_0;
    public Rectangle tile_0_1;
    public Rectangle tile_1_1;
    public Rectangle tile_2_1;
    public Rectangle tile_3_1;
    public Rectangle tile_4_1;
    public Rectangle tile_5_1;
    public Rectangle tile_6_1;
    public Rectangle tile_7_1;
    public Rectangle tile_0_2;
    public Rectangle tile_1_2;
    public Rectangle tile_2_2;
    public Rectangle tile_3_2;
    public Rectangle tile_4_2;
    public Rectangle tile_5_2;
    public Rectangle tile_6_2;
    public Rectangle tile_7_2;
    public Rectangle tile_0_3;
    public Rectangle tile_1_3;
    public Rectangle tile_2_3;
    public Rectangle tile_3_3;
    public Rectangle tile_4_3;
    public Rectangle tile_5_3;
    public Rectangle tile_6_3;
    public Rectangle tile_7_3;
    public Rectangle tile_0_4;
    public Rectangle tile_1_4;
    public Rectangle tile_2_4;
    public Rectangle tile_3_4;
    public Rectangle tile_4_4;
    public Rectangle tile_5_4;
    public Rectangle tile_6_4;
    public Rectangle tile_7_4;
    public Rectangle tile_0_5;
    public Rectangle tile_1_5;
    public Rectangle tile_2_5;
    public Rectangle tile_3_5;
    public Rectangle tile_4_5;
    public Rectangle tile_5_5;
    public Rectangle tile_6_5;
    public Rectangle tile_7_5;
    public Rectangle tile_0_6;
    public Rectangle tile_1_6;
    public Rectangle tile_2_6;
    public Rectangle tile_3_6;
    public Rectangle tile_4_6;
    public Rectangle tile_5_6;
    public Rectangle tile_6_6;
    public Rectangle tile_7_6;
    public Rectangle tile_0_7;
    public Rectangle tile_1_7;
    public Rectangle tile_2_7;
    public Rectangle tile_3_7;
    public Rectangle tile_4_7;
    public Rectangle tile_5_7;
    public Rectangle tile_6_7;
    public Rectangle tile_7_7;
    public TextField saveGameNameText;


    public Button przycisk;

    @FXML
    Button newGameButton;
    @FXML
    Button loadGameButton;
    @FXML
    Button saveGameButton;
    @FXML
    GridPane chessBoardGridPane;


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

    public void saveGameButtonAction() {
        System.out.println("Zapisz gre");
        String nameOfSavedGame = saveGameNameText.getText();
        //spisanie stanu gry do pliku
    }

    public void przyciskAkcja(ActionEvent actionEvent) {
        System.out.println("chujow sto");
    }
}
