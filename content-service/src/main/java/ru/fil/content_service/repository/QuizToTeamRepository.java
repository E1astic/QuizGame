package ru.fil.content_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.fil.content_service.entity.QuizToTeam;
import ru.fil.content_service.entity.QuizTeamId;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizToTeamRepository extends JpaRepository<QuizToTeam, QuizTeamId> {

    @Query("SELECT qtt FROM QuizToTeam qtt JOIN FETCH qtt.quiz JOIN FETCH qtt.team WHERE qtt.quiz.id = :quizId")
    List<QuizToTeam> findByQuizId(@Param("quizId") UUID quizId);

    @Query("SELECT qtt.team.id FROM QuizToTeam qtt WHERE qtt.quiz.id = :quizId")
    List<UUID> findTeamIdsByQuizId(@Param("quizId") UUID quizId);
}
