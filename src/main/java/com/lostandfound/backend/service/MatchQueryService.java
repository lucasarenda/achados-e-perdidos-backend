package com.lostandfound.backend.service;

import com.lostandfound.backend.dto.response.MatchResponse;
import com.lostandfound.backend.exception.ForbiddenException;
import com.lostandfound.backend.exception.ResourceNotFoundException;
import com.lostandfound.backend.model.Match;
import com.lostandfound.backend.model.enums.ItemStatus;
import com.lostandfound.backend.model.enums.MatchStatus;
import com.lostandfound.backend.repository.ItemRepository;
import com.lostandfound.backend.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchQueryService {

    private final MatchRepository matchRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesForItem(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item com o id: " + itemId + " não encontrado" );
        }
        return matchRepository.findAllByItemId(itemId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MatchResponse confirm(Long matchId, String userEmail) {
        Match match = getMatchOrThrow(matchId);
        assertInvolved(match, userEmail);

        match.setStatus(MatchStatus.CONFIRMADO);
        matchRepository.save(match);


        match.getLostItem().setStatus(ItemStatus.RESOLVIDO);
        match.getFoundItem().setStatus(ItemStatus.RESOLVIDO);
        itemRepository.save(match.getLostItem());
        itemRepository.save(match.getFoundItem());

        return toResponse(match);
    }

    @Transactional
    public MatchResponse reject(Long matchId, String userEmail) {
        Match match = getMatchOrThrow(matchId);
        assertInvolved(match, userEmail);

        match.setStatus(MatchStatus.REJEITADO);
        return toResponse(matchRepository.save(match));
    }

    private Match getMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match não encontrado id: " + matchId));
    }

    private void assertInvolved(Match match, String userEmail) {
        boolean isOwnerOfLostItem = match.getLostItem().getUser().getEmail().equalsIgnoreCase(userEmail);
        boolean isOwnerOfFoundItem = match.getFoundItem().getUser().getEmail().equalsIgnoreCase(userEmail);

        if (!isOwnerOfLostItem && !isOwnerOfFoundItem) {
            throw new ForbiddenException("Voê não tem permissão para atualizar este match");
        }
    }

    private MatchResponse toResponse(Match match) {
        return MatchResponse.builder()
                .id(match.getId())
                .lostItem(itemService.toResponse(match.getLostItem()))
                .foundItem(itemService.toResponse(match.getFoundItem()))
                .score(match.getScore())
                .status(match.getStatus())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
