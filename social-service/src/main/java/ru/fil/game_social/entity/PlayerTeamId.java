package ru.fil.game_social.entity;

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
public class PlayerTeamId implements Serializable {

    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "team_id")
    private UUID teamId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerTeamId that = (PlayerTeamId) o;
        return Objects.equals(playerId, that.playerId) && Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, teamId);
    }
}
