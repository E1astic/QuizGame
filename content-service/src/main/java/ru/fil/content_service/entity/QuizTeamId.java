package ru.fil.content_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class QuizTeamId implements Serializable {

    @Column(name = "quiz_id")
    private UUID quizId;

    @Column(name = "team_id")
    private UUID teamId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizTeamId that = (QuizTeamId) o;
        return Objects.equals(quizId, that.quizId) && Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quizId, teamId);
    }
}
