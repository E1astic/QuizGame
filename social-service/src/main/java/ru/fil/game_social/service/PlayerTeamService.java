package ru.fil.game_social.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.fil.game_social.repository.PlayerTeamRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerTeamService {

    private final PlayerTeamRepository playerTeamRepository;

    @Transactional
    public Optional<Integer> savePlayerToTeam(UUID playerId, UUID teamId, boolean isCapitan) {
        int res = playerTeamRepository.saveNative(playerId, teamId, isCapitan, OffsetDateTime.now());
        if (res == 0) {
            return Optional.empty();
        }
        return Optional.of(res);
    }
}
