package ru.fil.game_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSessionService {

    private final GameService gameService;
    private final GameTeamRepository gameTeamRepository;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<UUID, List<QuestionAnswerDto>> gameQuestionsCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, String>> correctAnswersCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, UUID>> teamAnswersCache = new ConcurrentHashMap<>(); // gameId -> (teamId -> answerId)
    private final Map<UUID, Set<UUID>> answeredQuestionsCache = new ConcurrentHashMap<>(); // gameId -> set of answered questionIds
    private final Map<UUID, Integer> currentQuestionIndexCache = new ConcurrentHashMap<>(); // gameId -> current question index
    private final Map<UUID, ScheduledFuture<?>> questionTimersCache = new ConcurrentHashMap<>(); // gameId -> timer future
    private final Map<UUID, AtomicInteger> timeLeftCache = new ConcurrentHashMap<>(); // gameId -> time left in seconds
    private static final int QUESTION_TIME_SECONDS = 30;

    public void loadGameQuestions(UUID gameId) {
        Optional<Game> gameOptional = gameService.getGameById(gameId);
        if (gameOptional.isEmpty()) {
            throw new IllegalStateException("Игра не найдена");
        }

        Game game = gameOptional.get();
        Quiz quiz = game.getQuiz();

        List<QuestionAnswerDto> questionsForClient = new ArrayList<>();
        Map<UUID, String> correctAnswersMap = new HashMap<>();

        int totalQuestions = quiz.getQuestions().size();
        int index = 0;
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
                    answerOptions,
                    index + 1,
                    totalQuestions,
                    QUESTION_TIME_SECONDS
            );
            questionsForClient.add(questionDto);
            index++;
        }

        gameQuestionsCache.put(gameId, questionsForClient);
        correctAnswersCache.put(gameId, correctAnswersMap);
        answeredQuestionsCache.put(gameId, ConcurrentHashMap.newKeySet());
        currentQuestionIndexCache.put(gameId, 0);
    }

    public List<QuestionAnswerDto> getQuestionsForGame(UUID gameId) {
        return gameQuestionsCache.get(gameId);
    }

    public QuestionAnswerDto getCurrentQuestion(UUID gameId) {
        List<QuestionAnswerDto> questions = gameQuestionsCache.get(gameId);
        if (questions == null || questions.isEmpty()) {
            return null;
        }
        
        Integer currentIndex = currentQuestionIndexCache.get(gameId);
        if (currentIndex == null || currentIndex >= questions.size()) {
            return null;
        }
        
        return questions.get(currentIndex);
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
        // Cancel any running timer for this game
        ScheduledFuture<?> timer = questionTimersCache.remove(gameId);
        if (timer != null) {
            timer.cancel(false);
        }
        timeLeftCache.remove(gameId);
        gameQuestionsCache.remove(gameId);
        correctAnswersCache.remove(gameId);
        teamAnswersCache.remove(gameId);
        answeredQuestionsCache.remove(gameId);
        currentQuestionIndexCache.remove(gameId);
    }

    public void startGame(UUID gameId) {
        Game game = gameService.getGameById(gameId)
                .orElseThrow(() -> new IllegalStateException("Игра не найдена"));
        
        loadGameQuestions(gameId);
        
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(java.time.OffsetDateTime.now());
        gameService.saveGame(game);
        
        // Start timer for the first question
        startQuestionTimer(gameId);
    }
    
    /**
     * Starts or restarts the timer for the current question
     */
    public void startQuestionTimer(UUID gameId) {
        // Cancel any existing timer
        cancelQuestionTimer(gameId);
        
        // Initialize time left
        timeLeftCache.put(gameId, new AtomicInteger(QUESTION_TIME_SECONDS));
        
        // Schedule periodic task to update time left every second
        ScheduledFuture<?> timer = taskScheduler.scheduleAtFixedRate(() -> {
            AtomicInteger timeLeft = timeLeftCache.get(gameId);
            if (timeLeft != null) {
                int remaining = timeLeft.decrementAndGet();
                
                // Send time update to all clients
                QuestionAnswerDto currentQuestion = getCurrentQuestionWithTime(gameId, remaining);
                if (currentQuestion != null) {
                    try {
                        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/timer", currentQuestion);
                    } catch (Exception e) {
                        log.error("Error sending time update", e);
                    }
                }
                
                // Time's up!
                if (remaining <= 0) {
                    handleTimeUp(gameId);
                }
            }
        }, 1000L);
        
        questionTimersCache.put(gameId, timer);
        log.info("⏱️ Timer started for game {} ({} seconds per question)", gameId, QUESTION_TIME_SECONDS);
    }
    
    /**
     * Cancels the timer for a game
     */
    public void cancelQuestionTimer(UUID gameId) {
        ScheduledFuture<?> timer = questionTimersCache.remove(gameId);
        if (timer != null) {
            timer.cancel(false);
            log.debug("⏹️ Timer cancelled for game {}", gameId);
        }
    }
    
    /**
     * Gets current question with updated time left
     */
    public QuestionAnswerDto getCurrentQuestionWithTime(UUID gameId, int timeLeft) {
        QuestionAnswerDto question = getCurrentQuestion(gameId);
        if (question != null) {
            return new QuestionAnswerDto(
                question.questionId(),
                question.questionText(),
                question.answers(),
                question.questionNumber(),
                question.totalQuestions(),
                timeLeft
            );
        }
        return null;
    }
    
    /**
     * Handles what happens when time runs out for a question
     */
    @Transactional
    public void handleTimeUp(UUID gameId) {
        log.warn("⏰ Time's up for game {}!", gameId);
        
        // Cancel the timer
        cancelQuestionTimer(gameId);
        
        // Move to next question
        Integer currentIndex = currentQuestionIndexCache.get(gameId);
        if (currentIndex == null) {
            currentIndex = 0;
        }
        Integer nextIndex = currentIndex + 1;
        
        List<QuestionAnswerDto> questions = gameQuestionsCache.get(gameId);
        QuestionAnswerDto nextQuestion = null;
        boolean shouldFinishGame = false;
        
        if (questions != null && nextIndex < questions.size()) {
            currentQuestionIndexCache.put(gameId, nextIndex);
            nextQuestion = questions.get(nextIndex);
            // Restart timer for next question
            startQuestionTimer(gameId);
        } else {
            // All questions answered - finish game
            shouldFinishGame = true;
        }
        
        // Create response to notify clients about time up
        GameAnswerResponse response = new GameAnswerResponse(
            gameId,
            null,
            null,
            false,
            "Время вышло!",
            nextQuestion,
            AnswerResultType.INCORRECT
        );
        
        // Send time up notification to all clients
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/answer", response);
        
        if (shouldFinishGame) {
            finishGame(gameId);
        }
    }

    public void finishGame(UUID gameId) {
        Game game = gameService.getGameById(gameId)
                .orElseThrow(() -> new IllegalStateException("Игра не найдена"));
        
        game.setStatus(GameStatus.FINISHED);
        game.setFinishedAt(java.time.OffsetDateTime.now());
        gameService.saveGame(game);
        
        clearGameSession(gameId);
    }

    public enum AnswerResultType {
        CORRECT,
        INCORRECT,
        ALREADY_ANSWERED
    }

    @Transactional
    public GameAnswerResponse submitAnswer(GameAnswerRequest request) {
        Optional<Game> gameOptional = gameService.getGameById(request.gameId());
        if (gameOptional.isEmpty()) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Игра не найдена", null, AnswerResultType.INCORRECT);
        }

        Game game = gameOptional.get();
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Игра не активна", null, AnswerResultType.INCORRECT);
        }

        Optional<GameTeam> gameTeamOptional = gameTeamRepository.findByGameIdAndTeamId(request.gameId(), request.teamId());
        if (gameTeamOptional.isEmpty()) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Команда не участвует в игре", null, AnswerResultType.INCORRECT);
        }

        // Check if this question was already answered by this team
        Map<UUID, UUID> teamAnswers = teamAnswersCache.computeIfAbsent(request.gameId(), k -> new ConcurrentHashMap<>());
        
        if (teamAnswers.containsKey(request.teamId())) {
            UUID lastAnsweredQuestionId = teamAnswers.get(request.teamId());
            if (lastAnsweredQuestionId != null && lastAnsweredQuestionId.equals(request.questionId())) {
                return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Команда уже ответила на этот вопрос", null, AnswerResultType.ALREADY_ANSWERED);
            }
        }

        boolean isCorrect = validateAnswer(request.gameId(), request.questionId(), request.answerId());
        
        teamAnswers.put(request.teamId(), request.questionId());
        
        // Mark question as answered for this game
        Set<UUID> answeredQuestions = answeredQuestionsCache.computeIfAbsent(request.gameId(), k -> ConcurrentHashMap.newKeySet());
        answeredQuestions.add(request.questionId());

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

        // Move to next question if answer is correct
        QuestionAnswerDto nextQuestion = null;
        boolean shouldFinishGame = false;
        
        if (isCorrect) {
            Integer currentIndex = currentQuestionIndexCache.get(request.gameId());
            if (currentIndex == null) {
                currentIndex = 0;
            }
            Integer nextIndex = currentIndex + 1;
            
            List<QuestionAnswerDto> questions = gameQuestionsCache.get(request.gameId());
            if (questions != null && nextIndex < questions.size()) {
                currentQuestionIndexCache.put(request.gameId(), nextIndex);
                nextQuestion = questions.get(nextIndex);
            } else {
                // All questions answered - mark for finishing game
                shouldFinishGame = true;
            }
        } else {
            // Return current question again if answer was wrong
            nextQuestion = getCurrentQuestion(request.gameId());
        }

        AnswerResultType resultType = isCorrect ? AnswerResultType.CORRECT : AnswerResultType.INCORRECT;
        GameAnswerResponse response = new GameAnswerResponse(
                request.gameId(), 
                request.questionId(), 
                request.teamId(), 
                isCorrect, 
                isCorrect ? "Правильный ответ" : "Неправильный ответ",
                nextQuestion,
                resultType
        );
        
        if (isCorrect) {
            // Restart timer for next question on correct answer
            startQuestionTimer(request.gameId());
        }
        
        if (shouldFinishGame) {
            finishGame(request.gameId());
        }
        
        return response;
    }
}
