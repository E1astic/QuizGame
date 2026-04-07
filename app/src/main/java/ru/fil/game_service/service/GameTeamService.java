package ru.fil.game_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fil.game_social.entity.Team;
import ru.fil.game_social.repository.TeamRepository;
import ru.fil.game_service.dto.GameJoinRequest;
import ru.fil.game_service.dto.GameJoinResponse;
import ru.fil.game_service.entity.Game;
import ru.fil.game_service.entity.GameStatus;
import ru.fil.game_service.entity.GameTeam;
import ru.fil.game_service.entity.GameTeamId;
import ru.fil.game_service.repository.GameRepository;
import ru.fil.game_service.repository.GameTeamRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameTeamService {

    private final GameRepository gameRepository;
    private final GameTeamRepository gameTeamRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public GameJoinResponse joinGame(GameJoinRequest request) {
        Optional<Game> gameOptional = gameRepository.findByIdAndStatus(request.gameId(), GameStatus.WAITING);
        if (gameOptional.isEmpty()) {
            return new GameJoinResponse(request.gameId(), request.teamId(), false, "Игра не найдена или уже началась");
        }

        Game game = gameOptional.get();

        Optional<Team> teamOptional = teamRepository.findById(request.teamId());
        if (teamOptional.isEmpty()) {
            return new GameJoinResponse(request.gameId(), request.teamId(), false, "Команда не найдена");
        }

        Team team = teamOptional.get();

        Optional<GameTeam> existingGameTeam = gameTeamRepository.findByGameIdAndTeamId(request.gameId(), request.teamId());
        if (existingGameTeam.isPresent()) {
            return new GameJoinResponse(request.gameId(), request.teamId(), false, "Команда уже присоединилась к игре");
        }

        GameTeamId gameTeamId = new GameTeamId(request.gameId(), request.teamId());
        GameTeam gameTeam = GameTeam.builder()
                .gameTeamId(gameTeamId)
                .game(game)
                .team(team)
                .score(0)
                .build();

        gameTeamRepository.save(gameTeam);

        return new GameJoinResponse(request.gameId(), request.teamId(), true, "Команда успешно присоединилась к игре");
    }
}
