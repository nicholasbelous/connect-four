package com.nicholasbelous.connectfour.service;

import com.nicholasbelous.connectfour.game.Game;

/** A stored game's id together with the game rebuilt from its move log. */
public record SavedGame(long id, Game game) {
}
