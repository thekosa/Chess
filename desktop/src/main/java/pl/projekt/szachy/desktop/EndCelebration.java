package pl.projekt.szachy.desktop;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import pl.projekt.szachy.core.model.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/** A short, cancellable solitaire-style victory lap. It never changes the game state. */
final class EndCelebration extends StackPane implements AutoCloseable {
    private final Canvas canvas = new Canvas();
    private final List<Sprite> sprites = new ArrayList<>();
    private AnimationTimer timer;
    private final Label title = new Label();

    EndCelebration() {
        setId("end-celebration");
        setVisible(false);
        setPickOnBounds(false);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        canvas.setMouseTransparent(true);
        title.setStyle("-fx-font-size: 30; -fx-font-weight: bold; -fx-text-fill: #fff9e7;");
        Label subtitle = new Label("Dobra partia. Czas na rewanż?");
        subtitle.setStyle("-fx-text-fill: #e1e9d3; -fx-font-size: 14;");
        Button skip = new Button("Pomiń efekt");
        skip.setId("skip-celebration");
        skip.setOnAction(event -> close());
        VBox card = new VBox(12, title, subtitle, skip);
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(390, 170);
        card.setStyle("-fx-padding: 24; -fx-background-color: rgba(25,55,43,0.94); -fx-background-radius: 18;"
                + "-fx-border-color: #d3b363; -fx-border-radius: 18;");
        getChildren().setAll(canvas, card);
    }
    @Override protected double computePrefWidth(double height) { return 0; }
    @Override protected double computePrefHeight(double width) { return 0; }

    void play(GameState state, Function<Piece, Image> images) {
        close();
        title.setText(state.status() == GameStatus.CHECKMATE
                ? "Wygrywają " + (state.sideToMove() == Side.BLACK ? "białe!" : "czarne!") : "Remis!");
        Random random = new Random(64);
        var entries = state.board().entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString())).toList();
        for (int i = 0; !entries.isEmpty() && i < Math.max(12, entries.size()); i++) {
            var entry = entries.get(i % entries.size());
            sprites.add(new Sprite(images.apply(entry.getValue()), 28 + entry.getKey().file() * 80,
                    (7 - entry.getKey().rank()) * 80, (random.nextDouble() - .5) * 430,
                    -150 - random.nextDouble() * 280, i * .07));
        }
        setVisible(true);
        boolean reduced = Boolean.getBoolean("chess.reducedMotion");
        timer = new AnimationTimer() {
            private long start;
            private long previous;
            @Override public void handle(long now) {
                if (start == 0) { start = now; previous = now; }
                double elapsed = (now - start) / 1e9;
                double dt = Math.min(.035, (now - previous) / 1e9);
                previous = now;
                if (elapsed > 6.5) { close(); return; }
                if (reduced) return;
                GraphicsContext graphics = canvas.getGraphicsContext2D();
                graphics.setFill(Color.rgb(18, 37, 30, .10));
                graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                for (int i = 0; i < sprites.size(); i++) {
                    Sprite sprite = sprites.get(i);
                    if (elapsed < sprite.delay) continue;
                    sprite.vy += 650 * dt;
                    sprite.x += sprite.vx * dt;
                    sprite.y += sprite.vy * dt;
                    double floor = Math.max(1, getHeight() - 65);
                    if (sprite.y > floor) { sprite.y = floor; sprite.vy = -Math.max(230, Math.abs(sprite.vy) * .8); }
                    if (sprite.x < 0) { sprite.x = 0; sprite.vx = Math.abs(sprite.vx); }
                    if (sprite.x > getWidth() - 60) { sprite.x = Math.max(0, getWidth() - 60); sprite.vx = -Math.abs(sprite.vx); }
                    graphics.setStroke(Color.hsb((i * 39 + elapsed * 25) % 360, .75, 1, .7));
                    graphics.setLineWidth(3);
                    graphics.strokeLine(sprite.x + 30, sprite.y + 32,
                            sprite.x + 30 - sprite.vx * .07, sprite.y + 32 - sprite.vy * .07);
                    graphics.drawImage(sprite.image, sprite.x, sprite.y, 60, 60);
                }
            }
        };
        timer.start();
    }
    @Override public void close() {
        if (timer != null) { timer.stop(); timer = null; }
        sprites.clear();
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        setVisible(false);
    }
    private static final class Sprite {
        final Image image;
        final double delay;
        double x, y, vx, vy;
        Sprite(Image image, double x, double y, double vx, double vy, double delay) {
            this.image = image; this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.delay = delay;
        }
    }
}
