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
    private static final String REBALANCE_LEFT = "0|a";
    private static final String REBALANCE_RIGHT = "0|z";

    private final BoardItemRepository repository;

    public BoardItemEntity create(String title) {
        String rank = nextRankForAppend();
        BoardItemEntity item = new BoardItemEntity(UUID.randomUUID(), title, rank, Instant.now());
        return repository.save(item);
    }

    private String nextRankForAppend() {
        return repository.findMaxRank()
                .map(max -> LexoRankGeneratorUtils.between(max, null))
                .orElseGet(LexoRankGeneratorUtils::initial);
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
            return rebalanceSingle(items.get(0));
        }
        subdivideAssign(REBALANCE_LEFT, REBALANCE_RIGHT, items, 0, items.size() - 1);
        return repository.saveAll(items);
    }

    private List<BoardItemEntity> rebalanceSingle(BoardItemEntity item) {
        item.setRank(LexoRankGeneratorUtils.initial());
        return List.of(repository.save(item));
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
        BoardItemEntity item = findOrThrow(itemId);

        if (leftId == null && rightId == null) {
            return handleMoveWhenBothNull(item);
        }
        validateMoveIds(itemId, leftId, rightId);

        List<BoardItemEntity> others = findAllExcluding(itemId);
        RankBounds bounds = resolveRankBounds(leftId, rightId, others);

        item.setRank(LexoRankGeneratorUtils.between(bounds.left(), bounds.right()));
        return repository.save(item);
    }

    private BoardItemEntity findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found: " + id));
    }

    private BoardItemEntity handleMoveWhenBothNull(BoardItemEntity item) {
        if (repository.count() > 1) {
            throw new IllegalArgumentException("Either leftId or rightId must be provided.");
        }
        item.setRank(LexoRankGeneratorUtils.initial());
        return repository.save(item);
    }

    private void validateMoveIds(UUID itemId, UUID leftId, UUID rightId) {
        if (leftId != null && leftId.equals(itemId)) {
            throw new IllegalArgumentException("leftId cannot be the same as itemId.");
        }
        if (rightId != null && rightId.equals(itemId)) {
            throw new IllegalArgumentException("rightId cannot be the same as itemId.");
        }
    }

    private List<BoardItemEntity> findAllExcluding(UUID excludeId) {
        return repository.findAllByOrderByRankAsc().stream()
                .filter(i -> !i.getId().equals(excludeId))
                .toList();
    }

    private RankBounds resolveRankBounds(UUID leftId, UUID rightId, List<BoardItemEntity> others) {
        if (leftId == null) {
            return new RankBounds(null, rankOf(rightId));
        }
        if (rightId == null) {
            String left = rankOf(leftId);
            String right = rankOfNextAfter(others, leftId);
            return new RankBounds(left, right);
        }
        return new RankBounds(rankOf(leftId), rankOf(rightId));
    }

    private String rankOf(UUID id) {
        return findOrThrow(id).getRank();
    }

    private String rankOfNextAfter(List<BoardItemEntity> ordered, UUID afterId) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(afterId)) {
                return (i + 1 < ordered.size()) ? ordered.get(i + 1).getRank() : null;
            }
        }
        return null;
    }

    private record RankBounds(String left, String right) {}
}
