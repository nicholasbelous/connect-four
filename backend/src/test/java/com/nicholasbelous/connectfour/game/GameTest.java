package com.nicholasbelous.connectfour.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Board pictures in these tests list rows top (row 0) to bottom (row 5):
 * R = Red, Y = Yellow, . = empty.
 */
class GameTest {

    // Full-board sequences, shared by several tests.

    /**
     * Fills the board with no four in a row. Columns 0, 1, 4, 5 alternate R/Y from the
     * bottom, columns 2, 3, 6 alternate Y/R, which gives the picture in {@link #DRAW_PICTURE}.
     */
    private static final List<Integer> DRAW_SEQUENCE = concat(
            repeat(0, 6), repeat(1, 6), repeat(4, 6), List.of(5),
            repeat(2, 6), repeat(3, 6), repeat(6, 6), repeat(5, 5));

    private static final List<String> DRAW_PICTURE = List.of(
            "YYRRYYR",
            "RRYYRRY",
            "YYRRYYR",
            "RRYYRRY",
            "YYRRYYR",
            "RRYYRRY");

    /**
     * Columns 0-5 filled so that Yellow's 18th disc, at the top of column 3, completes
     * four in a row along the top row. Column 6 stays empty.
     */
    private static final List<Integer> TOP_ROW_WIN_SEQUENCE = concat(
            repeat(4, 5), repeat(2, 5), repeat(5, 5), repeat(3, 5),
            repeat(0, 6), repeat(1, 6), List.of(4, 2, 5, 3));

    /** Like the top-row win, plus column 6: the 42nd move fills the board and wins. */
    private static final List<Integer> FULL_BOARD_WIN_SEQUENCE = concat(
            repeat(4, 5), repeat(6, 6), repeat(2, 5), repeat(5, 5), repeat(3, 5),
            repeat(0, 6), repeat(1, 6), List.of(4, 2, 5, 3));

    @Nested
    class NewGame {

        @Test
        void startsEmptyWithRedToMoveAndMoveNumberOne() {
            Game game = new Game();

            assertThat(picture(game.board())).containsExactly(
                    ".......",
                    ".......",
                    ".......",
                    ".......",
                    ".......",
                    ".......");
            assertThat(game.currentPlayer()).isEqualTo(Player.RED);
            assertThat(game.nextMoveNumber()).isEqualTo(1);
            assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(game.moves()).isEmpty();
            assertThat(game.winningCells()).isEmpty();
        }

        @Test
        void boardIsSixRowsBySevenColumns() {
            Player[][] board = new Game().board();

            assertThat(Game.ROWS).isEqualTo(6);
            assertThat(Game.COLUMNS).isEqualTo(7);
            assertThat(board).hasDimensions(Game.ROWS, Game.COLUMNS);
        }
    }

    @Nested
    class Playing {

        @Test
        void discsStackFromTheBottomAndPlayersAlternate() {
            Game game = new Game();

            game.play(3);
            assertThat(game.board()[5][3]).isEqualTo(Player.RED);
            assertThat(game.currentPlayer()).isEqualTo(Player.YELLOW);
            assertThat(game.nextMoveNumber()).isEqualTo(2);

            game.play(3);
            assertThat(game.board()[4][3]).isEqualTo(Player.YELLOW);
            assertThat(game.currentPlayer()).isEqualTo(Player.RED);
            assertThat(game.nextMoveNumber()).isEqualTo(3);

            game.play(4);
            assertThat(picture(game.board())).containsExactly(
                    ".......",
                    ".......",
                    ".......",
                    ".......",
                    "...Y...",
                    "...RR..");
            assertThat(game.moves()).containsExactly(3, 3, 4);
            assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
        }

        @Test
        void boardIsACopy() {
            Game game = playAll(3);

            game.board()[5][3] = Player.YELLOW;
            game.board()[0][0] = Player.RED;

            assertThat(game.board()[5][3]).isEqualTo(Player.RED);
            assertThat(game.board()[0][0]).isNull();
        }
    }

    @Nested
    class Wins {

        @Test
        void horizontal() {
            Game game = playAll(1, 1, 2, 2, 3, 3, 4);

            assertThat(picture(game.board())).containsExactly(
                    ".......",
                    ".......",
                    ".......",
                    ".......",
                    ".YYY...",
                    ".RRRR..");
            assertThat(game.status()).isEqualTo(GameStatus.RED_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(5, 1), new Position(5, 2), new Position(5, 3), new Position(5, 4));
        }

