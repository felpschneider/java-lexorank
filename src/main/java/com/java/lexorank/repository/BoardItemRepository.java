package com.java.lexorank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.java.lexorank.entity.BoardItemEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardItemRepository extends JpaRepository<BoardItemEntity, UUID> {
    List<BoardItemEntity> findAllByOrderByRankAsc();

    @Query("SELECT b.rank FROM BoardItemEntity b ORDER BY b.rank DESC LIMIT 1")
    Optional<String> findMaxRank();
}
