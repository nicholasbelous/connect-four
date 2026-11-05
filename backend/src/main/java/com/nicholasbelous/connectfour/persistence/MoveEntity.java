package com.nicholasbelous.connectfour.persistence;

import java.time.Instant;

import org.springframework.data.domain.Persistable;

import com.nicholasbelous.connectfour.game.Player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * A row in {@code moves}: one entry in a game's ordered move log.
 *
 * <p>Implements {@link Persistable} so that saving a new move always INSERTs. With an
 * assigned composite key, Spring Data would otherwise merge, which could overwrite an
 * existing row instead of failing on the primary key.
 */
@Entity
@Table(name = "moves")
@IdClass(MoveId.class)
public class MoveEntity implements Persistable<MoveId> {

    @Id
    @Column(name = "game_id")
    private Long gameId;

    @Id
    @Column(name = "move_number")
    private Integer moveNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Player player;

    @Column(name = "column_index", nullable = false)
    private int columnIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected MoveEntity() {
        // for JPA
    }

    public MoveEntity(long gameId, int moveNumber, Player player, int columnIndex, Instant now) {
        this.gameId = gameId;
        this.moveNumber = moveNumber;
        this.player = player;
        this.columnIndex = columnIndex;
        this.createdAt = now;
    }

    @Override
    public MoveId getId() {
        return new MoveId(gameId, moveNumber);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public Long getGameId() {
        return gameId;
    }

    public Integer getMoveNumber() {
        return moveNumber;
    }

    public Player getPlayer() {
        return player;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
