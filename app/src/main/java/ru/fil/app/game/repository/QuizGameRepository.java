package ru.fil.app.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.fil.app.game.entity.QuizGame;
import ru.fil.app.game.entity.GameStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizGameRepository extends JpaRepository<QuizGame, UUID> {

    @Query("SELECT qg FROM QuizGame qg JOIN FETCH qg.quiz JOIN FETCH qg.gameTeams gt JOIN FETCH gt.team WHERE qg.id = :id")
    Optional<QuizGame> findByIdWithTeams(@Param("id") UUID id);

    @Query("SELECT qg FROM QuizGame qg JOIN FETCH qg.quiz JOIN FETCH qg.gameTeams gt JOIN FETCH gt.team WHERE qg.status = :status")
    List<QuizGame> findByStatusWithTeams(@Param("status") GameStatus status);

    @Query("SELECT qg FROM QuizGame qg JOIN FETCH qg.quiz JOIN FETCH qg.gameTeams gt JOIN FETCH gt.team WHERE qg.quiz.id = :quizId AND qg.status = :status")
    Optional<QuizGame> findByQuizIdAndStatus(@Param("quizId") UUID quizId, @Param("status") GameStatus status);
}
