package com.nicholasbelous.connectfour.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body of {@code POST /api/games/{id}/moves}. The column's range is a game rule, so an
 * out-of-range column is a 422 from the game, not a 400 from validation.
 */
public record MoveRequest(
        @NotNull Integer column,
        @NotNull @Positive Integer moveNumber) {
}
