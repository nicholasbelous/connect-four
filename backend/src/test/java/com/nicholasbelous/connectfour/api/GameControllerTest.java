package com.nicholasbelous.connectfour.api;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import com.nicholasbelous.connectfour.game.Game;
import com.nicholasbelous.connectfour.game.IllegalMoveException;
import com.nicholasbelous.connectfour.service.GameNotFoundException;
import com.nicholasbelous.connectfour.service.GameService;
import com.nicholasbelous.connectfour.service.MoveConflictException;
import com.nicholasbelous.connectfour.service.SavedGame;

@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GameService gameService;

    @Test
    void createGameReturns201WithNewGameState() throws Exception {
        when(gameService.createGame()).thenReturn(new SavedGame(1, new Game()));

        mvc.perform(post("/api/games"))
                .andExpect(status().isCreated())
                .andExpect(content().json("""
                        {
                          "id": 1,
                          "board": [
                            [null, null, null, null, null, null, null],
                            [null, null, null, null, null, null, null],
                            [null, null, null, null, null, null, null],
                            [null, null, null, null, null, null, null],
                            [null, null, null, null, null, null, null],
                            [null, null, null, null, null, null, null]
                          ],
                          "currentPlayer": "RED",
                          "status": "IN_PROGRESS",
                          "moves": [],
                          "nextMoveNumber": 1,
                          "winningCells": []
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getGameReturns200WithBoardRowsTopToBottom() throws Exception {
        when(gameService.getGame(7)).thenReturn(new SavedGame(7, Game.fromMoves(List.of(3, 3, 4))));

        mvc.perform(get("/api/games/7"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "id": 7,
                          "board": [
                            [null, null, null, null,     null,  null, null],
                            [null, null, null, null,     null,  null, null],
                            [null, null, null, null,     null,  null, null],
                            [null, null, null, null,     null,  null, null],
                            [null, null, null, "YELLOW", null,  null, null],
                            [null, null, null, "RED",    "RED", null, null]
                          ],
                          "currentPlayer": "YELLOW",
                          "status": "IN_PROGRESS",
                          "moves": [3, 3, 4],
                          "nextMoveNumber": 4,
                          "winningCells": []
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void finishedGameHasNoCurrentPlayerAndReturnsWinningCells() throws Exception {
        Game won = Game.fromMoves(List.of(0, 1, 0, 1, 0, 1, 0));
        when(gameService.getGame(7)).thenReturn(new SavedGame(7, won));

        mvc.perform(get("/api/games/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RED_WON"))
                .andExpect(jsonPath("$.currentPlayer").value((Object) null))
                .andExpect(jsonPath("$.nextMoveNumber").value(8))
                .andExpect(content().json("""
                        {
                          "winningCells": [
                            {"row": 2, "column": 0},
                            {"row": 3, "column": 0},
                            {"row": 4, "column": 0},
                            {"row": 5, "column": 0}
                          ]
                        }
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void unknownGameReturns404() throws Exception {
        when(gameService.getGame(99)).thenThrow(new GameNotFoundException(99));

        mvc.perform(get("/api/games/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"status": 404, "error": "Not Found", "message": "Game 99 not found"}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void legalMoveReturns200WithUpdatedState() throws Exception {
        when(gameService.playMove(7, 3, 1)).thenReturn(new SavedGame(7, Game.fromMoves(List.of(3))));

        mvc.perform(post("/api/games/7/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"column": 3, "moveNumber": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board[5][3]").value("RED"))
                .andExpect(jsonPath("$.currentPlayer").value("YELLOW"))
                .andExpect(content().json("""
                        {"moves": [3], "nextMoveNumber": 2}
                        """, JsonCompareMode.LENIENT));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",                                   // no body
            "{",                                  // malformed JSON
            "{\"column\": \"abc\", \"moveNumber\": 1}", // wrong type
            "{\"column\": 2.7, \"moveNumber\": 1}",   // not a whole number
            "{\"moveNumber\": 1}",                // column missing
            "{\"column\": 3}",                    // moveNumber missing
            "{\"column\": 3, \"moveNumber\": 0}", // moveNumber not positive
            "{\"column\": 3, \"moveNumber\": -1}",
    })
    void malformedOrInvalidBodyReturns400(String body) throws Exception {
        mvc.perform(post("/api/games/7/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(gameService, never()).playMove(anyLong(), anyInt(), anyInt());
    }

    @Test
    void validationMessageNamesTheInvalidFields() throws Exception {
        mvc.perform(post("/api/games/7/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moveNumber\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "status": 400,
                          "error": "Bad Request",
                          "message": "column must not be null; moveNumber must be greater than 0"
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void staleMoveNumberReturns409() throws Exception {
        when(gameService.playMove(7, 3, 2)).thenThrow(new MoveConflictException(
                "Move number 2 is stale or out of order; the next move number is 3"));

        mvc.perform(post("/api/games/7/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"column\": 3, \"moveNumber\": 2}"))
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {
                          "status": 409,
                          "error": "Conflict",
                          "message": "Move number 2 is stale or out of order; the next move number is 3"
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void illegalMoveReturns422() throws Exception {
        when(gameService.playMove(7, 3, 7)).thenThrow(new IllegalMoveException(
                IllegalMoveException.Reason.COLUMN_FULL, "Column 3 is full"));

        mvc.perform(post("/api/games/7/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"column\": 3, \"moveNumber\": 7}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"status": 422, "error": "Unprocessable Content", "message": "Column 3 is full"}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void unknownMoveGameReturns404() throws Exception {
        when(gameService.playMove(99, 3, 1)).thenThrow(new GameNotFoundException(99));

        mvc.perform(post("/api/games/99/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"column\": 3, \"moveNumber\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Game 99 not found"));
    }
}
