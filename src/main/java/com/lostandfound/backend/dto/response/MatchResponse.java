package com.lostandfound.backend.dto.response;

import com.lostandfound.backend.model.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponse {
    private Long id;
    private ItemResponse lostItem;
    private ItemResponse foundItem;
    private Integer score;
    private MatchStatus status;
    private LocalDateTime createdAt;
}
