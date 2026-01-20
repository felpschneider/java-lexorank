package com.java.lexorank.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardItemResponse {
    private UUID id;
    private String title;
    private String rank;
    private Instant createdAt;
}
