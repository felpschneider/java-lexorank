package com.java.lexorank.service;

import com.java.lexorank.utils.LexoRankGeneratorUtils;
import com.java.lexorank.entity.BoardItemEntity;
import com.java.lexorank.exception.NotFoundException;
import com.java.lexorank.repository.BoardItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class BoardItemService {
    private final BoardItemRepository repository;

    public BoardItemEntity create(String title) {
        String rank = repository.findMaxRank()
                .map(maxRank -> LexoRankGeneratorUtils.between(maxRank, null))
                .orElseGet(LexoRankGeneratorUtils::initial);

        BoardItemEntity item = new BoardItemEntity(UUID.randomUUID(), title, rank, Instant.now());
        return repository.save(item);
    }

    public List<BoardItemEntity> listOrdered() {
        return repository.findAllByOrderByRankAsc();
    }

    public BoardItemEntity move(UUID itemId, UUID leftId, UUID rightId) {
        BoardItemEntity item = repository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        if (leftId == null && rightId == null) {
            if (repository.count() == 0) {
                item.setRank(LexoRankGeneratorUtils.initial());
                return repository.save(item);
            }
            throw new IllegalArgumentException("Either leftId or rightId must be provided.");
        }
        if (leftId != null && leftId.equals(itemId)) {
            throw new IllegalArgumentException("leftId cannot be the same as itemId.");
        }
        if (rightId != null && rightId.equals(itemId)) {
            throw new IllegalArgumentException("rightId cannot be the same as itemId.");
        }

        // Get all items ordered by rank (excluding the item being moved)
        List<BoardItemEntity> allItems = repository.findAllByOrderByRankAsc().stream()
                .filter(i -> !i.getId().equals(itemId))
                .toList();

        String leftRank = null;
        String rightRank = null;

        if (leftId == null) {
            // Moving to the beginning - use first item as right boundary
            if (!allItems.isEmpty()) {
                rightRank = allItems.get(0).getRank();
            }
        } else if (rightId == null) {
            // Moving after leftId - find the next item after leftId
            BoardItemEntity leftItem = repository.findById(leftId)
                    .orElseThrow(() -> new NotFoundException("Left item not found: " + leftId));
            leftRank = leftItem.getRank();

            // Find the next item after leftItem in the ordered list
            for (int i = 0; i < allItems.size(); i++) {
                if (allItems.get(i).getId().equals(leftId)) {
                    if (i + 1 < allItems.size()) {
                        rightRank = allItems.get(i + 1).getRank();
                    }
                    break;
                }
            }
            // If rightRank is still null, leftId was the last item, so rightRank stays null
        } else {
            // Both leftId and rightId provided
            leftRank = leftId == null ? null : repository.findById(leftId)
                    .orElseThrow(() -> new NotFoundException("Left item not found: " + leftId))
                    .getRank();
            rightRank = repository.findById(rightId)
                    .orElseThrow(() -> new NotFoundException("Right item not found: " + rightId))
                    .getRank();
        }

        String newRank = LexoRankGeneratorUtils.between(leftRank, rightRank);
        item.setRank(newRank);
        return repository.save(item);
    }
}
