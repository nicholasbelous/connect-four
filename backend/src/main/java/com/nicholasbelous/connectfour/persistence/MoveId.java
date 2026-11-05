package com.nicholasbelous.connectfour.persistence;

import java.io.Serializable;
import java.util.Objects;

/** Composite primary key of {@link MoveEntity}: (game_id, move_number). */
public class MoveId implements Serializable {

    private Long gameId;
    private Integer moveNumber;

    protected MoveId() {
        // for JPA
    }

    public MoveId(Long gameId, Integer moveNumber) {
        this.gameId = gameId;
        this.moveNumber = moveNumber;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MoveId that)) {
            return false;
        }
        return Objects.equals(gameId, that.gameId) && Objects.equals(moveNumber, that.moveNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, moveNumber);
    }
}
