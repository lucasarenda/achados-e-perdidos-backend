package com.lostandfound.backend.service;

import com.lostandfound.backend.model.Item;
import com.lostandfound.backend.model.Match;
import com.lostandfound.backend.model.enums.ItemStatus;
import com.lostandfound.backend.model.enums.ItemType;
import com.lostandfound.backend.model.enums.MatchStatus;
import com.lostandfound.backend.repository.ItemRepository;
import com.lostandfound.backend.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private static final int DATE_PROXIMITY_DAYS = 3;
    private static final double LOCATION_PROXIMITY_METERS = 500;
    private static final int MIN_SCORE_TO_PERSIST = 40;

    private static final int SCORE_CATEGORY = 30;
    private static final int SCORE_COLOR = 20;
    private static final int SCORE_DATE = 20;
    private static final int SCORE_LOCATION = 20;
    private static final int SCORE_DESCRIPTION = 10;

    private final ItemRepository itemRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public List<Match> findAndPersistMatches(Item newItem) {
        ItemType oppositeType = newItem.getType() == ItemType.PERDIDO ? ItemType.ENCONTRADO : ItemType.PERDIDO;

        List<Item> candidates = itemRepository.findByTypeAndCategoryAndStatus(
                oppositeType, newItem.getCategory(), ItemStatus.ATIVO);

        return candidates.stream()
                .map(candidate -> buildMatch(newItem, candidate))
                .filter(match -> match.getScore() >= MIN_SCORE_TO_PERSIST)
                .filter(match -> !alreadyExists(match))
                .map(matchRepository::save)
                .collect(Collectors.toList());
    }

    private Match buildMatch(Item newItem, Item candidate) {
        Item lostItem = newItem.getType() == ItemType.PERDIDO ? newItem : candidate;
        Item foundItem = newItem.getType() == ItemType.ENCONTRADO? newItem : candidate;

        int score = calculateScore(lostItem, foundItem);

        return Match.builder()
                .lostItem(lostItem)
                .foundItem(foundItem)
                .score(score)
                .status(MatchStatus.PENDENTE)
                .build();
    }

    public int calculateScore(Item lostItem, Item foundItem) {
        int score = 0;

        if (lostItem.getCategory() == foundItem.getCategory()) {
            score += SCORE_CATEGORY;
        }

        if (sameColor(lostItem.getColor(), foundItem.getColor())) {
            score += SCORE_COLOR;
        }

        if (datesAreClose(lostItem.getOccurredAt(), foundItem.getOccurredAt())) {
            score += SCORE_DATE;
        }

        if (locationsAreClose(lostItem, foundItem)) {
            score += SCORE_LOCATION;
        }

        if (descriptionsAreSimilar(lostItem.getDescription(), foundItem.getDescription())) {
            score += SCORE_DESCRIPTION;
        }

        return score;
    }

    private boolean sameColor(String colorA, String colorB) {
        if (colorA == null || colorB == null) {
            return false;
        }
        return colorA.trim().equalsIgnoreCase(colorB.trim());
    }

    private boolean datesAreClose(LocalDate dateA, LocalDate dateB) {
        if (dateA == null || dateB == null) {
            return false;
        }
        long daysBetween = Math.abs(ChronoUnit.DAYS.between(dateA, dateB));
        return daysBetween <= DATE_PROXIMITY_DAYS;
    }

    private boolean locationsAreClose(Item a, Item b) {
        if (a.getLatitude() == null || a.getLongitude() == null
                || b.getLatitude() == null || b.getLongitude() == null) {
            return false;
        }
        double distance = GeocodingService.distanceInMeters(
                a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        return distance <= LOCATION_PROXIMITY_METERS;
    }

    private boolean descriptionsAreSimilar(String descriptionA, String descriptionB) {
        if (descriptionA == null || descriptionB == null) {
            return false;
        }

        Set<String> wordsA = tokenize(descriptionA);
        Set<String> wordsB = tokenize(descriptionB);

        if (wordsA.isEmpty() || wordsB.isEmpty()) {
            return false;
        }

        Set<String> intersection = new HashSet<>(wordsA);
        intersection.retainAll(wordsB);


        double overlapRatio = (double) intersection.size() / Math.min(wordsA.size(), wordsB.size());
        return overlapRatio >= 0.3;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(word -> word.length() > 3)
                .collect(Collectors.toSet());
    }

    private boolean alreadyExists(Match match) {
        return matchRepository.existsByLostItemIdAndFoundItemId(
                match.getLostItem().getId(), match.getFoundItem().getId());
    }
}