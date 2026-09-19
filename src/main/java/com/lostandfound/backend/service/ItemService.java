package com.lostandfound.backend.service;

import com.lostandfound.backend.dto.request.ItemRequest;
import com.lostandfound.backend.dto.response.ItemResponse;
import com.lostandfound.backend.exception.BadRequestException;
import com.lostandfound.backend.exception.ForbiddenException;
import com.lostandfound.backend.exception.ResourceNotFoundException;
import com.lostandfound.backend.model.Item;
import com.lostandfound.backend.model.ItemImage;
import com.lostandfound.backend.model.User;
import com.lostandfound.backend.model.enums.ItemCategory;
import com.lostandfound.backend.model.enums.ItemStatus;
import com.lostandfound.backend.model.enums.ItemType;
import com.lostandfound.backend.repository.ItemImageRepository;
import com.lostandfound.backend.repository.ItemRepository;
import com.lostandfound.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;
    private final ImageStorageService imageStorageService;
    private final MatchingService matchingService;

    @Transactional
    public ItemResponse create(ItemRequest request, String userEmail) {
        User owner = getUserByEmail(userEmail);

        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();

        if ((latitude == null || longitude == null) && request.getLocationAddress() != null
                && !request.getLocationAddress().isBlank()) {
            var coordinates = geocodingService.geocode(request.getLocationAddress());
            if (coordinates.isPresent()) {
                latitude = coordinates.get().latitude();
                longitude = coordinates.get().longitude();
            }
        }

        Item item = Item.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .category(request.getCategory())
                .color(request.getColor())
                .occurredAt(request.getOccurredAt())
                .latitude(latitude)
                .longitude(longitude)
                .locationDescription(request.getLocationDescription() != null
                        ? request.getLocationDescription() : request.getLocationAddress())
                .status(ItemStatus.ATIVO)
                .user(owner)
                .build();

        Item saved = itemRepository.save(item);

        int matchesFound = matchingService.findAndPersistMatches(saved).size();
        if (matchesFound > 0) {
            log.info("Found {} potential match(es) for item {}", matchesFound, saved.getId());
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> search(ItemType type, ItemCategory category, ItemStatus status, String color) {
        String colorFilter = (color == null || color.isBlank())
                ? null
                : color.trim().toLowerCase();

        return itemRepository.search(type, category, status, colorFilter).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse getById(Long id) {
        return toResponse(getItemOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getMyItems(String userEmail) {
        User user = getUserByEmail(userEmail);
        return itemRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ItemResponse update(Long id, ItemRequest request, String userEmail) {
        Item item = getItemOrThrow(id);
        assertOwnership(item, userEmail);

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setType(request.getType());
        item.setCategory(request.getCategory());
        item.setColor(request.getColor());
        item.setOccurredAt(request.getOccurredAt());

        if (request.getLatitude() != null && request.getLongitude() != null) {
            // coordenadas explícitas têm prioridade
            item.setLatitude(request.getLatitude());
            item.setLongitude(request.getLongitude());
        } else if (request.getLocationAddress() != null && !request.getLocationAddress().isBlank()) {
            // sem coordenadas, mas com endereço novo: tenta geocodificar
            geocodingService.geocode(request.getLocationAddress()).ifPresent(coordinates -> {
                item.setLatitude(coordinates.latitude());
                item.setLongitude(coordinates.longitude());
            });
        }

        if (request.getLocationDescription() != null) {
            item.setLocationDescription(request.getLocationDescription());
        }

        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public void delete(Long id, String userEmail) {
        Item item = getItemOrThrow(id);
        assertOwnership(item, userEmail);
        itemRepository.delete(item);
    }

    @Transactional
    public ItemResponse markAsResolved(Long id, String userEmail) {
        Item item = getItemOrThrow(id);
        assertOwnership(item, userEmail);
        item.setStatus(ItemStatus.RESOLVIDO);
        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public ItemResponse addImage(Long itemId, MultipartFile file, String userEmail) {
        Item item = getItemOrThrow(itemId);
        assertOwnership(item, userEmail);

        String url = imageStorageService.store(file);

        ItemImage image = ItemImage.builder()
                .url(url)
                .item(item)
                .build();

        itemImageRepository.save(image);
        item.getImages().add(image);

        return toResponse(item);
    }

    private Item getItemOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item com o id: " + id + " não encontrado"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private void assertOwnership(Item item, String userEmail) {
        if (!item.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ForbiddenException("Você não tem permissão de modificar este item");
        }
    }

    public ItemResponse toResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .type(item.getType())
                .category(item.getCategory())
                .color(item.getColor())
                .occurredAt(item.getOccurredAt())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .locationDescription(item.getLocationDescription())
                .status(item.getStatus())
                .ownerId(item.getUser().getId())
                .ownerName(item.getUser().getName())
                .imageUrls(item.getImages().stream().map(ItemImage::getUrl).toList())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
