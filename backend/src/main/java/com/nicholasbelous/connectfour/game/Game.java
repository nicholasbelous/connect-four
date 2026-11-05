package com.nicholasbelous.connectfour.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A game of Connect Four on a 7-column by 6-row board.
 *
 * <p>Row 0 is the top row, matching how the board is drawn, so discs fall toward row 5.
 * The game is defined by its ordered list of moves (the column of each move); the player
 * for each move follows from its position in that list, with Red first.
 */
public final class Game {

    public static final int ROWS = 6;
    public static final int COLUMNS = 7;

    private static final int CELLS = ROWS * COLUMNS;
    private static final int WIN_LENGTH = 4;

    /**
     * The four line directions as {rowStep, columnStep}: horizontal, vertical, and the two
     * diagonals. Each is walked both ways from the last disc, so four directions cover all
     * eight neighbours.
     */
    private static final int[][] DIRECTIONS = {
            {0, 1},  // horizontal
            {1, 0},  // vertical
            {1, 1},  // diagonal: up-left to down-right
            {-1, 1}, // diagonal: down-left to up-right
    };

    private final Player[][] board = new Player[ROWS][COLUMNS];
    private final List<Integer> moves = new ArrayList<>();
    private final Set<Position> winningCells = new LinkedHashSet<>();
    private GameStatus status = GameStatus.IN_PROGRESS;

    /** Creates a new game with an empty board and Red to move. */
    public Game() {
    }

    /**
     * Replays a move log into a game.
     *
     * @param columns the column of each move, in order
     * @throws IllegalMoveException if any move in the log is illegal
     */
    public static Game fromMoves(List<Integer> columns) {
        Game game = new Game();
        for (int column : columns) {
            game.play(column);
        }
        return game;
    }

    /**
     * Drops the current player's disc into the lowest empty row of a column, then updates
     * the status.
     *
     * @throws IllegalMoveException if the game is over, the column is out of range, or the
     *         column is full; the game is unchanged
     */
    public void play(int column) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalMoveException(IllegalMoveException.Reason.GAME_OVER,
                    "The game is already over");
        }
        if (column < 0 || column >= COLUMNS) {
            throw new IllegalMoveException(IllegalMoveException.Reason.COLUMN_OUT_OF_RANGE,
                    "Column " + column + " is out of range; it must be 0 to " + (COLUMNS - 1));
        }
        int row = lowestEmptyRow(column);
        if (row < 0) {
            throw new IllegalMoveException(IllegalMoveException.Reason.COLUMN_FULL,
                    "Column " + column + " is full");
        }

        Player player = currentPlayer();
        board[row][column] = player;
        moves.add(column);
        updateStatus(new Position(row, column), player);
    }

    /**
     * The player whose turn it is, derived from the number of moves played.
     *
     * <p>Only meaningful while {@link #status()} is {@link GameStatus#IN_PROGRESS}. Once the
     * game is over it still alternates with the move count, but nobody can move.
     */
    public Player currentPlayer() {
        return moves.size() % 2 == 0 ? Player.RED : Player.YELLOW;
    }

    public GameStatus status() {
        return status;
    }

    /** The number the next move will have; the first move is 1. */
    public int nextMoveNumber() {
        return moves.size() + 1;
    }

    /** The column of each move so far, in order. Unmodifiable. */
    public List<Integer> moves() {
        return List.copyOf(moves);
    }

    /** A copy of the board as [row][column], row 0 at the top; empty cells are null. */
    public Player[][] board() {
        Player[][] copy = new Player[ROWS][];
        for (int row = 0; row < ROWS; row++) {
            copy[row] = board[row].clone();
        }
        return copy;
    }

    /**
     * Every disc in each winning line through the last move. Empty unless the game was won.
     * A line can be longer than four, for example when the last disc fills a gap. Unmodifiable.
     */
    public Set<Position> winningCells() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(winningCells));
    }

    /** Returns the lowest empty row in the column (the highest row index), or -1 if it's full. */
    private int lowestEmptyRow(int column) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][column] == null) {
                return row;
            }
        }
        return -1;
    }

    /** Checks only the lines through the disc just played, since any new win must use it. */
    private void updateStatus(Position lastMove, Player player) {
        for (int[] direction : DIRECTIONS) {
            List<Position> line = lineThrough(lastMove, direction[0], direction[1], player);
            if (line.size() >= WIN_LENGTH) {
                winningCells.addAll(line);
            }
        }

        if (!winningCells.isEmpty()) {
            status = (player == Player.RED) ? GameStatus.RED_WON : GameStatus.YELLOW_WON;
        } else if (moves.size() == CELLS) {
            status = GameStatus.DRAW;
        }
    }

    /**
     * Collects the unbroken run of the player's discs that passes through {@code start}
     * along one direction, walking both forwards and backwards from it.
     */
    private List<Position> lineThrough(Position start, int rowStep, int columnStep, Player player) {
        List<Position> line = new ArrayList<>();
        line.add(start);

        // Walk forwards from the start.
        int row = start.row() + rowStep;
        int column = start.column() + columnStep;
        while (isOnBoard(row, column) && board[row][column] == player) {
            line.add(new Position(row, column));
            row += rowStep;
            column += columnStep;
        }

        // Walk backwards from the start.
        row = start.row() - rowStep;
        column = start.column() - columnStep;
        while (isOnBoard(row, column) && board[row][column] == player) {
            line.add(new Position(row, column));
            row -= rowStep;
            column -= columnStep;
        }

        return line;
    }

    private static boolean isOnBoard(int row, int column) {
        return row >= 0 && row < ROWS && column >= 0 && column < COLUMNS;
    }
}
