package com.mental.health.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class SaveAnswersRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long itemId;
        private Integer answerValue;
        private String answerLabel;
    }
}
