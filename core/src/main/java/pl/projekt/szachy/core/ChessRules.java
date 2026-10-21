package pl.projekt.szachy.core;

import pl.projekt.szachy.core.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ChessRules {
    private static final int[][] ORTHOGONAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONAL = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] KNIGHT = {
            {1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}
    };
    private static final PieceType[] PROMOTIONS = {
            PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
    };

    private ChessRules() {
    }

    static List<Move> legalMoves(GameState state, Square from) {
        Piece piece = state.board().get(from);
        if (piece == null || piece.side() != state.sideToMove()) {
            return List.of();
        }
        List<Move> candidates = new ArrayList<>();
        switch (piece.type()) {
            case PAWN -> pawnMoves(state, from, piece, candidates);
            case KNIGHT -> stepMoves(state, from, piece, KNIGHT, candidates);
            case BISHOP -> slidingMoves(state, from, piece, DIAGONAL, candidates);
            case ROOK -> slidingMoves(state, from, piece, ORTHOGONAL, candidates);
            case QUEEN -> {
                slidingMoves(state, from, piece, ORTHOGONAL, candidates);
                slidingMoves(state, from, piece, DIAGONAL, candidates);
            }
            case KING -> {
                stepMoves(state, from, piece, ORTHOGONAL, candidates);
                stepMoves(state, from, piece, DIAGONAL, candidates);
                castlingMoves(state, from, piece, candidates);
            }
        }
        return candidates.stream()
                .filter(move -> !inCheck(apply(state, move, false).board(), piece.side()))
                .toList();
    }

    private static void pawnMoves(GameState state, Square from, Piece pawn, List<Move> moves) {
        int direction = pawn.side().pawnDirection();
        Square forward = offset(from, 0, direction);
        if (forward != null && !state.board().containsKey(forward)) {
            addPawnMove(from, forward, moves);
            int homeRank = pawn.side() == Side.WHITE ? 1 : 6;
            Square twice = offset(from, 0, direction * 2);
            if (from.rank() == homeRank && twice != null && !state.board().containsKey(twice)) {
                moves.add(new Move(from, twice));
            }
        }
        for (int fileStep : new int[]{-1, 1}) {
            Square target = offset(from, fileStep, direction);
            if (target == null) {
                continue;
            }
            Piece captured = state.board().get(target);
            if (captured != null && capturable(captured, pawn.side())) {
                addPawnMove(from, target, moves);
            } else if (captured == null && target.equals(state.enPassantTarget())) {
                Piece adjacent = state.board().get(new Square(target.file(), from.rank()));
                if (adjacent != null && adjacent.side() != pawn.side() && adjacent.type() == PieceType.PAWN) {
                    moves.add(new Move(from, target));
                }
            }
        }
    }

    private static void addPawnMove(Square from, Square to, List<Move> moves) {
        if (to.rank() == 0 || to.rank() == 7) {
            for (PieceType type : PROMOTIONS) {
                moves.add(new Move(from, to, type));
            }
        } else {
            moves.add(new Move(from, to));
        }
    }

    private static void stepMoves(GameState state, Square from, Piece piece,
                                  int[][] directions, List<Move> moves) {
        for (int[] direction : directions) {
            Square to = offset(from, direction[0], direction[1]);
            if (to != null && available(state.board().get(to), piece.side())) {
                moves.add(new Move(from, to));
            }
        }
    }

    private static void slidingMoves(GameState state, Square from, Piece piece,
                                     int[][] directions, List<Move> moves) {
        for (int[] direction : directions) {
            Square target = offset(from, direction[0], direction[1]);
            while (target != null) {
                Piece blocker = state.board().get(target);
                if (available(blocker, piece.side())) {
                    moves.add(new Move(from, target));
                }
                if (blocker != null) {
                    break;
                }
                target = offset(target, direction[0], direction[1]);
            }
        }
    }

    private static boolean available(Piece target, Side movingSide) {
        return target == null || capturable(target, movingSide);
    }

    private static boolean capturable(Piece target, Side movingSide) {
        return target.side() != movingSide && target.type() != PieceType.KING;
    }

    private static void castlingMoves(GameState state, Square from, Piece king, List<Move> moves) {
        int homeRank = king.side() == Side.WHITE ? 0 : 7;
        if (!from.equals(new Square(4, homeRank)) || inCheck(state.board(), king.side())) {
            return;
        }
        for (boolean kingSide : new boolean[]{true, false}) {
            if (!state.castlingRights().allows(king.side(), kingSide)) {
                continue;
            }
            Piece rook = state.board().get(new Square(kingSide ? 7 : 0, homeRank));
            if (!new Piece(king.side(), PieceType.ROOK).equals(rook)) {
                continue;
            }
            int step = kingSide ? 1 : -1;
            int limit = kingSide ? 7 : 0;
            boolean clear = true;
            for (int file = 4 + step; file != limit; file += step) {
                if (state.board().containsKey(new Square(file, homeRank))) {
                    clear = false;
                    break;
                }
            }
            Square through = new Square(4 + step, homeRank);
            Square target = new Square(4 + 2 * step, homeRank);
            if (clear && !attacked(state.board(), through, king.side().opposite())
                    && !attacked(state.board(), target, king.side().opposite())) {
                moves.add(new Move(from, target));
            }
        }
    }

    static boolean inCheck(Map<Square, Piece> board, Side side) {
        return board.entrySet().stream()
                .filter(entry -> entry.getValue().side() == side && entry.getValue().type() == PieceType.KING)
                .findFirst()
                .map(entry -> attacked(board, entry.getKey(), side.opposite()))
                .orElse(true);
    }

    /** Attacks are separate from moves: pawns attack empty diagonals and kings adjacent squares. */
    private static boolean attacked(Map<Square, Piece> board, Square target, Side attacker) {
        for (var entry : board.entrySet()) {
            Piece piece = entry.getValue();
            if (piece.side() != attacker) {
                continue;
            }
            Square from = entry.getKey();
            int df = target.file() - from.file();
            int dr = target.rank() - from.rank();
            int af = Math.abs(df);
            int ar = Math.abs(dr);
            if (af == 0 && ar == 0) {
                continue;
            }
            boolean diagonal = af == ar;
            boolean straight = df == 0 || dr == 0;
            boolean threatens = switch (piece.type()) {
                case PAWN -> af == 1 && dr == attacker.pawnDirection();
                case KNIGHT -> af * ar == 2;
                case KING -> af <= 1 && ar <= 1;
                case BISHOP -> diagonal && clearPath(board, from, target);
                case ROOK -> straight && clearPath(board, from, target);
                case QUEEN -> (diagonal || straight) && clearPath(board, from, target);
            };
            if (threatens) {
                return true;
            }
        }
        return false;
    }

    private static boolean clearPath(Map<Square, Piece> board, Square from, Square to) {
        int df = Integer.signum(to.file() - from.file());
        int dr = Integer.signum(to.rank() - from.rank());
        Square step = offset(from, df, dr);
        while (step != null && !step.equals(to)) {
            if (board.containsKey(step)) {
                return false;
            }
            step = offset(step, df, dr);
        }
        return step != null;
    }

    /** Called only for generated candidates or after validation by LocalGame. */
    static GameState apply(GameState state, Move move, boolean recordHistory) {
        Map<Square, Piece> board = new HashMap<>(state.board());
        Piece piece = board.remove(move.from());
        Square capturedSquare = move.to();
        if (piece.type() == PieceType.PAWN && move.from().file() != move.to().file()
                && !board.containsKey(move.to())) {
            capturedSquare = new Square(move.to().file(), move.from().rank());
        }
        Piece captured = board.remove(capturedSquare);
        board.put(move.to(), move.promotion() == null ? piece : new Piece(piece.side(), move.promotion()));
        if (piece.type() == PieceType.KING && Math.abs(move.to().file() - move.from().file()) == 2) {
            boolean kingSide = move.to().file() == 6;
            Piece rook = board.remove(new Square(kingSide ? 7 : 0, move.from().rank()));
            board.put(new Square(kingSide ? 5 : 3, move.from().rank()), rook);
        }
        CastlingRights rights = state.castlingRights();
        if (piece.type() == PieceType.KING) {
            rights = rights.withoutKing(piece.side());
        } else if (piece.type() == PieceType.ROOK) {
            rights = rights.withoutRook(piece.side(), move.from());
        }
        if (captured != null && captured.type() == PieceType.ROOK) {
            rights = rights.withoutRook(captured.side(), capturedSquare);
        }
        Square enPassant = piece.type() == PieceType.PAWN
                && Math.abs(move.to().rank() - move.from().rank()) == 2
                ? new Square(move.from().file(), (move.to().rank() + move.from().rank()) / 2) : null;
        List<Move> history = state.moveHistory();
        List<Piece> captures = state.capturedPieces();
        if (recordHistory) {
            history = new ArrayList<>(history);
            history.add(move);
            if (captured != null) {
                captures = new ArrayList<>(captures);
                captures.add(captured);
            }
        }
        return new GameState(board, piece.side().opposite(), rights, enPassant,
                GameStatus.ACTIVE, history, captures,
                piece.type() == PieceType.PAWN || captured != null ? 0 : state.halfmoveClock() + 1, 1);
    }

    static GameState updateStatus(GameState state) {
        boolean check = inCheck(state.board(), state.sideToMove());
        boolean hasMove = state.board().entrySet().stream()
                .filter(entry -> entry.getValue().side() == state.sideToMove())
                .anyMatch(entry -> !legalMoves(state, entry.getKey()).isEmpty());
        // A mate on the final move takes precedence over the 75-move rule.
        if (!hasMove) return state.withStatus(check ? GameStatus.CHECKMATE : GameStatus.STALEMATE);
        if (insufficientMaterial(state.board())) return state.withStatus(GameStatus.DRAW_INSUFFICIENT_MATERIAL);
        if (state.halfmoveClock() >= 150) return state.withStatus(GameStatus.DRAW_SEVENTY_FIVE_MOVES);
        if (state.repetitionCount() >= 5) return state.withStatus(GameStatus.DRAW_FIVEFOLD_REPETITION);
        return state.withStatus(check ? GameStatus.CHECK : GameStatus.ACTIVE);
    }

    record PositionKey(Map<Square, Piece> board, Side side, CastlingRights rights, Square enPassant) { }

    static PositionKey positionKey(GameState state) {
        Square effectiveEnPassant = null;
        if (state.enPassantTarget() != null) {
            // An unavailable or pinned en-passant capture does not distinguish positions.
            boolean capturable = state.board().entrySet().stream()
                    .filter(entry -> entry.getValue().side() == state.sideToMove()
                            && entry.getValue().type() == PieceType.PAWN)
                    .anyMatch(entry -> legalMoves(state, entry.getKey()).stream()
                            .anyMatch(move -> move.to().equals(state.enPassantTarget())
                                    && move.from().file() != move.to().file()));
            if (capturable) effectiveEnPassant = state.enPassantTarget();
        }
        return new PositionKey(state.board(), state.sideToMove(), state.castlingRights(), effectiveEnPassant);
    }

    private static boolean insufficientMaterial(Map<Square, Piece> board) {
        var pieces = board.entrySet().stream().filter(entry -> entry.getValue().type() != PieceType.KING).toList();
        if (pieces.isEmpty()) return true;
        if (pieces.size() == 1) return pieces.getFirst().getValue().type() == PieceType.BISHOP
                || pieces.getFirst().getValue().type() == PieceType.KNIGHT;
        if (pieces.stream().anyMatch(entry -> entry.getValue().type() != PieceType.BISHOP)) return false;
        int color = (pieces.getFirst().getKey().file() + pieces.getFirst().getKey().rank()) % 2;
        return pieces.stream().allMatch(entry -> (entry.getKey().file() + entry.getKey().rank()) % 2 == color);
    }

    private static Square offset(Square from, int df, int dr) {
        int file = from.file() + df;
        int rank = from.rank() + dr;
        return file >= 0 && file < 8 && rank >= 0 && rank < 8 ? new Square(file, rank) : null;
    }
}
