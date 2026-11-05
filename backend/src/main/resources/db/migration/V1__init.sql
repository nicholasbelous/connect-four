CREATE TABLE games (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    status     VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

-- The ordered move log; game state is derived by replaying it.
-- The primary key allows one move per move number in a game. The server checks that
-- each move has the next move number; the key is the backstop for concurrent requests.
CREATE TABLE moves (
    game_id      BIGINT      NOT NULL,
    move_number  INT         NOT NULL,
    player       VARCHAR(10) NOT NULL,
    column_index INT         NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (game_id, move_number),
    CONSTRAINT fk_moves_game FOREIGN KEY (game_id) REFERENCES games (id)
);
