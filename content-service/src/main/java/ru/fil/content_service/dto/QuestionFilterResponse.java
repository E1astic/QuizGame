package ru.fil.content_service.dto;

import java.util.UUID;

public record QuestionFilterResponse(UUID questionId, String questionName, String difficulty, String topicName) {
}
