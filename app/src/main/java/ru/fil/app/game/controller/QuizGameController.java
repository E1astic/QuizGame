package ru.fil.app.game.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import ru.fil.app.game.dto.AnswerSubmitRequest;
import ru.fil.app.game.dto.GameCreateRequest;
import ru.fil.app.game.dto.GameStartRequest;
import ru.fil.app.game.dto.TeamRegisterRequest;
import ru.fil.app.game.entity.QuizGame;
import ru.fil.app.game.service.QuizGameService;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class QuizGameController {

    private final QuizGameService quizGameService;

    /**
     * Регистрация команды в игре
     * POST /app/game/register
     */
    @MessageMapping("/game/register")
    public ResponseEntity<Void> registerTeam(@Payload TeamRegisterRequest request) {
        quizGameService.registerTeam(request.getGameId(), request.getTeamId());
        return ResponseEntity.ok().build();
    }

    /**
     * Старт игры
     * POST /app/game/start
     */
    @MessageMapping("/game/start")
    public ResponseEntity<Void> startGame(@Payload GameStartRequest request) {
        quizGameService.startGame(request.getGameId());
        return ResponseEntity.ok().build();
    }

    /**
     * Отправка ответа на вопрос
     * POST /app/game/answer
     */
    @MessageMapping("/game/answer")
    public ResponseEntity<Void> submitAnswer(@Payload AnswerSubmitRequest request) {
        quizGameService.submitAnswer(request);
        return ResponseEntity.ok().build();
    }

    /**
     * REST endpoint для создания новой игры
     */
    @PostMapping("/api/game/create")
    public ResponseEntity<QuizGame> createGame(@RequestBody GameCreateRequest request) {
        QuizGame game = quizGameService.createGame(request.getQuizId());
        return ResponseEntity.ok(game);
    }

    /**
     * REST endpoint для регистрации команды (альтернатива WebSocket)
     */
    @PostMapping("/api/game/{gameId}/register")
    public ResponseEntity<Void> registerTeamRest(
            @PathVariable UUID gameId,
            @RequestParam UUID teamId) {
        quizGameService.registerTeam(gameId, teamId);
        return ResponseEntity.ok().build();
    }

    /**
     * REST endpoint для старта игры (альтернатива WebSocket)
     */
    @PostMapping("/api/game/{gameId}/start")
    public ResponseEntity<Void> startGameRest(
            @PathVariable UUID gameId) {
        quizGameService.startGame(gameId);
        return ResponseEntity.ok().build();
    }

    /**
     * REST endpoint для отправки ответа (альтернатива WebSocket)
     */
    @PostMapping("/api/game/answer")
    public ResponseEntity<Void> submitAnswerRest(@RequestBody AnswerSubmitRequest request) {
        quizGameService.submitAnswer(request);
        return ResponseEntity.ok().build();
    }

    /**
     * REST endpoint для получения текущих счетов
     */
    @GetMapping("/api/game/{gameId}/scores")
    public ResponseEntity<Map<UUID, Integer>> getScores(
            @PathVariable UUID gameId) {
        return ResponseEntity.ok(quizGameService.getGameScores(gameId));
    }
}
