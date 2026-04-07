package ru.fil.game_social.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.fil.game_social.converter.PlayerConverter;
import ru.fil.game_social.dto.PlayerRegisterRequest;
import ru.fil.game_social.entity.Player;
import ru.fil.game_social.repository.PlayerRepository;

import java.math.BigDecimal;
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

    /**
     * Обновляет статистику игрока после завершения игры
     * @param playerId ID игрока
     * @param correctAnswers количество верных ответов в игре
     * @param totalQuestions общее количество вопросов в квизе
     * @param isWinner true если команда игрока победила
     */
    public void updatePlayerStats(UUID playerId, int correctAnswers, int totalQuestions, boolean isWinner) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Игрок не найден: " + playerId));
        
        // Увеличиваем games_played на 1
        int gamesPlayed = player.getGamesPlayed() + 1;
        player.setGamesPlayed(gamesPlayed);
        
        // Если команда победила, увеличиваем wins на 1
        if (isWinner) {
            player.setWins(player.getWins() + 1);
        }
        
        // Вычисляем новый рейтинг: (correctAnswers / totalQuestions + currentRating) / gamesPlayed
        BigDecimal correctRatio = totalQuestions > 0 
                ? BigDecimal.valueOf(correctAnswers).divide(BigDecimal.valueOf(totalQuestions), 10, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal newRating = correctRatio.add(player.getRating())
                .divide(BigDecimal.valueOf(gamesPlayed), 10, BigDecimal.ROUND_HALF_UP);
        player.setRating(newRating);
        
        playerRepository.save(player);
    }
}
