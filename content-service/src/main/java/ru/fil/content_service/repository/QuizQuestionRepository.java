package ru.fil.content_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.fil.content_service.entity.QuizQuestionId;
import ru.fil.content_service.entity.QuizToQuestion;

import java.util.UUID;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizToQuestion, QuizQuestionId> {

    @Query(value = """
        INSERT INTO quizzes_to_questions (quiz_id, question_id)
        VALUES (:quizId, :questionId)
        """, nativeQuery = true)
    @Modifying
    int saveNative(@Param("quizId") UUID quizId, @Param("questionId") UUID questionId);
}