        @Test
        void vertical() {
            Game game = playAll(3, 4, 3, 4, 3, 4, 3);

            assertThat(game.status()).isEqualTo(GameStatus.RED_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(2, 3), new Position(3, 3), new Position(4, 3), new Position(5, 3));
        }

        @Test
        void diagonalFromDownLeftToUpRight() {
            Game game = playAll(0, 1, 1, 2, 2, 3, 2, 3, 3, 0, 3);

            assertThat(picture(game.board())).containsExactly(
                    ".......",
                    ".......",
                    "...R...",
                    "..RR...",
                    "YRRY...",
                    "RYYY...");
            assertThat(game.status()).isEqualTo(GameStatus.RED_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(5, 0), new Position(4, 1), new Position(3, 2), new Position(2, 3));
        }

        @Test
        void diagonalFromUpLeftToDownRight() {
            Game game = playAll(6, 5, 5, 4, 4, 3, 4, 3, 3, 6, 3);

            assertThat(picture(game.board())).containsExactly(
                    ".......",
                    ".......",
                    "...R...",
                    "...RR..",
                    "...YRRY",
                    "...YYYR");
            assertThat(game.status()).isEqualTo(GameStatus.RED_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(2, 3), new Position(3, 4), new Position(4, 5), new Position(5, 6));
        }

        @Test
        void yellowCanWin() {
            Game game = playAll(0, 3, 1, 3, 0, 3, 1, 3);

            assertThat(game.status()).isEqualTo(GameStatus.YELLOW_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(2, 3), new Position(3, 3), new Position(4, 3), new Position(5, 3));
        }
    }

    @Nested
    class WinsAtTheEdges {

        @Test
        void inColumnZero() {
            Game game = playAll(0, 1, 0, 1, 0, 1, 0);

            assertThat(game.status()).isEqualTo(GameStatus.RED_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(2, 0), new Position(3, 0), new Position(4, 0), new Position(5, 0));
        }

        @Test
        void inColumnSix() {
            Game game = playAll(6, 5, 6, 5, 6, 5, 6);

            assertThat(game.status()).isEqualTo(GameStatus.RED_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(2, 6), new Position(3, 6), new Position(4, 6), new Position(5, 6));
        }

        @Test
        void inTheTopRow() {
            Game game = playAll(TOP_ROW_WIN_SEQUENCE);

            assertThat(picture(game.board())).containsExactly(
                    "YYYYRR.",
                    "RRYYRR.",
                    "YYRRYY.",
                    "RRYYRR.",
                    "YYRRYY.",
                    "RRYYRR.");
            assertThat(game.status()).isEqualTo(GameStatus.YELLOW_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(0, 0), new Position(0, 1), new Position(0, 2), new Position(0, 3));
        }
    }

    @Nested
    class WinningCells {

        @Test
        void fillingAGapThatMakesFiveInARowReturnsAllFive() {
            Game game = playAll(0, 0, 1, 1, 3, 3, 4, 4);
            assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);

            game.play(2);

            assertThat(picture(game.board())).containsExactly(
                    ".......",
                    ".......",
                    ".......",
                    ".......",
                    "YY.YY..",
                    "RRRRR..");
            assertThat(game.status()).isEqualTo(GameStatus.RED_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(5, 0), new Position(5, 1), new Position(5, 2),
                    new Position(5, 3), new Position(5, 4));
        }

        @Test
        void moveCompletingTwoLinesReturnsBothLines() {
            Game game = playAll(0, 0, 1, 0, 0, 1, 1, 6, 2, 6, 2, 5, 3);

            assertThat(picture(game.board())).containsExactly(
                    ".......",
                    ".......",
                    "R......",
                    "YR.....",
                    "YYR...Y",
                    "RRRR.YY");
            assertThat(game.status()).isEqualTo(GameStatus.RED_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    // horizontal
                    new Position(5, 0), new Position(5, 1), new Position(5, 2), new Position(5, 3),
                    // diagonal through the same last move
                    new Position(4, 2), new Position(3, 1), new Position(2, 0));
        }

