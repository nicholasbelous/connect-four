package com.nicholasbelous.connectfour.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.nicholasbelous.connectfour.game.Game;
import com.nicholasbelous.connectfour.game.GameStatus;
import com.nicholasbelous.connectfour.game.Player;
import com.nicholasbelous.connectfour.game.Position;
import com.nicholasbelous.connectfour.service.SavedGame;

/**
 * Game state returned by every game endpoint.
 *
 * @param board rows top to bottom, each cell "RED", "YELLOW", or null
 * @param currentPlayer whose turn it is; null unless the status is IN_PROGRESS
 * @param moves the column of each move so far, in order
 * @param winningCells every disc in each winning line; empty unless someone won
 */
public record GameResponse(
        long id,
        List<List<Player>> board,
        Player currentPlayer,
        GameStatus status,
        List<Integer> moves,
        int nextMoveNumber,
        List<Position> winningCells) {

    static GameResponse from(SavedGame saved) {
        Game game = saved.game();

        List<List<Player>> board = new ArrayList<>();
        for (Player[] row : game.board()) {
            board.add(Arrays.asList(row)); // keeps nulls for empty cells
        }

        Player currentPlayer = game.status() == GameStatus.IN_PROGRESS ? game.currentPlayer() : null;

        return new GameResponse(
                saved.id(),
                board,
                currentPlayer,
                game.status(),
                game.moves(),
                game.nextMoveNumber(),
                List.copyOf(game.winningCells()));
    }
}
