package ru.fil.app.game.entity;

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
public class QuizGameTeamId implements Serializable {

    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "team_id")
    private UUID teamId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizGameTeamId that = (QuizGameTeamId) o;
        return Objects.equals(gameId, that.gameId) && Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, teamId);
    }
}
