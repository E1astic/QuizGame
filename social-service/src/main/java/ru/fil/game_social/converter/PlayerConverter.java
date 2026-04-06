package ru.fil.game_social.converter;

import org.springframework.stereotype.Component;
import ru.fil.game_social.dto.PlayerRegisterRequest;
import ru.fil.game_social.entity.Player;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PlayerConverter {

    public Player mapToPlayer(UUID userId, PlayerRegisterRequest registerRequest) {
        return Player.builder()
                .id(userId)
                .nickname(registerRequest.nickname())
                .gamesPlayed(0)
                .wins(0)
                .rating(new BigDecimal("0.000"))
                .build();
    }
}
