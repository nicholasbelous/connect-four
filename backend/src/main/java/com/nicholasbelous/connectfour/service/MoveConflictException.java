package com.nicholasbelous.connectfour.service;

/** The move's number is stale or out of order, or another request already made that move. */
public class MoveConflictException extends RuntimeException {

    public MoveConflictException(String message) {
        super(message);
    }
}
