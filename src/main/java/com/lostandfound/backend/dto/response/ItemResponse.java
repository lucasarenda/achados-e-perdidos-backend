package com.lostandfound.backend.dto.response;

import com.lostandfound.backend.model.enums.ItemCategory;
import com.lostandfound.backend.model.enums.ItemStatus;
import com.lostandfound.backend.model.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemResponse {
    private Long id;
    private String title;
    private String description;
    private ItemType type;
    private ItemCategory category;
    private String color;
    private LocalDate occurredAt;
    private Double latitude;
    private Double longitude;
    private String locationDescription;
    private ItemStatus status;
    private Long ownerId;
    private String ownerName;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
