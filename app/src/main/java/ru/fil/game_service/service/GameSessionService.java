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
import ru.fil.game_service.dto.QuestionTransitionDto;
import ru.fil.game_service.entity.Game;
import ru.fil.game_service.entity.GameStatus;
import ru.fil.game_service.entity.GameTeam;
import ru.fil.game_service.repository.GameTeamRepository;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final GameService gameService;
    private final GameTeamRepository gameTeamRepository;

    private final Map<UUID, List<QuestionAnswerDto>> gameQuestionsCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, String>> correctAnswersCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, UUID>> teamAnswersCache = new ConcurrentHashMap<>(); // gameId -> (teamId -> answerId)
    private final Map<UUID, Set<UUID>> answeredQuestionsCache = new ConcurrentHashMap<>(); // gameId -> set of answered questionIds
    private final Map<UUID, Integer> currentQuestionIndexCache = new ConcurrentHashMap<>(); // gameId -> current question index
    private final Map<UUID, Set<UUID>> teamsAnsweredForCurrentQuestion = new ConcurrentHashMap<>(); // gameId -> set of teamIds that answered current question
    private final Map<UUID, OffsetDateTime> questionStartTimeCache = new ConcurrentHashMap<>(); // gameId -> when current question started
    
    private static final int QUESTION_TIME_LIMIT_SECONDS = 30;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    
    // Callback for sending transition messages - will be set by controller
    private Runnable questionTransitionCallback;
    
    public void setQuestionTransitionCallback(Runnable callback) {
        this.questionTransitionCallback = callback;
    }

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
                    totalQuestions
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
        gameQuestionsCache.remove(gameId);
        correctAnswersCache.remove(gameId);
        teamAnswersCache.remove(gameId);
        answeredQuestionsCache.remove(gameId);
        currentQuestionIndexCache.remove(gameId);
        teamsAnsweredForCurrentQuestion.remove(gameId);
        questionStartTimeCache.remove(gameId);
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
    
    private void startQuestionTimer(UUID gameId) {
        questionStartTimeCache.put(gameId, OffsetDateTime.now());
        
        scheduler.schedule(() -> {
            try {
                QuestionAnswerDto currentQuestion = getCurrentQuestion(gameId);
                if (currentQuestion != null) {
                    // Time's up - move to next question
                    moveToNextQuestion(gameId, null);
                }
            } catch (Exception e) {
                // Ignore errors in scheduled task
            }
        }, QUESTION_TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
    }
    
    private void moveToNextQuestion(UUID gameId, UUID correctTeamId) {
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
            // Clear teams that answered for the new question
            teamsAnsweredForCurrentQuestion.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet());
            // Start timer for the new question
            startQuestionTimer(gameId);
        } else {
            // All questions answered - finish game
            shouldFinishGame = true;
        }
        
        // Clear answered teams for this question
        Set<UUID> answeredTeams = teamsAnsweredForCurrentQuestion.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet());
        answeredTeams.clear();
        
        if (shouldFinishGame) {
            finishGame(gameId);
        }
        
        // Notify about question transition (this will be handled by controller via callback)
        if (questionTransitionCallback != null) {
            questionTransitionCallback.run();
        }
    }
    
    public QuestionTransitionDto createQuestionTransition(UUID gameId, UUID correctTeamId) {
        QuestionAnswerDto currentQuestion = getCurrentQuestion(gameId);
        if (currentQuestion == null) {
            return null;
        }
        
        String correctTeamName = null;
        if (correctTeamId != null) {
            Optional<Game> gameOptional = gameService.getGameById(gameId);
            if (gameOptional.isPresent()) {
                for (GameTeam gameTeam : gameOptional.get().getGameTeams()) {
                    if (gameTeam.getGameTeamId().getTeamId().equals(correctTeamId)) {
                        correctTeamName = gameTeam.getTeam().getName();
                        break;
                    }
                }
            }
        }
        
        return new QuestionTransitionDto(
                gameId,
                currentQuestion.questionNumber(),
                currentQuestion.totalQuestions(),
                currentQuestion.questionText(),
                correctTeamId,
                correctTeamName
        );
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
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Игра не найдена", null);
        }

        Game game = gameOptional.get();
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Игра не активна", null);
        }

        Optional<GameTeam> gameTeamOptional = gameTeamRepository.findByGameIdAndTeamId(request.gameId(), request.teamId());
        if (gameTeamOptional.isEmpty()) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Команда не участвует в игре", null);
        }

        // Check if this team already answered the current question
        Set<UUID> answeredTeams = teamsAnsweredForCurrentQuestion.computeIfAbsent(request.gameId(), k -> ConcurrentHashMap.newKeySet());
        
        if (answeredTeams.contains(request.teamId())) {
            return new GameAnswerResponse(request.gameId(), request.questionId(), request.teamId(), false, "Команда уже ответила на этот вопрос", null);
        }

        boolean isCorrect = validateAnswer(request.gameId(), request.questionId(), request.answerId());
        
        // Mark this team as having answered
        answeredTeams.add(request.teamId());
        
        // Store team's answer
        Map<UUID, UUID> teamAnswers = teamAnswersCache.computeIfAbsent(request.gameId(), k -> new ConcurrentHashMap<>());
        teamAnswers.put(request.teamId(), request.questionId());

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
            
            // Correct answer - move to next question and announce which team was correct
            moveToNextQuestion(request.gameId(), request.teamId());
        } else {
            // Check if all teams have answered incorrectly - if so, move to next question
            Optional<Game> updatedGameOptional = gameService.getGameById(request.gameId());
            if (updatedGameOptional.isPresent()) {
                int totalTeams = updatedGameOptional.get().getGameTeams().size();
                if (answeredTeams.size() >= totalTeams) {
                    // All teams answered - move to next question
                    moveToNextQuestion(request.gameId(), null);
                }
            }
        }

        // Return response with current question info
        QuestionAnswerDto currentQuestion = getCurrentQuestion(request.gameId());

        GameAnswerResponse response = new GameAnswerResponse(
                request.gameId(), 
                request.questionId(), 
                request.teamId(), 
                isCorrect, 
                isCorrect ? "Правильный ответ" : "Неправильный ответ",
                currentQuestion
        );
        
        return response;
    }
}
