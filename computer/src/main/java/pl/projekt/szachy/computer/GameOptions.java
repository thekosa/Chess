package pl.projekt.szachy.computer;

import pl.projekt.szachy.core.model.Side;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record GameOptions(Opponent opponent, Difficulty difficulty, Side humanSide) {
    public GameOptions {
        Objects.requireNonNull(opponent);
        Objects.requireNonNull(difficulty);
        Objects.requireNonNull(humanSide);
    }
    public static GameOptions local() {
        return new GameOptions(Opponent.TWO_PLAYERS, Difficulty.MEDIUM, Side.WHITE);
    }
    public List<String> arguments() {
        return List.of("--opponent=" + opponent.name(), "--difficulty=" + difficulty.name(),
                "--side=" + humanSide.name());
    }
    public static GameOptions fromArguments(String[] args) {
        GameOptions defaults = local();
        Opponent opponent = defaults.opponent;
        Difficulty difficulty = defaults.difficulty;
        Side side = defaults.humanSide;
        for (String arg : args) {
            String value = arg.substring(arg.indexOf('=') + 1).toUpperCase(Locale.ROOT);
            if (arg.startsWith("--opponent=")) opponent = Opponent.valueOf(value);
            if (arg.startsWith("--difficulty=")) difficulty = Difficulty.valueOf(value);
            if (arg.startsWith("--side=")) side = Side.valueOf(value);
        }
        return new GameOptions(opponent, difficulty, side);
    }
}
