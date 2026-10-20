package pl.projekt.szachy.desktop;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Animates temporary sprites; backend positions remain authoritative. */
final class MoveAnimator {
    static final Duration MOVE_DURATION = Duration.millis(300);
    private final Pane layer;
    private final List<ImageView> hiddenOriginals = new ArrayList<>();
    private ParallelTransition running;

    record Motion(ImageView piece, Node destination) {
    }

    MoveAnimator(Pane layer) {
        this.layer = Objects.requireNonNull(layer);
    }

    boolean isAnimating() {
        return running != null;
    }

    ParallelTransition play(List<Motion> motions, List<ImageView> captures, Runnable finished) {
        cancel();
        ParallelTransition transition = new ParallelTransition();
        // Captured sprites sit below the approaching moving sprites.
        for (ImageView original : captures) {
            ImageView sprite = copy(original, "captured-");
            FadeTransition fade = new FadeTransition(Duration.millis(140), sprite);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setDelay(Duration.millis(160));
            ScaleTransition shrink = new ScaleTransition(Duration.millis(140), sprite);
            shrink.setToX(0.7);
            shrink.setToY(0.7);
            shrink.setDelay(Duration.millis(160));
            transition.getChildren().addAll(fade, shrink);
        }
        for (Motion motion : motions) {
            ImageView sprite = copy(motion.piece(), "moving-");
            Bounds target = layer.sceneToLocal(
                    motion.destination().localToScene(motion.destination().getBoundsInLocal()));
            TranslateTransition travel = new TranslateTransition(MOVE_DURATION, sprite);
            travel.setToX(target.getCenterX() - (sprite.getLayoutX() + sprite.getBoundsInLocal().getWidth() / 2));
            travel.setToY(target.getCenterY() - (sprite.getLayoutY() + sprite.getBoundsInLocal().getHeight() / 2));
            travel.setInterpolator(Interpolator.EASE_BOTH);
            transition.getChildren().add(travel);
        }
        running = transition;
        transition.setOnFinished(event -> {
            if (running != transition) {
                return;
            }
            running = null;
            clearSprites();
            finished.run();
        });
        transition.play();
        return transition;
    }

    void cancel() {
        if (running != null) {
            // Remove the callback before stopping so an old move cannot overwrite a new game.
            running.setOnFinished(null);
            running.stop();
            running = null;
        }
        clearSprites();
    }

    private ImageView copy(ImageView original, String prefix) {
        Bounds bounds = layer.sceneToLocal(original.localToScene(original.getBoundsInLocal()));
        ImageView sprite = new ImageView(original.getImage());
        sprite.setFitWidth(original.getFitWidth());
        sprite.setFitHeight(original.getFitHeight());
        sprite.setPreserveRatio(original.isPreserveRatio());
        sprite.setId(prefix + original.getId());
        sprite.setManaged(false);
        sprite.setMouseTransparent(true);
        sprite.relocate(bounds.getMinX(), bounds.getMinY());
        layer.getChildren().add(sprite);
        original.setVisible(false);
        hiddenOriginals.add(original);
        return sprite;
    }

    private void clearSprites() {
        hiddenOriginals.forEach(original -> original.setVisible(true));
        hiddenOriginals.clear();
        layer.getChildren().clear();
    }
}
