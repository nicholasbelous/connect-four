package com.nicholasbelous.connectfour.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicholasbelous.connectfour.service.GameService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse createGame() {
        return GameResponse.from(gameService.createGame());
    }

    @GetMapping("/{id}")
    public GameResponse getGame(@PathVariable long id) {
        return GameResponse.from(gameService.getGame(id));
    }

    @PostMapping("/{id}/moves")
    public GameResponse playMove(@PathVariable long id, @Valid @RequestBody MoveRequest request) {
        return GameResponse.from(gameService.playMove(id, request.column(), request.moveNumber()));
    }
}
