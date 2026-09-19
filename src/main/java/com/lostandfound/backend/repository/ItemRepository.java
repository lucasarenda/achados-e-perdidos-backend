package com.lostandfound.backend.repository;

import com.lostandfound.backend.model.Item;
import com.lostandfound.backend.model.enums.ItemCategory;
import com.lostandfound.backend.model.enums.ItemStatus;
import com.lostandfound.backend.model.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("""
        SELECT i FROM Item i
        WHERE (:type IS NULL OR i.type = :type)
          AND (:category IS NULL OR i.category = :category)
          AND (:status IS NULL OR i.status = :status)
          AND (:color IS NULL OR LOWER(i.color) = :color)
        ORDER BY i.createdAt DESC
        """)
    List<Item> search(@Param("type") ItemType type,
                      @Param("category") ItemCategory category,
                      @Param("status") ItemStatus status,
                      @Param("color") String color);

    List<Item> findByTypeAndCategoryAndStatus(ItemType type, ItemCategory category, ItemStatus status);

    List<Item> findByUserId(Long userId);
}
