package ru.fil.game_social.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.fil.game_social.dto.TeamInviteRequest;
import ru.fil.game_social.entity.PlayerToTeam;
import ru.fil.game_social.repository.PlayerTeamRepository;
import ru.fil.game_social.repository.TeamInvitationRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamInvitationService {

    private final TeamInvitationRepository teamInvitationRepository;
    private final PlayerTeamRepository playerTeamRepository;

    @Transactional
    public Optional<Integer> createInvitation(UUID senderId, TeamInviteRequest inviteRequest) {
        Optional<PlayerToTeam> senderTeam = playerTeamRepository.findByPlayerId(senderId);
        if (senderTeam.isEmpty()) {
            log.info("Sender {} can't create invitation because he hasn't team", senderId);
            return Optional.empty();
        }
        if (!senderTeam.get().getTeam().getId().equals(inviteRequest.teamId())) {
            log.info("Sender {} can't create invitation because he in other team", senderId);
            return Optional.empty();
        }
        if (!senderTeam.get().getIsCapitan()) {
            log.info("Sender {} can't create invitation because he is not captain", senderId);
            return Optional.empty();
        }

        int res = teamInvitationRepository.saveNative(
                senderId, inviteRequest.recipientId(), inviteRequest.teamId(), OffsetDateTime.now());
        return res == 0 ? Optional.empty() : Optional.of(res);
    }
}
