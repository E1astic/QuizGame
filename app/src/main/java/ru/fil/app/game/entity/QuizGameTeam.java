package ru.fil.app.game.entity;

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

import java.util.Objects;

@Entity
@Table(name = "quiz_game_teams")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizGameTeam {

    @EmbeddedId
    private QuizGameTeamId quizGameTeamId;

    @MapsId("gameId")
    @ManyToOne
    @JoinColumn(name = "game_id", referencedColumnName = "id")
    private QuizGame game;

    @MapsId("teamId")
    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private Team team;

    @Column(name = "score", nullable = false)
    @Builder.Default
    private Integer score = 0;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizGameTeam that = (QuizGameTeam) o;
        return Objects.equals(quizGameTeamId, that.quizGameTeamId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(quizGameTeamId);
    }
}
