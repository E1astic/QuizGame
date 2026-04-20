package ru.fil.game_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fil.content_service.entity.Answer;
import ru.fil.content_service.entity.Question;
import ru.fil.content_service.entity.Quiz;
import ru.fil.content_service.entity.QuizToQuestion;
import ru.fil.content_service.repository.QuizRepository;
import ru.fil.game_service.dto.GameCreateRequest;
import ru.fil.game_service.dto.GameCreateResponse;
import ru.fil.game_service.entity.Game;
import ru.fil.game_service.entity.GameStatus;
import ru.fil.game_service.repository.GameRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;

    @Transactional
    public Optional<GameCreateResponse> createGame(GameCreateRequest request) {
        Optional<Quiz> quizOptional = quizRepository.findById(request.quizId());
        if (quizOptional.isEmpty()) {
            return Optional.empty();
        }

        Quiz quiz = quizOptional.get();
        
        Game game = Game.builder()
                .quiz(quiz)
                .status(GameStatus.WAITING)
                .build();
        
        game = gameRepository.save(game);
        
        return Optional.of(new GameCreateResponse(game.getId(), quiz.getName()));
    }

    public Optional<Game> getGameById(UUID gameId) {
        return gameRepository.findByIdWithQuizAndQuestions(gameId);
    }

    public Game saveGame(Game game) {
        return gameRepository.save(game);
    }
}
