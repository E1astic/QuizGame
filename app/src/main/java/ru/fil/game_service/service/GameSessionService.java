package ru.fil.game_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fil.content_service.entity.Answer;
import ru.fil.content_service.entity.Question;
import ru.fil.content_service.entity.Quiz;
import ru.fil.content_service.entity.QuizToQuestion;
import ru.fil.game_service.dto.GameAnswerRequest;
import ru.fil.game_service.dto.GameAnswerResponse;
import ru.fil.game_service.dto.QuestionAnswerDto;
import ru.fil.game_service.entity.Game;
import ru.fil.game_service.entity.GameStatus;
import ru.fil.game_service.entity.GameTeam;
import ru.fil.game_service.repository.GameTeamRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final GameService gameService;
    private final GameTeamRepository gameTeamRepository;

    private final Map<UUID, List<QuestionAnswerDto>> gameQuestionsCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, String>> correctAnswersCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, UUID>> teamAnswersCache = new ConcurrentHashMap<>(); // gameId -> (teamId -> answerId)

    public void loadGameQuestions(UUID gameId) {
        Optional<Game> gameOptional = gameService.getGameById(gameId);
        if (gameOptional.isEmpty()) {
            throw new IllegalStateException("Игра не найдена");
        }

        Game game = gameOptional.get();
        Quiz quiz = game.getQuiz();

        List<QuestionAnswerDto> questionsForClient = new ArrayList<>();
        Map<UUID, String> correctAnswersMap = new HashMap<>();

        for (QuizToQuestion quizToQuestion : quiz.getQuestions()) {
            Question question = quizToQuestion.getQuestion();
            
            List<QuestionAnswerDto.AnswerOptionDto> answerOptions = new ArrayList<>();
            
            for (Answer answer : question.getAnswers()) {
                answerOptions.add(new QuestionAnswerDto.AnswerOptionDto(
                        answer.getId(),
                        answer.getName()
                ));
                
                if (answer.getIsCorrect()) {
                    correctAnswersMap.put(answer.getId(), question.getId().toString());
                }
            }

            Collections.shuffle(answerOptions);

            QuestionAnswerDto questionDto = new QuestionAnswerDto(
                    question.getId(),
                    question.getName(),
                    answerOptions
            );
            questionsForClient.add(questionDto);
        }

        gameQuestionsCache.put(gameId, questionsForClient);
        correctAnswersCache.put(gameId, correctAnswersMap);
    }

    public List<QuestionAnswerDto> getQuestionsForGame(UUID gameId) {
        return gameQuestionsCache.get(gameId);
    }

    public boolean validateAnswer(UUID gameId, UUID questionId, UUID answerId) {
        Map<UUID, String> correctAnswers = correctAnswersCache.get(gameId);
        if (correctAnswers == null) {
            return false;
        }
        
        String correctQuestionId = correctAnswers.get(answerId);
        return questionId.toString().equals(correctQuestionId);
    }

    public void clearGameSession(UUID gameId) {
        gameQuestionsCache.remove(gameId);
        correctAnswersCache.remove(gameId);
        teamAnswersCache.remove(gameId);
    }

    public void startGame(UUID gameId) {
        Game game = gameService.getGameById(gameId)
                .orElseThrow(() -> new IllegalStateException("Игра не найдена"));
        
        loadGameQuestions(gameId);
        
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(java.time.OffsetDateTime.now());
        gameService.saveGame(game);
    }

    public void finishGame(UUID gameId) {
        Game game = gameService.getGameById(gameId)
                .orElseThrow(() -> new IllegalStateException("Игра не найдена"));
        
        game.setStatus(GameStatus.FINISHED);
        game.setFinishedAt(java.time.OffsetDateTime.now());
        gameService.saveGame(game);
        
        clearGameSession(gameId);
    }

    @Transactional
    public GameAnswerResponse submitAnswer(GameAnswerRequest request) {
        Optional<Game> gameOptional = gameService.getGameById(request.gameId());
        if (gameOptional.isEmpty()) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Игра не найдена");
        }

        Game game = gameOptional.get();
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Игра не активна");
        }

        Optional<GameTeam> gameTeamOptional = gameTeamRepository.findByGameIdAndTeamId(request.gameId(), request.teamId());
        if (gameTeamOptional.isEmpty()) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Команда не участвует в игре");
        }

        Map<UUID, UUID> teamAnswers = teamAnswersCache.computeIfAbsent(request.gameId(), k -> new ConcurrentHashMap<>());
        
        if (teamAnswers.containsKey(request.teamId())) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Команда уже ответила на этот вопрос");
        }

        boolean isCorrect = validateAnswer(request.gameId(), request.questionId(), request.answerId());
        
        teamAnswers.put(request.teamId(), request.answerId());

        if (isCorrect) {
            GameTeam gameTeam = gameTeamOptional.get();
            Integer currentScore = gameTeam.getScore();
            
            List<GameTeam> gameTeams = new ArrayList<>(game.getGameTeams());
            int teamIndex = -1;
            for (int i = 0; i < gameTeams.size(); i++) {
                if (gameTeams.get(i).getGameTeamId().getTeamId().equals(request.teamId())) {
                    teamIndex = i;
                    break;
                }
            }
            
            if (teamIndex >= 0) {
                GameTeam teamToUpdate = gameTeams.get(teamIndex);
                teamToUpdate.setScore(currentScore + 1);
            }
        }

        return new GameAnswerResponse(
                request.gameId(), 
                request.questionId(), 
                request.teamId(), 
                isCorrect, 
                isCorrect ? "Правильный ответ" : "Неправильный ответ"
        );
    }
}
