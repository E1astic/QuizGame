package ru.fil.game_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.fil.game_service.entity.GameTeam;
import ru.fil.game_service.entity.GameTeamId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameTeamRepository extends JpaRepository<GameTeam, GameTeamId> {
    
    List<GameTeam> findByGameId(UUID gameId);
    
    Optional<GameTeam> findByGameIdAndTeamId(UUID gameId, UUID teamId);
}
