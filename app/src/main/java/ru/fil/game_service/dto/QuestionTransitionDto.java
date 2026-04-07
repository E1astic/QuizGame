package ru.fil.game_service.dto;

import java.util.UUID;

public record QuestionTransitionDto(
        UUID gameId,
        Integer questionNumber,
        Integer totalQuestions,
        String questionText,
        UUID correctTeamId,
        String correctTeamName
) {
}
