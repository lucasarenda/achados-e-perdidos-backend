package com.lostandfound.backend.controller;

import com.lostandfound.backend.dto.response.MatchResponse;
import com.lostandfound.backend.service.MatchQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MatchController {

    private final MatchQueryService matchQueryService;

    @GetMapping("/items/{itemId}/matches")
    public ResponseEntity<List<MatchResponse>> getMatchesForItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(matchQueryService.getMatchesForItem(itemId));
    }

    @PatchMapping("/matches/{matchId}/confirm")
    public ResponseEntity<MatchResponse> confirm(@PathVariable Long matchId, Authentication authentication) {
        return ResponseEntity.ok(matchQueryService.confirm(matchId, authentication.getName()));
    }

    @PatchMapping("/matches/{matchId}/reject")
    public ResponseEntity<MatchResponse> reject(@PathVariable Long matchId, Authentication authentication) {
        return ResponseEntity.ok(matchQueryService.reject(matchId, authentication.getName()));
    }
}
