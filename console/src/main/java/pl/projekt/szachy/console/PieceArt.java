package pl.projekt.szachy.console;

import pl.projekt.szachy.core.model.PieceType;

/** ASCII only: terminal cell widths never depend on Unicode chess glyphs. */
final class PieceArt {
    private PieceArt() { }

    static String[] sprite(PieceType type, boolean compact) {
        if (compact) {
            return switch (type) {
                case KING -> new String[]{"  +  ", " _|_ ", " \\_/ ", " / \\ ", "/___\\"};
                case QUEEN -> new String[]{"o o o", "\\|/|/", " \\_/ ", " / \\ ", "/___\\"};
                case BISHOP -> new String[]{" /\\  ", "(/ ) ", " \\_/ ", " / \\ ", "/___\\"};
                case KNIGHT -> new String[]{" /`-.", "/ . |", "`-/ |", " /  |", "/___\\"};
                case ROOK -> new String[]{"|_|_|", "|___|", " | | ", " / \\ ", "/___\\"};
                case PAWN -> new String[]{"     ", " (_) ", " /|\\ ", " / \\ ", "/___\\"};
            };
        }
        return switch (type) {
            case KING -> new String[]{"    +    ", "   _|_   ", "  /___\\  ", "  \\   /  ", "   | |   ", "  /   \\  ", " /_____\\ "};
            case QUEEN -> new String[]{"         ", " o  o  o ", " \\|/|\\|/ ", "  \\___/  ", "   | |   ", "  /   \\  ", " /_____\\ "};
            case BISHOP -> new String[]{"    /\\   ", "   / /   ", "  (   )  ", "   \\_/   ", "   | |   ", "  /   \\  ", " /_____\\ "};
            case KNIGHT -> new String[]{"   /`-.  ", "  / .  \\ ", " /  __/  ", " `--/ |  ", "   /  |  ", "  /   |  ", " /_____\\ "};
            case ROOK -> new String[]{" |_|_|_| ", " |     | ", "  |   |  ", "  |   |  ", "  |   |  ", " /     \\ ", "/_______\\"};
            case PAWN -> new String[]{"         ", "   ___   ", "  (   )  ", "   \\_/   ", "   | |   ", "  /   \\  ", " /_____\\ "};
        };
    }
}
