package com.lostandfound.backend.service;

import com.lostandfound.backend.model.Item;
import com.lostandfound.backend.model.Match;
import com.lostandfound.backend.model.enums.ItemCategory;
import com.lostandfound.backend.model.enums.ItemStatus;
import com.lostandfound.backend.model.enums.ItemType;
import com.lostandfound.backend.model.enums.MatchStatus;
import com.lostandfound.backend.repository.ItemRepository;
import com.lostandfound.backend.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchingService")
class MatchingServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchingService matchingService;

    private Item lostItem;
    private Item foundItem;

    @BeforeEach
    void setUp() {
        lostItem = Item.builder()
                .id(1L)
                .type(ItemType.PERDIDO)
                .category(ItemCategory.MOCHILA)
                .color("Black")
                .occurredAt(LocalDate.of(2026, 9, 18))
                .description("Black backpack with a blue keychain")
                .latitude(-23.5489)
                .longitude(-46.6388)
                .status(ItemStatus.ATIVO)
                .build();

        foundItem = Item.builder()
                .id(2L)
                .type(ItemType.ENCONTRADO)
                .category(ItemCategory.MOCHILA)
                .color("Black")
                .occurredAt(LocalDate.of(2026, 9, 18))
                .description("Black backpack found near the entrance, has a blue keychain")
                .latitude(-23.5490)
                .longitude(-46.6389)
                .status(ItemStatus.ATIVO)
                .build();
    }

    @Nested
    @DisplayName("calculateScore")
    class CalculateScore {

        @Test
        @DisplayName("scores 100 when category, color, date, location and description all match")
        void scoresMaximumWhenEverythingMatches() {
            int score = matchingService.calculateScore(lostItem, foundItem);

            assertThat(score).isEqualTo(100);
        }

        @Test
        @DisplayName("scores 0 when nothing matches")
        void scoresZeroWhenNothingMatches() {
            foundItem.setCategory(ItemCategory.CARTEIRA);
            foundItem.setColor("Red");
            foundItem.setOccurredAt(LocalDate.of(2026, 1, 1));
            foundItem.setLatitude(10.0);
            foundItem.setLongitude(10.0);
            foundItem.setDescription("Completely unrelated text");

            int score = matchingService.calculateScore(lostItem, foundItem);

            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("adds only the category score when just the category matches")
        void scoresOnlyCategory() {
            foundItem.setColor("Red");
            foundItem.setOccurredAt(LocalDate.of(2026, 1, 1));
            foundItem.setLatitude(10.0);
            foundItem.setLongitude(10.0);
            foundItem.setDescription("Nothing similar here");

            int score = matchingService.calculateScore(lostItem, foundItem);

            assertThat(score).isEqualTo(30);
        }

        @Test
        @DisplayName("does not score color when either item has no color set")
        void doesNotScoreColorWhenMissing() {
            lostItem.setColor(null);

            int score = matchingService.calculateScore(lostItem, foundItem);

            // 100 minus the 20 color points
            assertThat(score).isEqualTo(80);
        }

        @Test
        @DisplayName("scores the date criterion when items are within 3 days of each other")
        void scoresDateWithinProximityWindow() {
            foundItem.setOccurredAt(lostItem.getOccurredAt().plusDays(3));

            int score = matchingService.calculateScore(lostItem, foundItem);

            assertThat(score).isEqualTo(100);
        }

        @Test
        @DisplayName("does not score the date criterion when items are more than 3 days apart")
        void doesNotScoreDateOutsideProximityWindow() {
            foundItem.setOccurredAt(lostItem.getOccurredAt().plusDays(4));

            int score = matchingService.calculateScore(lostItem, foundItem);

            assertThat(score).isEqualTo(80);
        }

        @Test
        @DisplayName("scores the location criterion when items are within 500 meters")
        void scoresLocationWithinProximity() {
            // ~200m apart in São Paulo
            foundItem.setLatitude(-23.5507);
            foundItem.setLongitude(-46.6388);

            int score = matchingService.calculateScore(lostItem, foundItem);

            assertThat(score).isEqualTo(100);
        }

        @Test
        @DisplayName("does not score the location criterion when items are far apart")
        void doesNotScoreLocationOutsideProximity() {
            foundItem.setLatitude(-23.6000);
            foundItem.setLongitude(-46.7000);

            int score = matchingService.calculateScore(lostItem, foundItem);

            assertThat(score).isEqualTo(80);
        }

        @Test
        @DisplayName("does not score the location criterion when coordinates are missing")
        void doesNotScoreLocationWhenMissing() {
            lostItem.setLatitude(null);
            lostItem.setLongitude(null);

            int score = matchingService.calculateScore(lostItem, foundItem);

            assertThat(score).isEqualTo(80);
        }

        @Test
        @DisplayName("does not score the description criterion when descriptions share no meaningful words")
        void doesNotScoreDescriptionWhenUnrelated() {
            foundItem.setDescription("Totally different sentence about something else");

            int score = matchingService.calculateScore(lostItem, foundItem);

            assertThat(score).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("findAndPersistMatches")
    class FindAndPersistMatches {

        @Test
        @DisplayName("persists a match when a candidate scores at or above the minimum threshold")
        void persistsMatchAboveThreshold() {
            when(itemRepository.findByTypeAndCategoryAndStatus(
                    ItemType.ENCONTRADO, ItemCategory.MOCHILA, ItemStatus.ATIVO))
                    .thenReturn(List.of(foundItem));
            when(matchRepository.existsByLostItemIdAndFoundItemId(1L, 2L)).thenReturn(false);
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

            List<Match> matches = matchingService.findAndPersistMatches(lostItem);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).getScore()).isEqualTo(100);
            assertThat(matches.get(0).getStatus()).isEqualTo(MatchStatus.PENDENTE);
            assertThat(matches.get(0).getLostItem()).isEqualTo(lostItem);
            assertThat(matches.get(0).getFoundItem()).isEqualTo(foundItem);
            verify(matchRepository, times(1)).save(any(Match.class));
        }

        @Test
        @DisplayName("does not persist a match when the candidate scores below the minimum threshold")
        void doesNotPersistMatchBelowThreshold() {
            foundItem.setColor("Red");
            foundItem.setOccurredAt(LocalDate.of(2026, 1, 1));
            foundItem.setLatitude(10.0);
            foundItem.setLongitude(10.0);
            foundItem.setDescription("Nothing similar here");

            when(itemRepository.findByTypeAndCategoryAndStatus(
                    ItemType.ENCONTRADO, ItemCategory.MOCHILA, ItemStatus.ATIVO))
                    .thenReturn(List.of(foundItem));

            List<Match> matches = matchingService.findAndPersistMatches(lostItem);

            assertThat(matches).isEmpty();
            verify(matchRepository, never()).save(any(Match.class));
        }

        @Test
        @DisplayName("does not persist a duplicate match for the same item pair")
        void doesNotPersistDuplicateMatch() {
            when(itemRepository.findByTypeAndCategoryAndStatus(
                    ItemType.ENCONTRADO, ItemCategory.MOCHILA, ItemStatus.ATIVO))
                    .thenReturn(List.of(foundItem));
            when(matchRepository.existsByLostItemIdAndFoundItemId(1L, 2L)).thenReturn(true);

            List<Match> matches = matchingService.findAndPersistMatches(lostItem);

            assertThat(matches).isEmpty();
            verify(matchRepository, never()).save(any(Match.class));
        }

        @Test
        @DisplayName("searches for ENCONTRADO candidates when the new item is PERDIDO, and vice-versa")
        void searchesOppositeType() {
            when(itemRepository.findByTypeAndCategoryAndStatus(
                    ItemType.PERDIDO, ItemCategory.MOCHILA, ItemStatus.ATIVO))
                    .thenReturn(List.of());

            matchingService.findAndPersistMatches(foundItem);

            verify(itemRepository).findByTypeAndCategoryAndStatus(
                    ItemType.PERDIDO, ItemCategory.MOCHILA, ItemStatus.ATIVO);
        }
    }
}
