package com.nicholasbelous.connectfour.service;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nicholasbelous.connectfour.game.Game;
import com.nicholasbelous.connectfour.game.IllegalMoveException;
import com.nicholasbelous.connectfour.game.Player;
import com.nicholasbelous.connectfour.persistence.GameEntity;
import com.nicholasbelous.connectfour.persistence.GameRepository;
import com.nicholasbelous.connectfour.persistence.MoveEntity;
import com.nicholasbelous.connectfour.persistence.MoveRepository;

/**
 * Stores games as an ordered move log and rebuilds each game from it with
 * {@link Game#fromMoves}. All rules live in {@link Game}.
 */
@Service
public class GameService {

    private final GameRepository games;
    private final MoveRepository moves;

    public GameService(GameRepository games, MoveRepository moves) {
        this.games = games;
        this.moves = moves;
    }

    @Transactional
    public SavedGame createGame() {
        GameEntity entity = games.save(new GameEntity(Instant.now()));
        return new SavedGame(entity.getId(), new Game());
    }

    @Transactional(readOnly = true)
    public SavedGame getGame(long id) {
        findGame(id);
        return new SavedGame(id, rebuild(id));
    }

    /**
     * Plays one move. The move insert and the cached status update happen in one transaction.
     *
     * @throws GameNotFoundException if the game doesn't exist
     * @throws MoveConflictException if {@code moveNumber} isn't the game's next move number,
     *         or a concurrent request stored that move number first
     * @throws IllegalMoveException if the move breaks the rules
     */
    @Transactional
    public SavedGame playMove(long gameId, int column, int moveNumber) {
        // Lock the game row first. A concurrent move on the same game waits here, then sees
        // this move and fails the move-number check with a 409. Without the lock, MySQL can
        // deadlock: both inserts hold shared locks on the game row (foreign-key check) and
        // each then needs an exclusive lock on it to update the status.
        GameEntity entity = games.findByIdForUpdate(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        Game game = rebuild(gameId);

        if (moveNumber != game.nextMoveNumber()) {
            throw new MoveConflictException("Move number " + moveNumber
                    + " is stale or out of order; the next move number is " + game.nextMoveNumber());
        }

        Player player = game.currentPlayer();
        game.play(column);

        Instant now = Instant.now();
        try {
            // Flush now so a primary-key collision surfaces here, not at commit.
            moves.saveAndFlush(new MoveEntity(gameId, moveNumber, player, column, now));
        } catch (DataIntegrityViolationException e) {
            // Backstop behind the row lock. The game exists and every column is set, so the
            // primary key is the only constraint this insert can break: another request
            // stored this move first.
            throw new MoveConflictException("Move number " + moveNumber
                    + " was already played by another request");
        }
        entity.recordMove(game.status(), now);

        return new SavedGame(gameId, game);
    }

    private GameEntity findGame(long id) {
        return games.findById(id).orElseThrow(() -> new GameNotFoundException(id));
    }

    private Game rebuild(long gameId) {
        List<Integer> columns = moves.findByGameIdOrderByMoveNumberAsc(gameId).stream()
                .map(MoveEntity::getColumnIndex)
                .toList();
        try {
            return Game.fromMoves(columns);
        } catch (IllegalMoveException e) {
            // Stored moves were validated when played, so this means corrupt data, not a
            // bad request; don't let it surface as a 422.
            throw new IllegalStateException("Stored moves for game " + gameId + " are invalid", e);
        }
    }
}
