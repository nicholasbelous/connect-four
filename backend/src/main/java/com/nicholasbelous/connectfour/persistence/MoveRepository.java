package com.nicholasbelous.connectfour.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MoveRepository extends JpaRepository<MoveEntity, MoveId> {

    List<MoveEntity> findByGameIdOrderByMoveNumberAsc(Long gameId);
}
