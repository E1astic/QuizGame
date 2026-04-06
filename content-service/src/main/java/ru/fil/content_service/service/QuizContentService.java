package ru.fil.content_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.stereotype.Service;
import ru.fil.content_service.converter.QuizContentConverter;
import ru.fil.content_service.dto.QuestionFilterRequest;
import ru.fil.content_service.dto.QuestionFilterResponse;
import ru.fil.content_service.dto.QuizCreateRequest;
import ru.fil.content_service.dto.QuizCreateResponse;
import ru.fil.content_service.entity.Question;
import ru.fil.content_service.entity.Quiz;
import ru.fil.content_service.repository.QuestionRepository;
import ru.fil.content_service.repository.QuizQuestionRepository;
import ru.fil.content_service.repository.QuizRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuizContentService {

    private final QuestionRepository questionRepository;
    private final QuizContentConverter quizContentConverter;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public List<QuestionFilterResponse> filterQuestions(QuestionFilterRequest filterRequest) {
        List<Question> filteredQuestions = questionRepository.findByTopicIdInAndDifficultyIn(
                filterRequest.topicIds(), filterRequest.difficulties());
        Collections.shuffle(filteredQuestions);
        return filteredQuestions
                .subList(0, Math.min(filteredQuestions.size(), filterRequest.limit()))
                .stream()
                .map(quizContentConverter::mapToQuestionFilterResponse)
                .toList();
    }

    @Transactional
    public Optional<QuizCreateResponse> createGame(QuizCreateRequest createRequest) {
        Quiz quiz = quizContentConverter.mapToQuiz(createRequest);
        quiz = quizRepository.save(quiz);

        for(QuestionFilterResponse filterResponse : createRequest.content()) {
            quizQuestionRepository.saveNative(quiz.getId(), filterResponse.questionId());
        }

        Optional<Quiz> quizOptional = quizRepository.findById(quiz.getId());
        if(quizOptional.isPresent()) {
            return Optional.of(quizContentConverter.mapToQuizCreateResponse(quiz, createRequest.content().size()));
        }
        return Optional.empty();
    }
}
