package ru.fil.content_service.converter;

import org.springframework.stereotype.Component;
import ru.fil.content_service.dto.QuestionFilterResponse;
import ru.fil.content_service.dto.QuizCreateRequest;
import ru.fil.content_service.dto.QuizCreateResponse;
import ru.fil.content_service.entity.Question;
import ru.fil.content_service.entity.Quiz;

import java.util.HashSet;
import java.util.Set;

@Component
public class QuizContentConverter {

    public QuestionFilterResponse mapToQuestionFilterResponse(Question question) {
        return new QuestionFilterResponse(
                question.getId(),
                question.getName(),
                question.getDifficulty().name(),
                question.getTopic().getName()
        );
    }

    public Quiz mapToQuiz(QuizCreateRequest createRequest) {
        Set<String> topics = new HashSet<>();
        Set<String> difficulties = new HashSet<>();
        for (QuestionFilterResponse question : createRequest.content()) {
            topics.add(question.topicName());
            difficulties.add(question.difficulty());
        }
        String topicsField = String.join(", ", topics);
        String difficultiesField = String.join(", ", difficulties);

        return Quiz.builder()
                .name(createRequest.name())
                .startAt(createRequest.startAt())
                .topics(topicsField)
                .difficulties(difficultiesField)
                .questions(new HashSet<>())
                .build();
    }

    public QuizCreateResponse mapToQuizCreateResponse(Quiz quiz, int questionsNum) {
        return new QuizCreateResponse(
                quiz.getName(), quiz.getStartAt(), quiz.getTopics(), quiz.getDifficulties(), questionsNum
        );
    }
}
