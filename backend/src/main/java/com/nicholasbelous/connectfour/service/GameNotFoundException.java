package com.nicholasbelous.connectfour.service;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(long id) {
        super("Game " + id + " not found");
    }
}
