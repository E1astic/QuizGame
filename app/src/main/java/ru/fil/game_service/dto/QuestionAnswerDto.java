package ru.fil.game_service.dto;

import java.util.List;
import java.util.UUID;

public record QuestionAnswerDto(
        UUID questionId, 
        String questionText, 
        List<AnswerOptionDto> answers,
        Integer questionNumber,
        Integer totalQuestions
) {
    
    public record AnswerOptionDto(UUID answerId, String answerText) {
    }
}
