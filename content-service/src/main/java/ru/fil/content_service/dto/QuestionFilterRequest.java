package ru.fil.content_service.dto;

import ru.fil.content_service.entity.QuestionDifficulty;

import java.util.List;
import java.util.UUID;

public record QuestionFilterRequest(List<UUID> topicIds, List<QuestionDifficulty> difficulties, int limit) {

}
