package ru.fil.game_social.entity;

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
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "players_to_teams")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PlayerToTeam {

    @EmbeddedId
    private PlayerTeamId playerTeamId;

    @MapsId("playerId")
    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "user_id")
    private Player player;

    @MapsId("teamId")
    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private Team team;

    @Column(name = "is_capitan", nullable = false)
    private Boolean isCapitan;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerToTeam that = (PlayerToTeam) o;
        return Objects.equals(playerTeamId, that.playerTeamId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(playerTeamId);
    }
}
