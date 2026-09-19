package com.lostandfound.backend.repository;

import com.lostandfound.backend.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("""
            SELECT m FROM Match m
            WHERE m.lostItem.id = :itemId OR m.foundItem.id = :itemId
            ORDER BY m.score DESC
            """)
    List<Match> findAllByItemId(@Param("itemId") Long itemId);

    boolean existsByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);
}
