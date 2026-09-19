package com.lostandfound.backend.controller;

import com.lostandfound.backend.dto.request.ItemRequest;
import com.lostandfound.backend.dto.response.ItemResponse;
import com.lostandfound.backend.model.enums.ItemCategory;
import com.lostandfound.backend.model.enums.ItemStatus;
import com.lostandfound.backend.model.enums.ItemType;
import com.lostandfound.backend.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest request,
                                                Authentication authentication) {
        ItemResponse created = itemService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @GetMapping
    public ResponseEntity<List<ItemResponse>> search(@RequestParam(required = false) ItemType type,
                                                       @RequestParam(required = false) ItemCategory category,
                                                       @RequestParam(required = false) ItemStatus status,
                                                       @RequestParam(required = false) String color) {
        return ResponseEntity.ok(itemService.search(type, category, status, color));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ItemResponse>> getMyItems(Authentication authentication) {
        return ResponseEntity.ok(itemService.getMyItems(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody ItemRequest request,
                                                Authentication authentication) {
        return ResponseEntity.ok(itemService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        itemService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ItemResponse> resolve(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(itemService.markAsResolved(id, authentication.getName()));
    }

    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<ItemResponse> addImage(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file,
                                                  Authentication authentication) {
        return ResponseEntity.ok(itemService.addImage(id, file, authentication.getName()));
    }
}
