package com.nicholasbelous.connectfour.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface GameRepository extends JpaRepository<GameEntity, Long> {

    /**
     * Loads a game with a row lock (SELECT ... FOR UPDATE), so moves on the same game run
     * one at a time until the transaction ends.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GameEntity g where g.id = :id")
    Optional<GameEntity> findByIdForUpdate(@Param("id") Long id);
}