        @Test
        void areEmptyWhileTheGameIsInProgress() {
            Game game = playAll(0, 0, 1, 1, 2);

            assertThat(game.winningCells()).isEmpty();
        }
    }

    @Nested
    class NotWins {

        @Test
        void threeInARowIsNotAWin() {
            Game game = playAll(0, 0, 1, 1, 2);

            assertThat(picture(game.board())).containsExactly(
                    ".......",
                    ".......",
                    ".......",
                    ".......",
                    "YY.....",
                    "RRR....");
            assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(game.currentPlayer()).isEqualTo(Player.YELLOW);
        }

        @Test
        void lineBrokenByOpponentDiscIsNotAWin() {
            Game game = playAll(0, 2, 1, 6, 3, 6, 4);

            assertThat(picture(game.board())).containsExactly(
                    ".......",
                    ".......",
                    ".......",
                    ".......",
                    "......Y",
                    "RRYRR.Y");
            assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(game.winningCells()).isEmpty();
        }
    }

    @Nested
    class IllegalMoves {

        @ParameterizedTest
        @ValueSource(ints = {-1, 7})
        void columnOutOfRange(int column) {
            Game game = playAll(3);

            assertIllegalMove(() -> game.play(column), IllegalMoveException.Reason.COLUMN_OUT_OF_RANGE);
            assertUnchanged(game, playAll(3));
        }

        @Test
        void columnFull() {
            Game game = playAll(0, 0, 0, 0, 0, 0);
            assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);

            assertIllegalMove(() -> game.play(0), IllegalMoveException.Reason.COLUMN_FULL);
            assertUnchanged(game, playAll(0, 0, 0, 0, 0, 0));
        }

        @Test
        void moveAfterAWin() {
            Game game = playAll(0, 1, 0, 1, 0, 1, 0);
            assertThat(game.status()).isEqualTo(GameStatus.RED_WON);

            assertIllegalMove(() -> game.play(2), IllegalMoveException.Reason.GAME_OVER);
            assertUnchanged(game, playAll(0, 1, 0, 1, 0, 1, 0));
        }

        @Test
        void moveAfterADraw() {
            Game game = playAll(DRAW_SEQUENCE);
            assertThat(game.status()).isEqualTo(GameStatus.DRAW);

            // Every column is also full; GAME_OVER is the reason that applies.
            assertIllegalMove(() -> game.play(0), IllegalMoveException.Reason.GAME_OVER);
            assertUnchanged(game, playAll(DRAW_SEQUENCE));
        }
    }

    @Nested
    class FullBoard {

        @Test
        void fullBoardWithNoFourInARowIsADraw() {
            Game game = playAll(DRAW_SEQUENCE.subList(0, 41));
            assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);

            game.play(DRAW_SEQUENCE.get(41));
            Player[][] board = game.board();

            // Prove the board really is a draw position, independently of Game's own
            // win detection: it's full and a brute-force scan finds no line of four.
            assertThat(picture(board)).containsExactlyElementsOf(DRAW_PICTURE);
            assertThat(countDiscs(board)).isEqualTo(Game.ROWS * Game.COLUMNS);
            assertThat(linesOfFour(board)).isEmpty();

            assertThat(game.status()).isEqualTo(GameStatus.DRAW);
            assertThat(game.winningCells()).isEmpty();
            assertThat(game.nextMoveNumber()).isEqualTo(43);
        }

        @Test
        void finalMoveThatFillsTheBoardAndMakesFourIsAWinNotADraw() {
            Game game = playAll(FULL_BOARD_WIN_SEQUENCE.subList(0, 41));
            assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(linesOfFour(game.board())).isEmpty();

            game.play(FULL_BOARD_WIN_SEQUENCE.get(41));
            Player[][] board = game.board();

            assertThat(picture(board)).containsExactly(
                    "YYYYRRR",
                    "RRYYRRY",
                    "YYRRYYR",
                    "RRYYRRY",
                    "YYRRYYR",
                    "RRYYRRY");
            assertThat(countDiscs(board)).isEqualTo(Game.ROWS * Game.COLUMNS);
            assertThat(game.status()).isEqualTo(GameStatus.YELLOW_WON);
            assertThat(game.winningCells()).containsExactlyInAnyOrder(
                    new Position(0, 0), new Position(0, 1), new Position(0, 2), new Position(0, 3));
        }
    }

    @Nested
    class FromMoves {

        static Stream<Arguments> moveLogs() {
            return Stream.of(
                    Arguments.of("empty", List.of()),
                    Arguments.of("in progress", List.of(3, 3, 4, 2, 0)),
                    Arguments.of("red won", List.of(0, 1, 1, 2, 2, 3, 2, 3, 3, 0, 3)),
                    Arguments.of("yellow won", TOP_ROW_WIN_SEQUENCE),
                    Arguments.of("draw", DRAW_SEQUENCE));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("moveLogs")
        void replaysToTheSameStateAsPlayingMovesOneByOne(String description, List<Integer> moves) {
            Game replayed = Game.fromMoves(moves);

            assertUnchanged(replayed, playAll(moves));
            assertThat(replayed.moves()).containsExactlyElementsOf(moves);
        }

        static Stream<Arguments> illegalMoveLogs() {
            return Stream.of(
                    Arguments.of(List.of(3, 7), IllegalMoveException.Reason.COLUMN_OUT_OF_RANGE),
                    Arguments.of(List.of(3, -1), IllegalMoveException.Reason.COLUMN_OUT_OF_RANGE),
                    Arguments.of(repeat(0, 7), IllegalMoveException.Reason.COLUMN_FULL),
                    Arguments.of(List.of(0, 1, 0, 1, 0, 1, 0, 1), IllegalMoveException.Reason.GAME_OVER));
        }

        @ParameterizedTest
        @MethodSource("illegalMoveLogs")
        void rejectsAnIllegalSequence(List<Integer> moves, IllegalMoveException.Reason reason) {
            assertIllegalMove(() -> Game.fromMoves(moves), reason);
        }
    }

    // Helpers

    private static Game playAll(int... columns) {
        Game game = new Game();
        for (int column : columns) {
            game.play(column);
        }
        return game;
    }

    private static Game playAll(List<Integer> columns) {
        return playAll(columns.stream().mapToInt(Integer::intValue).toArray());
    }

    private static void assertIllegalMove(ThrowingCallable move, IllegalMoveException.Reason reason) {
        assertThatThrownBy(move).isInstanceOfSatisfying(IllegalMoveException.class,
                e -> assertThat(e.reason()).isEqualTo(reason));
    }

    /** Asserts every piece of state the Game exposes matches the expected game. */
    private static void assertUnchanged(Game actual, Game expected) {
        assertThat(actual.board()).isDeepEqualTo(expected.board());
        assertThat(actual.currentPlayer()).isEqualTo(expected.currentPlayer());
        assertThat(actual.status()).isEqualTo(expected.status());
        assertThat(actual.nextMoveNumber()).isEqualTo(expected.nextMoveNumber());
        assertThat(actual.moves()).isEqualTo(expected.moves());
        assertThat(actual.winningCells()).isEqualTo(expected.winningCells());
    }

    private static List<String> picture(Player[][] board) {
        List<String> rows = new ArrayList<>();
        for (Player[] row : board) {
            StringBuilder line = new StringBuilder();
            for (Player cell : row) {
                line.append(cell == null ? '.' : cell == Player.RED ? 'R' : 'Y');
            }
            rows.add(line.toString());
        }
        return rows;
    }

    private static long countDiscs(Player[][] board) {
        return Stream.of(board).flatMap(Stream::of).filter(cell -> cell != null).count();
    }

    /**
     * Brute-force scan, independent of Game: every 4-cell window in every direction
     * from every cell. Returns the starting cell of each window holding four matching discs.
     */
    private static List<Position> linesOfFour(Player[][] board) {
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {-1, 1}};
        List<Position> found = new ArrayList<>();
        for (int row = 0; row < board.length; row++) {
            for (int column = 0; column < board[row].length; column++) {
                for (int[] d : directions) {
                    int endRow = row + 3 * d[0];
                    int endColumn = column + 3 * d[1];
                    if (endRow < 0 || endRow >= board.length || endColumn >= board[row].length) {
                        continue;
                    }
                    Player first = board[row][column];
                    if (first == null) {
                        continue;
                    }
                    final int r = row;
                    final int c = column;
                    boolean allMatch = IntStream.range(1, 4)
                            .allMatch(i -> board[r + i * d[0]][c + i * d[1]] == first);
                    if (allMatch) {
                        found.add(new Position(row, column));
                    }
                }
            }
        }
        return found;
    }

    private static List<Integer> repeat(int column, int times) {
        return Collections.nCopies(times, column);
    }

    @SafeVarargs
    private static List<Integer> concat(List<Integer>... parts) {
        List<Integer> all = new ArrayList<>();
        for (List<Integer> part : parts) {
            all.addAll(part);
        }
        return List.copyOf(all);
    }
}
