package com.nicholasbelous.connectfour.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nicholasbelous.connectfour.game.GameStatus;
import com.nicholasbelous.connectfour.game.Player;
import com.nicholasbelous.connectfour.persistence.GameEntity;
import com.nicholasbelous.connectfour.persistence.GameRepository;
import com.nicholasbelous.connectfour.persistence.MoveEntity;
import com.nicholasbelous.connectfour.persistence.MoveRepository;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    private static final long GAME_ID = 7;
    private static final Instant CREATED = Instant.parse("2026-11-04T12:00:00Z");

    @Mock
    private GameRepository games;

    @Mock
    private MoveRepository moves;

    private GameService service;
    private GameEntity storedGame;

    @BeforeEach
    void setUp() {
        service = new GameService(games, moves);

        // A stored game with two moves, so the next move number is 3.
        storedGame = new GameEntity(CREATED);
        when(games.findByIdForUpdate(GAME_ID)).thenReturn(Optional.of(storedGame));
        when(moves.findByGameIdOrderByMoveNumberAsc(GAME_ID)).thenReturn(List.of(
                new MoveEntity(GAME_ID, 1, Player.RED, 3, CREATED),
                new MoveEntity(GAME_ID, 2, Player.YELLOW, 3, CREATED)));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4})
    void staleOrOutOfOrderMoveNumberIsAConflictAndNothingIsSaved(int moveNumber) {
        assertThatThrownBy(() -> service.playMove(GAME_ID, 4, moveNumber))
                .isInstanceOf(MoveConflictException.class)
                .hasMessage("Move number " + moveNumber
                        + " is stale or out of order; the next move number is 3");

        verify(moves, never()).saveAndFlush(any());
        assertThat(storedGame.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(storedGame.getUpdatedAt()).isEqualTo(CREATED);
    }
}
