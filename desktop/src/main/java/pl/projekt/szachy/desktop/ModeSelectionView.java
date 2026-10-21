package pl.projekt.szachy.desktop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import pl.projekt.szachy.computer.GameOptions;

/** Shared launcher for all frontends; each mode owns an independent game session. */
final class ModeSelectionView {
    private ModeSelectionView() { }

    static void show(Stage stage, Consumer<GameOptions> openDesktop, Function<GameOptions, String> openWeb,
                     boolean startConsole, boolean startWeb) {
        Label eyebrow = new Label("SZACHY / WYBIERZ SWÓJ STYL");
        eyebrow.getStyleClass().add("mode-eyebrow");
        Label title = new Label("Jak chcesz zagrać?");
        title.getStyleClass().add("mode-title");
        Label intro = new Label("Zagraj we dwoje lub zmierz się ze Stockfishem. Wybierz przeciwnika i interfejs.");
        intro.setWrapText(true);
        GameOptionsView options = new GameOptionsView(GameOptions.local());

        Button desktop = new Button("Graj w oknie");
        desktop.setId("desktop-mode");
        desktop.setDefaultButton(true);
        desktop.setOnAction(event -> openDesktop.accept(options.options()));
        VBox graphicalCard = card("01 / GRAFICZNY", "Klasyczna plansza",
                "Figury, podświetlane ruchy i płynne animacje. Sterowanie myszką.", desktop);

        Button console = new Button("Otwórz terminal");
        console.setId("console-mode");
        VBox consoleCard = card("02 / KONSOLA ASCII", "Szachy ze znaków",
                "Rysowane figury, kolorowe pola i animacje. Ruchy wpisujesz z klawiatury.", console);
        Button consoleOnline = new Button("Konsola online…");
        consoleOnline.setMaxWidth(Double.MAX_VALUE);
        consoleCard.getChildren().add(consoleOnline);

        Button web = new Button("Graj w przeglądarce");
        web.setId("web-mode");
        VBox webCard = card("03 / WEB", "W przeglądarce",
                "Ta sama gra na stronie. Animacje, historia i podświetlane ruchy. Sterowanie myszką.", web);

        HBox cards = new HBox(16, graphicalCard, consoleCard, webCard);
        Label feedback = new Label("Konsola otworzy terminal, a tryb Web — przeglądarkę.");
        feedback.setId("mode-feedback");
        feedback.setWrapText(true);
        feedback.setMinHeight(42);
        feedback.getStyleClass().add("mode-feedback");
        consoleOnline.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog("http://localhost:8080");
            dialog.initOwner(stage);
            dialog.setTitle("Konsola online");
            dialog.setHeaderText("Podaj adres działającego serwera szachów.");
            dialog.setContentText("Serwer:");
            dialog.showAndWait().ifPresent(address -> {
                consoleOnline.setDisable(true);
                Thread worker = new Thread(() -> {
                    try {
                        ConsoleLauncher.launchOnline(address.strip());
                        Platform.runLater(() -> feedback.setText("W terminalu wybierz pokój lub utwórz nowy."));
                    } catch (Exception exception) {
                        Platform.runLater(() -> feedback.setText("Nie udało się otworzyć konsoli online: " + exception.getMessage()));
                    } finally { Platform.runLater(() -> consoleOnline.setDisable(false)); }
                }, "launch-console-online");
                worker.setDaemon(true); worker.start();
            });
        });
        web.setOnAction(event -> {
            try {
                String address = openWeb.apply(options.options());
                feedback.setText("Gra: " + address + "  Pozostaw okno Szachy otwarte podczas gry w przeglądarce.");
            } catch (Exception exception) {
                feedback.setText("Nie udało się otworzyć gry webowej: " + exception.getMessage());
            }
        });
        console.setOnAction(event -> {
            GameOptions selectedOptions = options.options();
            console.setDisable(true);
            feedback.setText("Otwieranie terminala…");
            Thread worker = new Thread(() -> {
                try {
                    ConsoleLauncher.launch(selectedOptions);
                    Platform.runLater(() -> {
                        console.setDisable(false);
                        feedback.setText("Terminal został otwarty. Wpisz help, aby zobaczyć polecenia.");
                    });
                } catch (Exception exception) {
                    Platform.runLater(() -> {
                        console.setDisable(false);
                        feedback.setText("Nie udało się otworzyć terminala: " + exception.getMessage());
                    });
                }
            }, "launch-console");
            worker.setDaemon(true);
            worker.start();
        });

        VBox root = new VBox(14, eyebrow, title, intro, options, cards, feedback);
        root.setPadding(new Insets(34));
        root.getStyleClass().add("mode-window");
        Scene scene = new Scene(root, 940, 610);
        scene.getStylesheets().add(Objects.requireNonNull(
                ModeSelectionView.class.getResource("modes.css")).toExternalForm());
        stage.setTitle("Szachy — wybierz tryb");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
        if (startConsole) {
            console.fire();
        } else if (startWeb) {
            web.fire();
        }
    }

    private static VBox card(String caption, String title, String description, Button button) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("mode-caption");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("mode-card-title");
        Label text = new Label(description);
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(text, Priority.ALWAYS);
        button.setMaxWidth(Double.MAX_VALUE);
        VBox card = new VBox(14, captionLabel, titleLabel, text, button);
        card.getStyleClass().add("mode-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(280);
        card.setMinHeight(220);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }
}
