package com.nicholasbelous.connectfour.game;

/** Thrown when a move breaks the rules. The game's state is left unchanged. */
public class IllegalMoveException extends RuntimeException {

    public enum Reason {
        COLUMN_OUT_OF_RANGE,
        COLUMN_FULL,
        GAME_OVER
    }

    private final Reason reason;

    public IllegalMoveException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
