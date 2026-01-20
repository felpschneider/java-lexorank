package com.java.lexorank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardItemRequest {
    @NotBlank(message = "Title is required")
    private String title;
}
