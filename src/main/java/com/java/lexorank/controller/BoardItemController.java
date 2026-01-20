package com.java.lexorank.controller;

import com.java.lexorank.entity.BoardItemEntity;
import com.java.lexorank.dto.BoardItemResponse;
import com.java.lexorank.dto.CreateBoardItemRequest;
import com.java.lexorank.dto.MoveBoardItemRequest;
import com.java.lexorank.service.BoardItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
@Tag(name = "Board Items", description = "Manage board items with LexoRank ordering")
@AllArgsConstructor
public class BoardItemController {
    private final BoardItemService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new board item", description = "Creates a board item with an automatically assigned LexoRank")
    public BoardItemResponse create(@Valid @RequestBody CreateBoardItemRequest request) {
        BoardItemEntity item = service.create(request.getTitle());
        return toResponse(item);
    }

    @GetMapping
    @Operation(summary = "List all board items", description = "Returns all items ordered by their LexoRank")
    public List<BoardItemResponse> list() {
        return service.listOrdered().stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}/move")
    @Operation(summary = "Reorder a board item", description = "Moves an item between two others, updating only the moved item's rank")
    public BoardItemResponse move(@PathVariable UUID id, @Valid @RequestBody MoveBoardItemRequest request) {
        BoardItemEntity item = service.move(id, request.getLeftId(), request.getRightId());
        return toResponse(item);
    }

    private BoardItemResponse toResponse(BoardItemEntity item) {
        return new BoardItemResponse(item.getId(), item.getTitle(), item.getRank(), item.getCreatedAt());
    }
}
