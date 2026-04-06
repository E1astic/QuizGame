package ru.fil.game_social.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.fil.game_social.converter.PlayerConverter;
import ru.fil.game_social.dto.PlayerRegisterRequest;
import ru.fil.game_social.entity.Player;
import ru.fil.game_social.repository.PlayerRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerConverter playerConverter;

    public Optional<Player> register(UUID userId, PlayerRegisterRequest registerRequest) {
        Player existingPlayer = playerRepository.findById(userId).orElse(null);
        if (existingPlayer != null) {
            return Optional.empty();
        }
        Player player = playerConverter.mapToPlayer(userId, registerRequest);
        return Optional.of(playerRepository.save(player));
    }

}
