package ru.fil.app.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fil.app.game.dto.AnswerSubmitRequest;
import ru.fil.app.game.dto.QuestionMessage;
import ru.fil.app.game.entity.GameStatus;
import ru.fil.app.game.entity.QuizGame;
import ru.fil.app.game.entity.QuizGameTeam;
import ru.fil.app.game.repository.QuizGameRepository;
import ru.fil.content_service.entity.Answer;
import ru.fil.content_service.entity.Question;
import ru.fil.content_service.entity.Quiz;
import ru.fil.content_service.entity.QuizToQuestion;
import ru.fil.content_service.repository.QuestionRepository;
import ru.fil.content_service.repository.QuizRepository;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGameService {

    private final QuizGameRepository quizGameRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    // In-memory хранилище для активных игр
    private final Map<UUID, GameSession> activeGames = new ConcurrentHashMap<>();

    @Transactional
    public void registerTeam(UUID gameId, UUID teamId) {
        QuizGame game = quizGameRepository.findByIdWithTeams(gameId)
                .orElseThrow(() -> new RuntimeException("Игра не найдена: " + gameId));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new RuntimeException("Регистрация закрыта. Игра уже началась или завершена.");
        }

        // Проверяем, не зарегистрирована ли уже команда
        boolean alreadyRegistered = game.getGameTeams().stream()
                .anyMatch(gt -> gt.getTeam().getId().equals(teamId));

        if (alreadyRegistered) {
            throw new RuntimeException("Команда уже зарегистрирована в этой игре.");
        }

        // Создаем связь команды с игрой
        QuizGameTeam gameTeam = QuizGameTeam.builder()
                .game(game)
                .teamId(new ru.fil.app.game.entity.QuizGameTeamId(gameId, teamId))
                .score(0)
                .build();

        game.getGameTeams().add(gameTeam);
        quizGameRepository.save(game);

        log.info("Команда {} зарегистрирована в игре {}", teamId, gameId);
    }

    @Transactional
    public void startGame(UUID gameId) {
        QuizGame game = quizGameRepository.findByIdWithTeams(gameId)
                .orElseThrow(() -> new RuntimeException("Игра не найдена: " + gameId));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new RuntimeException("Игра уже началась или завершена.");
        }

        if (game.getGameTeams().isEmpty()) {
            throw new RuntimeException("Нет зарегистрированных команд для начала игры.");
        }

        // Загружаем вопросы из связанного квиза
        Quiz quiz = game.getQuiz();
        List<Question> questions = loadQuestionsForQuiz(quiz.getId());

        if (questions.isEmpty()) {
            throw new RuntimeException("В квизе нет вопросов.");
        }

        // Переносим вопросы в in-memory хранилище
        GameSession session = new GameSession();
        session.setGame(game);
        session.setQuestions(questions);
        session.setCurrentQuestionIndex(0);
        session.setTeamScores(new ConcurrentHashMap<>());

        // Инициализируем счетчики команд
        for (QuizGameTeam gameTeam : game.getGameTeams()) {
            session.getTeamScores().put(gameTeam.getTeam().getId(), 0);
        }

        activeGames.put(gameId, session);

        // Обновляем статус игры
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(OffsetDateTime.now());
        quizGameRepository.save(game);

        log.info("Игра {} началась. Вопросов: {}", gameId, questions.size());

        // Отправляем первый вопрос всем участникам
        sendQuestionToPlayers(gameId, 0);
    }

    private List<Question> loadQuestionsForQuiz(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Квиз не найден"));

        // Загружаем вопросы с ответами через join fetch
        return questionRepository.findByQuizIdWithAnswers(quizId).stream()
                .sorted(Comparator.comparing(q -> {
                    // Находим порядок вопроса в квизе
                    Optional<QuizToQuestion> qtq = quiz.getQuestions().stream()
                            .filter(qt -> qt.getQuestion().getId().equals(q.getId()))
                            .findFirst();
                    return qtq.map(value -> 0).orElse(1);
                }))
                .collect(Collectors.toList());
    }

    private void sendQuestionToPlayers(UUID gameId, int questionIndex) {
        GameSession session = activeGames.get(gameId);
        if (session == null) {
            log.error("Сессия игры не найдена: {}", gameId);
            return;
        }

        List<Question> questions = session.getQuestions();
        if (questionIndex >= questions.size()) {
            finishGame(gameId);
            return;
        }

        Question question = questions.get(questionIndex);
        session.setCurrentQuestionIndex(questionIndex);

        // Формируем сообщение с вопросом (без указания правильного ответа)
        List<QuestionMessage.AnswerOption> answerOptions = question.getAnswers().stream()
                .map(answer -> QuestionMessage.AnswerOption.builder()
                        .answerId(answer.getId())
                        .answerText(answer.getName())
                        .build())
                .collect(Collectors.toList());

        // Перемешиваем варианты ответов
        Collections.shuffle(answerOptions);

        QuestionMessage message = QuestionMessage.builder()
                .questionId(question.getId())
                .questionText(question.getName())
                .answers(answerOptions)
                .questionNumber(questionIndex + 1)
                .totalQuestions(questions.size())
                .build();

        // Отправляем вопрос всем подписчикам игры
        String destination = "/topic/game/" + gameId + "/question";
        messagingTemplate.convertAndSend(destination, message);

        log.info("Отправлен вопрос {} игрокам игры {}", questionIndex + 1, gameId);
    }

    @Transactional
    public void submitAnswer(AnswerSubmitRequest request) {
        UUID gameId = request.getGameId();
        UUID teamId = request.getTeamId();
        UUID questionId = request.getQuestionId();
        UUID answerId = request.getAnswerId();

        GameSession session = activeGames.get(gameId);
        if (session == null) {
            throw new RuntimeException("Игра не активна");
        }

        Question currentQuestion = session.getQuestions().get(session.getCurrentQuestionIndex());
        if (!currentQuestion.getId().equals(questionId)) {
            throw new RuntimeException("Неактуальный вопрос. Ожидается вопрос: " + currentQuestion.getId());
        }

        // Проверяем правильность ответа
        Boolean isCorrect = questionRepository.isAnswerCorrect(answerId, questionId);
        
        if (isCorrect == null) {
            throw new RuntimeException("Ответ не найден для данного вопроса");
        }

        if (isCorrect) {
            // Обновляем счет команды
            session.getTeamScores().merge(teamId, 10, Integer::sum);

            log.info("Команда {} дала правильный ответ на вопрос {} в игре {}", teamId, questionId, gameId);

            // Переходим к следующему вопросу
            int nextIndex = session.getCurrentQuestionIndex() + 1;
            sendQuestionToPlayers(gameId, nextIndex);
        } else {
            log.info("Команда {} дала неправильный ответ на вопрос {} в игре {}", teamId, questionId, gameId);
            // Отправляем уведомление о неправильном ответе только этой команде
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/wrong-answer", 
                Map.of("questionId", questionId, "answerId", answerId, "correct", false, "teamId", teamId));
        }
    }

    private void finishGame(UUID gameId) {
        GameSession session = activeGames.remove(gameId);
        if (session == null) {
            return;
        }

        QuizGame game = session.getGame();
        game.setStatus(GameStatus.FINISHED);
        game.setFinishedAt(OffsetDateTime.now());
        quizGameRepository.save(game);

        // Отправляем сообщение о завершении игры
        Map<String, Object> result = new HashMap<>();
        result.put("gameId", gameId);
        result.put("status", "FINISHED");
        result.put("scores", session.getTeamScores());

        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/finished", result);

        log.info("Игра {} завершена. Результаты: {}", gameId, session.getTeamScores());
    }

    public Map<UUID, Integer> getGameScores(UUID gameId) {
        GameSession session = activeGames.get(gameId);
        if (session == null) {
            return Collections.emptyMap();
        }
        return new HashMap<>(session.getTeamScores());
    }

    // Внутренний класс для хранения состояния активной игры
    @lombok.Data
    public static class GameSession {
        private QuizGame game;
        private List<Question> questions;
        private int currentQuestionIndex;
        private Map<UUID, Integer> teamScores;
    }
}
