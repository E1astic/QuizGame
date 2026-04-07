package ru.fil.game_social.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.fil.game_social.converter.TeamConverter;
import ru.fil.game_social.dto.TeamCreateRequest;
import ru.fil.game_social.dto.TeamDeleteRequest;
import ru.fil.game_social.dto.TeamJoinRequest;
import ru.fil.game_social.entity.PlayerToTeam;
import ru.fil.game_social.entity.Team;
import ru.fil.game_social.entity.TeamInvitation;
import ru.fil.game_social.repository.PlayerTeamRepository;
import ru.fil.game_social.repository.TeamInvitationRepository;
import ru.fil.game_social.repository.TeamRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamConverter teamConverter;
    private final PlayerTeamService playerTeamService;
    private final PlayerTeamRepository playerTeamRepository;
    private final TeamInvitationRepository teamInvitationRepository;

    @Transactional
    public Optional<Team> createTeam(UUID userId, TeamCreateRequest createRequest) {
        Optional<Team> existingTeamByName = teamRepository.findByName(createRequest.name());
        if (existingTeamByName.isPresent()) {
            log.info("Team with name {} already exists", createRequest.name());
            return Optional.empty();
        }
        Optional<PlayerToTeam> existingTeamForPlayer = playerTeamRepository.findByPlayerId(userId);
        if (existingTeamForPlayer.isPresent()) {
            log.info("Player {} already in team", existingTeamForPlayer.get().getPlayer().getNickname());
            return Optional.empty();
        }

        Team team = teamConverter.mapToTeam(createRequest);
        team = teamRepository.save(team);
        playerTeamService.savePlayerToTeam(userId, team.getId(), true)
                .orElseThrow(() -> new RuntimeException("Ошибка при создании связки игрок-команда"));
        return Optional.of(team);
    }

    @Transactional
    public boolean deleteTeam(UUID userId, TeamDeleteRequest deleteRequest) {
        Optional<PlayerToTeam> playerToTeam = playerTeamRepository.findByPlayerIdAndTeamId(userId, deleteRequest.teamId());
        if (playerToTeam.isEmpty()) {
            log.info("Player {} not a member of team {}", userId, deleteRequest.teamId());
            return false;
        }
        if (!playerToTeam.get().getIsCapitan()) {
            log.info("Player {} not a captain of team {}", userId, deleteRequest.teamId());
            return false;
        }
        teamRepository.deleteById(deleteRequest.teamId());
        return true;
    }

    @Transactional
    public Optional<Integer> joinToTeam(UUID userId, TeamJoinRequest joinRequest) {
        Optional<TeamInvitation> invitation = teamInvitationRepository.findByRecipientIdAndTeamId(userId, joinRequest.teamId());
        if (invitation.isEmpty()) {
            log.info("Player {} hasn't invitation to team {}", userId, joinRequest.teamId());
            return Optional.empty();
        }

        Optional<PlayerToTeam> existingTeamForPlayer = playerTeamRepository.findByPlayerId(userId);
        if (existingTeamForPlayer.isPresent()) {
            log.info("Player {} already in team", existingTeamForPlayer.get().getPlayer().getNickname());
            return Optional.empty();
        }

        int res = playerTeamService.savePlayerToTeam(userId, joinRequest.teamId(), false)
                .orElseThrow(() -> new RuntimeException("Ошибка при создании связки игрок-команда"));
        teamInvitationRepository.delete(invitation.get());
        return Optional.of(res);
    }

    /**
     * Обновляет статистику команды после завершения игры
     * @param teamId ID команды
     * @param correctAnswers количество верных ответов в игре
     * @param totalQuestions общее количество вопросов в квизе
     * @param isWinner true если команда победила
     */
    @Transactional
    public void updateTeamStats(UUID teamId, int correctAnswers, int totalQuestions, boolean isWinner) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Команда не найдена: " + teamId));
        
        // Увеличиваем games_played на 1
        int gamesPlayed = team.getGamesPlayed() + 1;
        team.setGamesPlayed(gamesPlayed);
        
        // Если команда победила, увеличиваем wins на 1
        if (isWinner) {
            team.setWins(team.getWins() + 1);
        }
        
        // Вычисляем новый рейтинг: (correctAnswers / totalQuestions + currentRating) / gamesPlayed
        BigDecimal correctRatio = totalQuestions > 0 
                ? BigDecimal.valueOf(correctAnswers).divide(BigDecimal.valueOf(totalQuestions), 10, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal newRating = correctRatio.add(team.getRating())
                .divide(BigDecimal.valueOf(gamesPlayed), 10, BigDecimal.ROUND_HALF_UP);
        team.setRating(newRating);
        
        teamRepository.save(team);
    }
}
