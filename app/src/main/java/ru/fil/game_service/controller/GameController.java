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
            gameSessionService.startGame(gameId);
            List<QuestionAnswerDto> questions = gameSessionService.getQuestionsForGame(gameId);
            
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/started", 
                new GameStartResponse(gameId, true, "Игра началась"));
            
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/questions", questions);
            
            return ResponseEntity.ok(new GameStartResponse(gameId, true, "Игра успешно началась"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new GameStartResponse(gameId, false, e.getMessage()));
        }
    }

    @MessageMapping("/game/answer")
    public ResponseEntity<GameAnswerResponse> submitAnswer(@Payload GameAnswerRequest request) {
        GameAnswerResponse response = gameSessionService.submitAnswer(request);
        
        // Send answer response only to the team that answered (using queue)
        messagingTemplate.convertAndSendToUser(
            request.teamId().toString(), 
            "/queue/answer", 
            response
        );
        
        // If the answer was correct, broadcast the question transition to all teams
        if (response.correct()) {
            QuestionTransitionDto transition = gameSessionService.createQuestionTransition(
                request.gameId(), 
                request.teamId()
            );
            if (transition != null) {
                String message = transition.correctTeamName() != null 
                    ? "Команда \"" + transition.correctTeamName() + "\" ответила правильно! Переходим к вопросу " + transition.questionNumber() 
                    : "Переходим к вопросу " + transition.questionNumber();
                
                messagingTemplate.convertAndSend(
                    "/topic/game/" + request.gameId() + "/transition",
                    transition
                );
            }
        } else {
            // Check if all teams have answered - if so, broadcast transition without correct team
            QuestionTransitionDto transition = gameSessionService.createQuestionTransition(
                request.gameId(), 
                null
            );
            if (transition != null && transition.questionNumber() != null) {
                // This means we're moving to next question because all teams answered incorrectly
                messagingTemplate.convertAndSend(
                    "/topic/game/" + request.gameId() + "/transition",
                    transition
                );
            }
        }
        
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
