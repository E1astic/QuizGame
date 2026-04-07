package ru.fil.game_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.fil.game_service.entity.Game;
import ru.fil.game_service.entity.GameStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    
    List<Game> findByStatus(GameStatus status);
    
    Optional<Game> findByIdAndStatus(UUID id, GameStatus status);
}
