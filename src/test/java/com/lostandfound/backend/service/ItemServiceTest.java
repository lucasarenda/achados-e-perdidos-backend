package com.lostandfound.backend.service;

import com.lostandfound.backend.dto.request.ItemRequest;
import com.lostandfound.backend.dto.response.ItemResponse;
import com.lostandfound.backend.exception.ForbiddenException;
import com.lostandfound.backend.exception.ResourceNotFoundException;
import com.lostandfound.backend.model.Item;
import com.lostandfound.backend.model.User;
import com.lostandfound.backend.model.enums.ItemCategory;
import com.lostandfound.backend.model.enums.ItemStatus;
import com.lostandfound.backend.model.enums.ItemType;
import com.lostandfound.backend.repository.ItemImageRepository;
import com.lostandfound.backend.repository.ItemRepository;
import com.lostandfound.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService")
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemImageRepository itemImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private MatchingService matchingService;

    @InjectMocks
    private ItemService itemService;

    private User owner;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Lucas").email("lucas@example.com").password("x").build();

        item = Item.builder()
                .id(10L)
                .title("Black backpack")
                .type(ItemType.PERDIDO)
                .category(ItemCategory.MOCHILA)
                .occurredAt(LocalDate.of(2026, 9, 18))
                .status(ItemStatus.ATIVO)
                .user(owner)
                .images(List.of())
                .build();
    }

    @Test
    @DisplayName("creates an item, geocodes the address when coordinates are missing, and runs the matching engine")
    void createsItemAndGeocodesAddress() {
        ItemRequest request = new ItemRequest();
        request.setTitle("Black backpack");
        request.setType(ItemType.PERDIDO);
        request.setCategory(ItemCategory.MOCHILA);
        request.setOccurredAt(LocalDate.of(2026, 9, 18));
        request.setLocationAddress("Rua da Consolação, 930, São Paulo");

        when(userRepository.findByEmail("lucas@example.com")).thenReturn(Optional.of(owner));
        when(geocodingService.geocode("Rua da Consolação, 930, São Paulo"))
                .thenReturn(Optional.of(new GeocodingService.Coordinates(-23.5489, -46.6388)));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item saved = invocation.getArgument(0);
            saved.setId(10L);
            saved.setImages(List.of());
            return saved;
        });
        when(matchingService.findAndPersistMatches(any(Item.class))).thenReturn(List.of());

        ItemResponse response = itemService.create(request, "lucas@example.com");

        assertThat(response.getLatitude()).isEqualTo(-23.5489);
        assertThat(response.getLongitude()).isEqualTo(-46.6388);
        assertThat(response.getOwnerId()).isEqualTo(1L);
        verify(matchingService).findAndPersistMatches(any(Item.class));
    }

    @Test
    @DisplayName("does not call the geocoding service when latitude/longitude are already provided")
    void skipsGeocodingWhenCoordinatesProvided() {
        ItemRequest request = new ItemRequest();
        request.setTitle("Black backpack");
        request.setType(ItemType.PERDIDO);
        request.setCategory(ItemCategory.MOCHILA);
        request.setOccurredAt(LocalDate.of(2026, 9, 18));
        request.setLatitude(-23.5489);
        request.setLongitude(-46.6388);

        when(userRepository.findByEmail("lucas@example.com")).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item saved = invocation.getArgument(0);
            saved.setId(10L);
            saved.setImages(List.of());
            return saved;
        });
        when(matchingService.findAndPersistMatches(any(Item.class))).thenReturn(List.of());

        itemService.create(request, "lucas@example.com");

        verify(geocodingService, never()).geocode(any());
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when the item does not exist")
    void throwsWhenItemNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("throws ForbiddenException when a user who is not the owner tries to update an item")
    void throwsWhenNonOwnerUpdates() {
        ItemRequest request = new ItemRequest();
        request.setTitle("New title");
        request.setType(ItemType.PERDIDO);
        request.setCategory(ItemCategory.MOCHILA);
        request.setOccurredAt(LocalDate.of(2026, 9, 18));

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.update(10L, request, "someone-else@example.com"))
                .isInstanceOf(ForbiddenException.class);

        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("allows the owner to update their own item")
    void allowsOwnerToUpdate() {
        ItemRequest request = new ItemRequest();
        request.setTitle("Updated title");
        request.setType(ItemType.PERDIDO);
        request.setCategory(ItemCategory.MOCHILA);
        request.setOccurredAt(LocalDate.of(2026, 9, 19));

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemResponse response = itemService.update(10L, request, "lucas@example.com");

        assertThat(response.getTitle()).isEqualTo("Updated title");
    }

    @Test
    @DisplayName("throws ForbiddenException when a user who is not the owner tries to delete an item")
    void throwsWhenNonOwnerDeletes() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.delete(10L, "someone-else@example.com"))
                .isInstanceOf(ForbiddenException.class);

        verify(itemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("marks an item as resolved")
    void marksItemAsResolved() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemResponse response = itemService.markAsResolved(10L, "lucas@example.com");

        assertThat(response.getStatus()).isEqualTo(ItemStatus.RESOLVIDO);
    }
}
