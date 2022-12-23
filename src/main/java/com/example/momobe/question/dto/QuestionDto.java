package com.example.momobe.question.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionDto {
    @NotBlank
    private String content;
}
