package ru.fil.content_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.fil.content_service.entity.Question;
import ru.fil.content_service.entity.QuestionDifficulty;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByTopicIdInAndDifficultyIn(List<UUID> topicIds, List<QuestionDifficulty> difficulties);

    @Query("SELECT DISTINCT q FROM Question q JOIN FETCH q.answers WHERE q.id IN (SELECT qtq.question.id FROM QuizToQuestion qtq WHERE qtq.quiz.id = :quizId)")
    List<Question> findByQuizIdWithAnswers(@Param("quizId") UUID quizId);

    @Query("SELECT a.isCorrect FROM Answer a WHERE a.id = :answerId AND a.question.id = :questionId")
    Boolean isAnswerCorrect(@Param("answerId") UUID answerId, @Param("questionId") UUID questionId);
}
