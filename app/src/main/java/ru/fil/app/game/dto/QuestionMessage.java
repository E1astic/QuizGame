package ru.fil.app.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMessage {
    private UUID questionId;
    private String questionText;
    private List<AnswerOption> answers;
    private int questionNumber;
    private int totalQuestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerOption {
        private UUID answerId;
        private String answerText;
    }
}
