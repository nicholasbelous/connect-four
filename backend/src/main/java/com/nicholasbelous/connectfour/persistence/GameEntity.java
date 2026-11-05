package com.nicholasbelous.connectfour.persistence;

import java.time.Instant;

import com.nicholasbelous.connectfour.game.GameStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A row in {@code games}. The status is a cached copy of the state derived from the move
 * log, updated in the same transaction as each move insert.
 */
@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GameEntity() {
        // for JPA
    }

    public GameEntity(Instant now) {
        this.status = GameStatus.IN_PROGRESS;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Records that a move was made, caching the resulting status. */
    public void recordMove(GameStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
