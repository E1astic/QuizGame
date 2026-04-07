package ru.fil.game_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT DISTINCT g FROM Game g " +
            "LEFT JOIN FETCH g.quiz q " +
            "LEFT JOIN FETCH q.questions qtq " +
            "LEFT JOIN FETCH qtq.question quest " +
            "LEFT JOIN FETCH quest.answers " +
            "WHERE g.id = :gameId")
    Optional<Game> findByIdWithQuizAndQuestions(@Param("gameId") UUID gameId);

    @Query("SELECT DISTINCT g FROM Game g " +
            "LEFT JOIN FETCH g.gameTeams gt " +
            "LEFT JOIN FETCH gt.team t " +
            "LEFT JOIN FETCH t.players pt " +
            "LEFT JOIN FETCH pt.player p " +
            "LEFT JOIN FETCH g.quiz q " +
            "LEFT JOIN FETCH q.questions qtq " +
            "WHERE g.id = :gameId")
    Optional<Game> findByIdWithTeamsAndPlayers(@Param("gameId") UUID gameId);
}
