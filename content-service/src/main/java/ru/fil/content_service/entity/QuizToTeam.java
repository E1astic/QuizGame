package ru.fil.content_service.entity;

import jakarta.persistence.Column;
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
import ru.fil.game_social.entity.Team;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "quizzes_to_teams")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class QuizToTeam {

    @EmbeddedId
    private QuizTeamId quizTeamId;

    @MapsId("quizId")
    @ManyToOne
    @JoinColumn(name = "quiz_id", referencedColumnName = "id")
    private Quiz quiz;

    @MapsId("teamId")
    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private Team team;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizToTeam that = (QuizToTeam) o;
        return Objects.equals(quizTeamId, that.quizTeamId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(quizTeamId);
    }
}
