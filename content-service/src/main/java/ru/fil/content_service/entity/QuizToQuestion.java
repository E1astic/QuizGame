package ru.fil.content_service.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "quizzes_to_questions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class QuizToQuestion {

    @EmbeddedId
    private QuizQuestionId quizQuestionId;

    @MapsId("quizId")
    @ManyToOne
    @JoinColumn(name = "quiz_id", referencedColumnName = "id")
    private Quiz quiz;

    @MapsId("questionId")
    @ManyToOne
    @JoinColumn(name = "question_id", referencedColumnName = "id")
    private Question question;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizToQuestion that = (QuizToQuestion) o;
        return Objects.equals(quizQuestionId, that.quizQuestionId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(quizQuestionId);
    }
}
