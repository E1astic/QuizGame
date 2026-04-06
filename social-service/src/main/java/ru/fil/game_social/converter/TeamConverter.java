package ru.fil.game_social.converter;

import org.springframework.stereotype.Component;
import ru.fil.game_social.dto.TeamCreateRequest;
import ru.fil.game_social.entity.Team;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
public class TeamConverter {

    public Team mapToTeam(TeamCreateRequest createRequest) {
        return Team.builder()
                .name(createRequest.name())
                .maxSize(5)
                .gamesPlayed(0)
                .wins(0)
                .rating(new BigDecimal("0.0"))
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
