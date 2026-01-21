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

    /**
     * Rebalance all items: reassign ranks evenly between "0|a" and "0|z" so that
     * ranks stay short and there is space for future inserts. Call this when
     * creation or move fails with "Rank Space Exhausted" (rank too long).
     */
    public List<BoardItemEntity> rebalance() {
        List<BoardItemEntity> items = repository.findAllByOrderByRankAsc();
        if (items.isEmpty()) {
            return List.of();
        }
        if (items.size() == 1) {
            items.get(0).setRank(LexoRankGeneratorUtils.initial());
            return List.of(repository.save(items.get(0)));
        }
        String left = "0|a";
        String right = "0|z";
        subdivideAssign(left, right, items, 0, items.size() - 1);
        return repository.saveAll(items);
    }

    private void subdivideAssign(String left, String right, List<BoardItemEntity> items, int start, int end) {
        if (start > end) {
            return;
        }
        if (start == end) {
            items.get(start).setRank(LexoRankGeneratorUtils.between(left, right));
            return;
        }
        int mid = (start + end) / 2;
        String midRank = LexoRankGeneratorUtils.between(left, right);
        items.get(mid).setRank(midRank);
        subdivideAssign(left, midRank, items, start, mid - 1);
        subdivideAssign(midRank, right, items, mid + 1, end);
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
