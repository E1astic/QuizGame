package ru.fil.game_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.fil.game_service.dto.*;
import ru.fil.game_service.entity.Game;
import ru.fil.game_service.service.GameService;
import ru.fil.game_service.service.GameSessionService;
import ru.fil.game_service.service.GameTeamService;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;
    private final GameTeamService gameTeamService;
    private final GameSessionService gameSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/create")
    public ResponseEntity<GameCreateResponse> createGame(@Payload GameCreateRequest request) {
        return gameService.createGame(request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    @MessageMapping("/game/join")
    public ResponseEntity<GameJoinResponse> joinGame(@Payload GameJoinRequest request) {
        GameJoinResponse response = gameTeamService.joinGame(request);
        if (response.success()) {
            messagingTemplate.convertAndSend("/topic/game/" + request.gameId() + "/teams", 
                "Команда " + request.teamId() + " присоединилась к игре");
        }
        return ResponseEntity.ok(response);
    }

    @MessageMapping("/game/start")
    public ResponseEntity<GameStartResponse> startGame(@Payload UUID gameId) {
        try {
            log.info("🚀 Запуск игры: {}", gameId);
            gameSessionService.startGame(gameId);
            List<QuestionAnswerDto> questions = gameSessionService.getQuestionsForGame(gameId);
            
            log.info("📤 Отправка сообщения о старте игры в /topic/game/{}/started", gameId);
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/started", 
                new GameStartResponse(gameId, true, "Игра началась"));
            
            log.info("📤 Отправка {} вопросов в /topic/game/{}/questions", questions != null ? questions.size() : 0, gameId);
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/questions", questions);
            
            // Send initial question transition to show first question
            QuestionTransitionDto firstTransition = gameSessionService.createQuestionTransition(gameId, null);
            if (firstTransition != null) {
                log.info("📤 Отправка первого вопроса в /topic/game/{}/transition", gameId);
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/transition", firstTransition);
            }
            
            return ResponseEntity.ok(new GameStartResponse(gameId, true, "Игра успешно началась"));
        } catch (IllegalStateException e) {
            log.error("❌ Ошибка при запуске игры: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GameStartResponse(gameId, false, e.getMessage()));
        }
    }

    @MessageMapping("/game/answer")
    public ResponseEntity<GameAnswerResponse> submitAnswer(@Payload GameAnswerRequest request) {
        log.info("📥 Controller: Received answer from team {} for question {} in game {}", 
                request.teamId(), request.questionId(), request.gameId());
        
        GameAnswerResponse response = gameSessionService.submitAnswer(request);
        
        // Send answer response only to the team that answered (using queue)
        messagingTemplate.convertAndSendToUser(
            request.teamId().toString(), 
            "/queue/answer", 
            response
        );
        log.info("📤 Controller: Sent answer response to team /queue/answer");
        
        // Transition is already sent by moveToNextQuestion in service via callback
        // No need to send it again here - the service handles all transition logic
        
        return ResponseEntity.ok(response);
    }

    @MessageMapping("/game/finish")
    public ResponseEntity<GameStartResponse> finishGame(@Payload UUID gameId) {
        try {
            gameSessionService.finishGame(gameId);
            
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/finished", 
                new GameStartResponse(gameId, true, "Игра завершена"));
            
            return ResponseEntity.ok(new GameStartResponse(gameId, true, "Игра успешно завершена"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new GameStartResponse(gameId, false, e.getMessage()));
        }
    }

    @MessageMapping("/game/questions")
    public void getQuestions(@Payload UUID gameId) {
        try {
            List<QuestionAnswerDto> questions = gameSessionService.getQuestionsForGame(gameId);
            if (questions == null || questions.isEmpty()) {
                log.warn("⚠️ Вопросы не найдены для игры: {}", gameId);
                return;
            }

            String destination = "/topic/game/" + gameId + "/questions";
            log.info("📤 Sending {} questions to {}", questions.size(), destination);

            messagingTemplate.convertAndSend(destination, questions);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid gameId format: {}", gameId, e);
        } catch (Exception e) {
            log.error("❌ Unexpected error in getQuestions", e);
        }
    }
}
