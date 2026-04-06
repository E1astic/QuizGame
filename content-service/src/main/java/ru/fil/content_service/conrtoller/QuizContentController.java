package ru.fil.content_service.conrtoller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fil.content_service.dto.QuestionFilterRequest;
import ru.fil.content_service.dto.QuestionFilterResponse;
import ru.fil.content_service.dto.QuizCreateRequest;
import ru.fil.content_service.dto.QuizCreateResponse;
import ru.fil.content_service.entity.Question;
import ru.fil.content_service.service.QuizContentService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class QuizContentController {

    private final QuizContentService quizContentService;

    @PostMapping("/filter")
    public ResponseEntity<List<QuestionFilterResponse>> getFilteredQuestions(
            @RequestBody QuestionFilterRequest filterRequest
    ) {
        return ResponseEntity.ok(quizContentService.filterQuestions(filterRequest));
    }

    @PostMapping("/game")
    public ResponseEntity<?> createGame(
            @RequestBody QuizCreateRequest createRequest
    ) {
        Optional<QuizCreateResponse> quiz = quizContentService.createGame(createRequest);
        return quiz.isPresent()
                ? ResponseEntity.ok(quiz.get())
                : ResponseEntity.badRequest().body("Ошибка при создании квиза");
    }

}
