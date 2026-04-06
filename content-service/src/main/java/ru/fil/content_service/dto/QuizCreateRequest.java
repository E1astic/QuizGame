package ru.fil.content_service.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record QuizCreateRequest(String name, OffsetDateTime startAt, List<QuestionFilterResponse> content) {
}
