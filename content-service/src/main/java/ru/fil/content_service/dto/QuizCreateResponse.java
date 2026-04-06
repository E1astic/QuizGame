package ru.fil.content_service.dto;

import java.time.OffsetDateTime;

public record QuizCreateResponse(String name, OffsetDateTime startAt, String topics, String difficulties, int questionCount) {
}
