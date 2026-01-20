package com.java.lexorank.service;

import com.java.lexorank.domain.LexoRankGenerator;
import com.java.lexorank.domain.BoardItem;
import com.java.lexorank.repository.BoardItemRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BoardItemService {
    private final BoardItemRepository repository;
    private final LexoRankGenerator lexoRankGenerator;

    public BoardItemService(BoardItemRepository repository, LexoRankGenerator lexoRankGenerator) {
        this.repository = repository;
        this.lexoRankGenerator = lexoRankGenerator;
    }

    public BoardItem create(String title) {
        String rank = repository.findMaxRank()
                .map(maxRank -> lexoRankGenerator.between(maxRank, null))
                .orElseGet(lexoRankGenerator::initial);

        BoardItem item = new BoardItem(UUID.randomUUID(), title, rank, Instant.now());
        return repository.save(item);
    }

    public List<BoardItem> listOrdered() {
        return repository.findAllByOrderByRankAsc();
    }

    public BoardItem move(UUID itemId, UUID leftId, UUID rightId) {
        BoardItem item = repository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        if (leftId == null && rightId == null) {
            if (repository.count() == 0) {
                item.setRank(lexoRankGenerator.initial());
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
        List<BoardItem> allItems = repository.findAllByOrderByRankAsc().stream()
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
            BoardItem leftItem = repository.findById(leftId)
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

        String newRank = lexoRankGenerator.between(leftRank, rightRank);
        item.setRank(newRank);
        return repository.save(item);
    }
}
