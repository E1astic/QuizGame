package ru.fil.game_service.dto;

import java.util.List;
import java.util.UUID;

public record QuestionAnswerDto(
        UUID questionId, 
        String questionText, 
        List<AnswerOptionDto> answers,
        Integer questionNumber,
        Integer totalQuestions,
        Integer timeLeft
) {
    
    public record AnswerOptionDto(UUID answerId, String answerText) {
    }
    
    // Конструктор без timeLeft для обратной совместимости
    public QuestionAnswerDto(UUID questionId, String questionText, List<AnswerOptionDto> answers, Integer questionNumber, Integer totalQuestions) {
        this(questionId, questionText, answers, questionNumber, totalQuestions, 30);
    }
}
