package ru.fil.game_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "games_to_teams")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Setter
public class GameTeam {

    @EmbeddedId
    private GameTeamId gameTeamId;

    @MapsId("gameId")
    @ManyToOne
    @JoinColumn(name = "game_id", referencedColumnName = "id")
    private Game game;

    @MapsId("teamId")
    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private ru.fil.game_social.entity.Team team;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameTeam that = (GameTeam) o;
        return Objects.equals(gameTeamId, that.gameTeamId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(gameTeamId);
    }
}
