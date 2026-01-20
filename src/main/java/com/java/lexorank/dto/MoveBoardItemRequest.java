package com.java.lexorank.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MoveBoardItemRequest {
    private UUID leftId;
    private UUID rightId;
}
