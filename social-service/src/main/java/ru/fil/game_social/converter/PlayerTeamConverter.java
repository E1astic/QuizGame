package ru.fil.game_social.converter;

import org.springframework.stereotype.Component;
import ru.fil.game_social.entity.Player;
import ru.fil.game_social.entity.PlayerToTeam;
import ru.fil.game_social.entity.Team;

import java.time.OffsetDateTime;

@Component
public class PlayerTeamConverter {

    public PlayerToTeam mapToPlayerTeam(Player player, Team team) {
        return PlayerToTeam.builder()
                .player(player)
                .team(team)
                .isCapitan(false)
                .joinedAt(OffsetDateTime.now())
                .build();
    }
}
